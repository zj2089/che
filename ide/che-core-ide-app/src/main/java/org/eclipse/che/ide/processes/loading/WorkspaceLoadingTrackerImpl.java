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
package org.eclipse.che.ide.processes.loading;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.workspace.event.MachineRunningEvent;
import org.eclipse.che.ide.api.workspace.event.MachineStartingEvent;
import org.eclipse.che.ide.api.workspace.event.WorkspaceRunningEvent;
import org.eclipse.che.ide.api.workspace.model.EnvironmentImpl;
import org.eclipse.che.ide.api.workspace.model.MachineConfigImpl;
import org.eclipse.che.ide.api.workspace.model.MachineImpl;
import org.eclipse.che.ide.console.CommandConsoleFactory;
import org.eclipse.che.ide.machine.MachineResources;
import org.eclipse.che.ide.processes.panel.EnvironmentOutputEvent;
import org.eclipse.che.ide.processes.panel.ProcessesPanelPresenter;
import org.eclipse.che.ide.processes.panel.ProcessesPanelView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vetal on 9/29/17.
 */
@Singleton
public class WorkspaceLoadingTrackerImpl implements WorkspaceLoadingTracker, EnvironmentOutputEvent.Handler {

  private class Chunk {
    long size = 0;
    long downloaded = 0;
    boolean waiting = true;
  }

  private class Image {
    private String machineName;
    private String sha256;
    private String dockerImage;
    private Map<String, Chunk> chunks;
    private boolean downloaded;

    public Image(String machineName) {
      this.machineName = machineName;
      chunks = new HashMap<>();
    }

    public String getMachineName() {
      return machineName;
    }

    public void setSha256(String sha256) {
      this.sha256 = sha256;
    }

    public String getSha256() {
      return sha256;
    }

    public void setDockerImage(String dockerImage) {
      this.dockerImage = dockerImage;
    }

    public String getDockerImage() {
      return dockerImage;
    }

    public Map<String, Chunk> getChunks() {
      return chunks;
    }

    public void setDownloaded(boolean downloaded) {
      this.downloaded = downloaded;
    }

    public boolean isDownloaded() {
      return downloaded;
    }
  }

  private final AppContext appContext;
  private final EventBus eventBus;
  private final ProcessesPanelPresenter processesPanelPresenter;
  private final MachineResources resources;

  private final WorkspaceLoadingTrackerView view;

  //private Map<String, String> downloadProgress = new HashMap<>();
  private Map<String, Image> images = new HashMap<>();

  @Inject
  public WorkspaceLoadingTrackerImpl(
      AppContext appContext,
      EventBus eventBus,
      ProcessesPanelPresenter processesPanelPresenter,
      MachineResources resources,
      WorkspaceLoadingTrackerView view
  ) {

    this.appContext = appContext;
    this.eventBus = eventBus;
    this.processesPanelPresenter = processesPanelPresenter;
    this.resources = resources;

    this.view = view;

    eventBus.addHandler(WorkspaceRunningEvent.TYPE, new WorkspaceRunningEvent.Handler() {
      @Override
      public void onWorkspaceRunning(WorkspaceRunningEvent event) {
        onWorkspaceRunnning();
      }
    });

    eventBus.addHandler(MachineStartingEvent.TYPE, new MachineStartingEvent.Handler() {
      @Override
      public void onMachineStarting(MachineStartingEvent event) {
      }
    });

    eventBus.addHandler(MachineRunningEvent.TYPE, new MachineRunningEvent.Handler() {
      @Override
      public void onMachineRunning(MachineRunningEvent event) {
        view.setStartWorkspaceRuntimeRunning(event.getMachine().getName());
      }
    });
  }

  private void onWorkspaceRunnning() {
    for (String machineName : images.keySet()) {
      view.setMachinePullingComplete(machineName);
    }

    view.showStartingWorkspaceRuntimes();
//    view.showInitializingWorkspaceAgents();
    view.showWorkspaceStarted();
  }

//  private void onWorkspaceAlreadyRunning() {
//    view.showWorkspaceIsAlreadyRunning();
//  }

  private static native void log(String msg) /*-{ console.log(msg); }-*/;

  @Override
  public void startTracking() {
    log(">> startTracking");

    if (WorkspaceStatus.RUNNING == appContext.getWorkspace().getStatus()) {
//      onWorkspaceAlreadyRunning();
      return;
    }

    view.showLoadingStarted();

    String defaultEnvironmentName = appContext.getWorkspace().getConfig().getDefaultEnv();
    EnvironmentImpl defaultEnvironment = appContext.getWorkspace().getConfig().getEnvironments().get(defaultEnvironmentName);

    Map<String, MachineConfigImpl> machines = defaultEnvironment.getMachines();
    for (final String machineName : machines.keySet()) {
      MachineConfigImpl machine = machines.get(machineName);
      log(">> machine " + machineName);

      view.pullMachine(machineName);

      //downloadProgress.put(machineName, "");
      images.put(machineName, new Image(machineName));
    }

    eventBus.addHandler(EnvironmentOutputEvent.TYPE, this);
    ((ProcessesPanelView)processesPanelPresenter.getView()).addWidget("*", "Workspace-start", resources.output(), view, true);
  }

  @Override
  public void onEnvironmentOutput(EnvironmentOutputEvent event) {
    log(">> onEnvironmentOutput " + event.getMachineName() + " : " + event.getContent());

    ((ProcessesPanelView)processesPanelPresenter.getView()).showProcessOutput("*");

    Image machine = images.get(event.getMachineName());
    if (machine == null) {
      // TEMPORARY don't handle the machine if it is not exist
      return;
    }

    String text = event.getContent();

    if (text.startsWith("[DOCKER] ")) {
      handleDockerOutput(machine, text);
      return;
    }
  }

  private void handleDockerOutput(Image machine, String text) {
    try {
      if (dockerPullingStarted(machine, text)) {
        // [DOCKER] sha256:40a6dd3c1f3af152d834e66fdf1dbca722dbc8ab4e98e157251c5179e8a6aa44: Pulling from docker.io/eclipse/ubuntu_jdk8
        // Containing image SHA and image URL
        return;

      } else if (dockerPullingFinished(machine, text)) {
        // [DOCKER] Digest: sha256:40a6dd3c1f3af152d834e66fdf1dbca722dbc8ab4e98e157251c5179e8a6aa44
        // indicates image has been fully downloaded
        return;

      } else if (dockerPreparePullingChunk(machine, text)) {
        // [DOCKER] 6a447dcfe27d: Pulling fs layer
        // [DOCKER] d010c8cf75d7: Waiting
        return;

//      } else if (dockerWaitingPullingChunk(machine, text)) {
//        // [DOCKER] d010c8cf75d7: Waiting
//        return;

      } else if (dockerChunkPullingProgress(machine, text)) {
        // [DOCKER] 9fb6c798fa41: Downloading 16.22 MB/47.54 MB
        // gives how much of chunk has been already downloaded
        return;

      } else if (dockerChunkPullingCompleted(machine, text)) {
        // [DOCKER] 6fabefc10853: Download complete
        // mark chunk as fully downloaded
        return;
      }

    } catch (Exception e) {
      log("ERROR. Unable to parse docker output. " + e.getMessage());
      return;
    }

    // [DOCKER] 6fabefc10853: Verifying Checksum
    // skip this
  }

  /**
   * [DOCKER] sha256:40a6dd3c1f3af152d834e66fdf1dbca722dbc8ab4e98e157251c5179e8a6aa44: Pulling from docker.io/eclipse/ubuntu_jdk8
   *
   * @param machine
   * @param text
   *
   * @return
   */
  private boolean dockerPullingStarted(Image machine, String text) {
    if (!text.startsWith("[DOCKER] sha256:")) {
      return false;
    }

    String []parts = text.split(":");

    String sha256 = parts[1];
    log("SHA [" + sha256 + "]");
    machine.setSha256(sha256);

    String dockerImage = parts[2];
    if (dockerImage.startsWith(" Pulling from ")) {
      dockerImage = dockerImage.substring(" Pulling from ".length()).trim();
      log("DOCKER IMAGE [" + dockerImage + "]");
      machine.setDockerImage(dockerImage);
      view.setMachineImage(machine.getMachineName(), dockerImage);
    }

    return true;
  }

  /**
   * [DOCKER] Digest: sha256:40a6dd3c1f3af152d834e66fdf1dbca722dbc8ab4e98e157251c5179e8a6aa44
   *
   * @param machine
   * @param text
   * @return
   */
  private boolean dockerPullingFinished(Image machine, String text) {
    if (!text.startsWith("[DOCKER] Digest: ")) {
      return false;
    }

    String []parts = text.split(":");

    String sha256 = parts[2].trim();
    log("SHA [" + sha256 + "]");

    if (sha256.equals(machine.getSha256())) {
      machine.setDownloaded(true);
      view.setMachinePullingComplete(machine.getMachineName());
    } else {
      log("ERROR! Image sha256:" + sha256 + " not found!");
    }

    view.showStartingWorkspaceRuntimes();

    view.addStartWorkspaceRuntime(machine.getMachineName(), machine.getDockerImage());

    return true;
  }

  /**
   * [DOCKER] 6a447dcfe27d: Pulling fs layer
   * [DOCKER] d010c8cf75d7: Waiting
   *
   * @param machine
   * @param text
   * @return
   */
  private boolean dockerPreparePullingChunk(Image machine, String text) {
    if (!(text.startsWith("[DOCKER] ") && (text.indexOf(": Pulling fs layer") > 0 || text.indexOf(": Waiting") > 0))) {
      return false;
    }

    text = text.substring("[DOCKER] ".length());

    String []parts = text.split(":");

    String hash = parts[0];
    log(">> docker pulling progress HASH [" + hash + "]");

    Chunk chunk = machine.getChunks().get(hash);
    if (chunk == null) {
      chunk = new Chunk();
      machine.getChunks().put(hash, chunk);
    }

    return true;
  }

  /**
   * [DOCKER] e7cfbd075aa8: Downloading 67.58 MB/244.3 MB
   *
   * @param machine
   * @param text
   * @return
   */
  private boolean dockerChunkPullingProgress(Image machine, String text) {
    if (!(text.startsWith("[DOCKER] ") && text.indexOf(": Downloading ") > 0)) {
      return false;
    }

    text = text.substring("[DOCKER] ".length());
    // now text must be like `e7cfbd075aa8: Downloading 67.58 MB/244.3 MB`

    String []parts = text.split(":");

    String hash = parts[0];
    String value = parts[1].substring(" Downloading ".length());
    // now value must be like `67.58 MB/244.3 MB`

    String []values = value.split("/");

    long downloaded = getSizeInBytes(values[0]);
    long size = getSizeInBytes(values[1]);

    Chunk chunk = machine.getChunks().get(hash);
    if (chunk == null) {
      chunk = new Chunk();
      machine.getChunks().put(hash, chunk);
    }

    chunk.downloaded = downloaded;
    chunk.size = size;
    chunk.waiting = false;

    refreshMachineDownloadingProgress(machine);

    return true;
  }

  private void refreshMachineDownloadingProgress(Image machine) {
    long totalDownloaded = 0;
    long totalSize = 0;

    int waitings = 0;

    for (Chunk chunk : machine.chunks.values()) {
      if (chunk.waiting) {
        waitings++;
      }

      totalSize += chunk.size;
      totalDownloaded += chunk.downloaded;
    }

    log(">> TOTAL DOWNLOADED " + totalDownloaded);
    log(">> TOTAL SIZE " + totalSize);

    log(">> WAITINGS " + waitings);
    if (waitings > 2) {
      return;
    }

    double percents = Math.round(totalDownloaded * 100 / totalSize);
    log(">> percents [" + percents + "]");

    view.setMachinePullingProgress(machine.getMachineName(), percents + "%");
  }

  /**
   * `621 B`      -> return 621
   * `490.8 kB`   -> return 490.8 * 1024
   * `1.474 MB`   -> return 1.474 * 1024 * 1024
   * `244.3 MB`   -> return 244.3 * 1024 * 1024
   *
   * @param value
   * @return
   */
  private long getSizeInBytes(String value) {
    value = value.toUpperCase();
    long size = 0;

    if (value.endsWith(" B")) {
      value = value.substring(0, value.length() - 2);
      size = Long.parseLong(value);
    } else if (value.endsWith(" KB")) {
      value = value.substring(0, value.length() - 3);
      size = (long)(Double.parseDouble(value) * 1024);
    } else if (value.endsWith(" MB")) {
      value = value.substring(0, value.length() - 3);
      size = (long)(Double.parseDouble(value) * 1024 * 1024);
    }

    return size;
  }

  /**
   * [DOCKER] 6fabefc10853: Download complete
   *
   * @param machine
   * @param text
   * @return
   */
  private boolean dockerChunkPullingCompleted(Image machine, String text) {
    return false;
  }

}
