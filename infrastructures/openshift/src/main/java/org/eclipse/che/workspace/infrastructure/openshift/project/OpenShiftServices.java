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
package org.eclipse.che.workspace.infrastructure.openshift.project;

import static org.eclipse.che.workspace.infrastructure.openshift.Constants.CHE_WORKSPACE_ID_LABEL;
import static org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftObjectUtil.putLabel;
import static org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftObjectUtil.putSelector;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.List;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;

/**
 * Defines an internal API for managing {@link Service} instances in {@link
 * OpenShiftServices#namespace predefined namespace}.
 *
 * @author Sergii Leshchenko
 */
public class OpenShiftServices {
  private final String namespace;
  private final String workspaceId;
  private final OpenShiftClientFactory clientFactory;

  OpenShiftServices(String namespace, String workspaceId, OpenShiftClientFactory clientFactory) {
    this.namespace = namespace;
    this.workspaceId = workspaceId;
    this.clientFactory = clientFactory;
  }

  /**
   * Creates specified service.
   *
   * @param service service to create
   * @return created service
   * @throws InfrastructureException when any exception occurs
   */
  public Service create(Service service) throws InfrastructureException {
    putLabel(service, CHE_WORKSPACE_ID_LABEL, workspaceId);
    putSelector(service, CHE_WORKSPACE_ID_LABEL, workspaceId);
    try {
      return clientFactory.create().services().inNamespace(namespace).create(service);
    } catch (KubernetesClientException e) {
      throw new InfrastructureException(e.getMessage(), e);
    }
  }

  /**
   * Returns all existing services.
   *
   * @throws InfrastructureException when any exception occurs
   */
  public List<Service> get() throws InfrastructureException {
    try {
      return clientFactory
          .create()
          .services()
          .inNamespace(namespace)
          .withLabel(CHE_WORKSPACE_ID_LABEL, workspaceId)
          .list()
          .getItems();
    } catch (KubernetesClientException e) {
      throw new InfrastructureException(e.getMessage(), e);
    }
  }

  /**
   * Deletes all existing services.
   *
   * @throws InfrastructureException when any exception occurs
   */
  public void delete() throws InfrastructureException {
    try {
      clientFactory
          .create()
          .services()
          .inNamespace(namespace)
          .withLabel(CHE_WORKSPACE_ID_LABEL, workspaceId)
          .delete();
    } catch (KubernetesClientException e) {
      throw new InfrastructureException(e.getMessage(), e);
    }
  }
}
