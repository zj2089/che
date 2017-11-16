/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.openshift;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.eclipse.che.workspace.infrastructure.openshift.Constants.CHE_ORIGINAL_NAME_LABEL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.Machine;
import org.eclipse.che.api.core.model.workspace.runtime.MachineStatus;
import org.eclipse.che.api.core.model.workspace.runtime.ServerStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.server.URLRewriter.NoOpURLRewriter;
import org.eclipse.che.api.workspace.server.WorkspaceSharedPool;
import org.eclipse.che.api.workspace.server.hc.ServersChecker;
import org.eclipse.che.api.workspace.server.hc.ServersCheckerFactory;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalRuntime;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;
import org.eclipse.che.api.workspace.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.event.RuntimeStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.event.ServerStatusEvent;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.workspace.infrastructure.openshift.bootstrapper.OpenShiftBootstrapperFactory;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergii Leshchenko
 * @author Anton Korneta
 */
public class OpenShiftInternalRuntime extends InternalRuntime<OpenShiftRuntimeContext> {

  private static final Logger LOG = LoggerFactory.getLogger(OpenShiftInternalRuntime.class);

  private static final String RUNTIME_STOPPED_STATE = "STOPPED";
  private static final String RUNTIME_RUNNING_STATE = "RUNNING";
  private static final String POD_FAILED_STATUS = "Failed";

  private final EventService eventService;
  private final ServersCheckerFactory serverCheckerFactory;
  private final OpenShiftBootstrapperFactory bootstrapperFactory;
  private final Map<String, OpenShiftMachine> machines;
  private final int machineStartTimeoutMin;
  private final OpenShiftProject project;
  private final WorkspaceSharedPool pool;

  @Inject
  public OpenShiftInternalRuntime(
      @Named("che.infra.openshift.machine_start_timeout_min") int machineStartTimeoutMin,
      NoOpURLRewriter urlRewriter,
      EventService eventService,
      OpenShiftBootstrapperFactory bootstrapperFactory,
      ServersCheckerFactory serverCheckerFactory,
      @Assisted OpenShiftRuntimeContext context,
      @Assisted OpenShiftProject project,
      WorkspaceSharedPool pool) {
    super(context, urlRewriter, false);
    this.eventService = eventService;
    this.bootstrapperFactory = bootstrapperFactory;
    this.serverCheckerFactory = serverCheckerFactory;
    this.machineStartTimeoutMin = machineStartTimeoutMin;
    this.project = project;
    this.pool = pool;
    this.machines = new ConcurrentHashMap<>();
  }

  @Override
  protected void internalStart(Map<String, String> startOptions) throws InfrastructureException {
    try {
      final OpenShiftEnvironment osEnv = getContext().getOpenShiftEnvironment();

      List<Service> createdServices = new ArrayList<>();
      for (Service service : osEnv.getServices().values()) {
        createdServices.add(project.services().create(service));
      }

      List<Route> createdRoutes = new ArrayList<>();
      for (Route route : osEnv.getRoutes().values()) {
        createdRoutes.add(project.routes().create(route));
      }
      registerAbnormalStopHandler();
      registerContainersEventsPublisher();
      createPods(createdServices, createdRoutes);
      bootstrapMachines();

    } catch (InfrastructureException | RuntimeException | InterruptedException e) {
      LOG.error("Failed to start of OpenShift runtime. " + e.getMessage());
      boolean interrupted = Thread.interrupted() || e instanceof InterruptedException;
      try {
        project.cleanUp();
      } catch (InfrastructureException ignored) {
      }
      if (interrupted) {
        throw new InfrastructureException("OpenShift environment start was interrupted");
      }
      try {
        throw e;
      } catch (InfrastructureException rethrow) {
        throw rethrow;
      } catch (Exception wrap) {
        throw new InternalInfrastructureException(e.getMessage(), wrap);
      }
    }
  }

  /**
   * Bootstraps machines in parallel.
   *
   * @throws InfrastructureException when any error occurs while bootstrapping machines
   * @throws InterruptedException when machines bootstrapping was interrupted
   */
  private void bootstrapMachines() throws InfrastructureException, InterruptedException {
    List<CompletableFuture> checkMachines = new ArrayList<>(machines.size());
    List<BootstrapMachineTask> tasks = new ArrayList<>(machines.size());

    for (OpenShiftMachine machine : machines.values()) {
      BootstrapMachineTask task = new BootstrapMachineTask(machine);
      tasks.add(task);
      checkMachines.add(CompletableFuture.runAsync(task, pool.getExecutor()));
    }
    try {
      CompletableFuture.allOf(checkMachines.toArray(new CompletableFuture[checkMachines.size()]))
          .join();
    } catch (CompletionException e) {
      tasks.forEach(BootstrapMachineTask::interrupt);
      if (e.getCause() instanceof InterruptedException) {
        throw (InterruptedException) e.getCause();
      } else if (e.getCause() instanceof InfrastructureException) {
        throw (InfrastructureException) e.getCause();
      } else {
        throw new InfrastructureException(e.getCause().getMessage(), e.getCause());
      }
    }
  }

  private class BootstrapMachineTask implements Runnable {

    private OpenShiftMachine machine;

    BootstrapMachineTask(OpenShiftMachine machine) {
      this.machine = machine;
    }

    @Override
    public void run() {
      if (Thread.interrupted()) {
        return;
      }
      try {
        machine.waitRunning(machineStartTimeoutMin);
        bootstrapMachine(machine);
        checkMachineServers(machine);
        sendRunningEvent(machine.getName());
      } catch (InfrastructureException ex) {
        sendFailedEvent(machine.getName(), ex.getMessage());
        throw new CompletionException(ex);
      } catch (InterruptedException ei) {
        throw new CompletionException(ei);
      }
    }

    public void interrupt() {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public Map<String, ? extends Machine> getInternalMachines() {
    return ImmutableMap.copyOf(machines);
  }

  @Override
  protected void internalStop(Map<String, String> stopOptions) throws InfrastructureException {
    project.cleanUp();
  }

  @Override
  public Map<String, String> getProperties() {
    return emptyMap();
  }

  private void registerAbnormalStopHandler() throws InfrastructureException {
    project
        .pods()
        .watch(
            (action, pod) -> {
              if (pod.getStatus() != null && POD_FAILED_STATUS.equals(pod.getStatus().getPhase())) {
                try {
                  internalStop(emptyMap());
                } catch (InfrastructureException ex) {
                  LOG.error("OpenShift environment stop failed cause '{}'", ex.getMessage());
                } finally {
                  sendRuntimeStoppedEvent(
                      format("Pod '%s' was abnormally stopped", pod.getMetadata().getName()));
                }
              }
            });
  }

  private void registerContainersEventsPublisher() throws InfrastructureException {
    project
        .pods()
        .watchContainers(
            event ->
                eventService.publish(
                    DtoFactory.newDto(MachineLogEvent.class)
                        .withMachineName(event.getMachineName())
                        .withRuntimeId(DtoConverter.asDto(getContext().getIdentity()))
                        .withText(event.getMessage())
                        .withTime(event.getTime())));
  }

  /**
   * Bootstraps machine.
   *
   * @param machine the OpenShift machine instance to bootstrap
   * @throws InfrastructureException when any error occurs while bootstrapping machine
   * @throws InterruptedException when machine bootstrapping was interrupted
   */
  private void bootstrapMachine(OpenShiftMachine machine)
      throws InfrastructureException, InterruptedException {
    bootstrapperFactory
        .create(
            getContext().getIdentity(),
            getContext().getEnvironment().getMachines().get(machine.getName()).getInstallers(),
            machine)
        .bootstrap();
  }

  /**
   * Checks whether machine servers are ready.
   *
   * @param machine the OpenShift machine instance
   * @throws InfrastructureException when any error while server checks occur
   * @throws InterruptedException when process of server check was interrupted
   */
  private void checkMachineServers(OpenShiftMachine machine)
      throws InfrastructureException, InterruptedException {
    final ServersChecker check =
        serverCheckerFactory.create(
            getContext().getIdentity(), machine.getName(), machine.getServers());
    check.startAsync(new ServerReadinessHandler(machine.getName()));
    check.await();
  }

  /**
   * Creates OpenShift pods and resolves machine servers based on routes and services.
   *
   * @param services created OpenShift services
   * @param routes created OpenShift routes
   * @throws InfrastructureException when any error occurs while creating OpenShift pods
   */
  private void createPods(List<Service> services, List<Route> routes)
      throws InfrastructureException {
    final ServerResolver serverResolver = ServerResolver.of(services, routes);
    for (Pod toCreate : getContext().getOpenShiftEnvironment().getPods().values()) {
      final Pod createdPod = project.pods().create(toCreate);
      final ObjectMeta podMetadata = createdPod.getMetadata();
      for (Container container : createdPod.getSpec().getContainers()) {
        String podOriginalName = podMetadata.getLabels().get(CHE_ORIGINAL_NAME_LABEL);
        OpenShiftMachine machine =
            new OpenShiftMachine(
                Names.machineName(podOriginalName, container.getName()),
                podMetadata.getName(),
                container.getName(),
                serverResolver.resolve(createdPod, container),
                project);
        machines.put(machine.getName(), machine);
        sendStartingEvent(machine.getName());
      }
    }
  }

  private class ServerReadinessHandler implements Consumer<String> {
    private String machineName;

    ServerReadinessHandler(String machineName) {
      this.machineName = machineName;
    }

    @Override
    public void accept(String serverRef) {
      final OpenShiftMachine machine = machines.get(machineName);
      if (machine == null) {
        // Probably machine was removed from the list during server check start due to some reason
        return;
      }

      machine.setStatus(serverRef, ServerStatus.RUNNING);

      eventService.publish(
          DtoFactory.newDto(ServerStatusEvent.class)
              .withIdentity(DtoConverter.asDto(getContext().getIdentity()))
              .withMachineName(machineName)
              .withServerName(serverRef)
              .withStatus(ServerStatus.RUNNING)
              .withServerUrl(machine.getServers().get(serverRef).getUrl()));
    }
  }

  private void sendStartingEvent(String machineName) {
    eventService.publish(
        DtoFactory.newDto(MachineStatusEvent.class)
            .withIdentity(DtoConverter.asDto(getContext().getIdentity()))
            .withEventType(MachineStatus.STARTING)
            .withMachineName(machineName));
  }

  private void sendRunningEvent(String machineName) {
    eventService.publish(
        DtoFactory.newDto(MachineStatusEvent.class)
            .withIdentity(DtoConverter.asDto(getContext().getIdentity()))
            .withEventType(MachineStatus.RUNNING)
            .withMachineName(machineName));
  }

  private void sendFailedEvent(String machineName, String message) {
    eventService.publish(
        DtoFactory.newDto(MachineStatusEvent.class)
            .withIdentity(DtoConverter.asDto(getContext().getIdentity()))
            .withEventType(MachineStatus.FAILED)
            .withMachineName(machineName)
            .withError(message));
  }

  private void sendRuntimeStoppedEvent(String errorMsg) {
    eventService.publish(
        DtoFactory.newDto(RuntimeStatusEvent.class)
            .withIdentity(DtoConverter.asDto(getContext().getIdentity()))
            .withStatus(RUNTIME_STOPPED_STATE)
            .withPrevStatus(RUNTIME_RUNNING_STATE)
            .withFailed(true)
            .withError(errorMsg));
  }
}
