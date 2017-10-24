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
package org.eclipse.che.plugin.docker.machine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.WorkspacesRoutingSuffixProvider;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;

/**
 */
public class NewCustomServerEvaluationStrategy extends CustomServerEvaluationStrategy {
  /** Default constructor */
  @Inject
  public NewCustomServerEvaluationStrategy(
      @Nullable @Named("che.docker.ip") String cheDockerIp,
      @Nullable @Named("che.docker.ip.external") String cheDockerIpExternal,
      @Nullable @Named("che.docker.server_evaluation_strategy.custom.template")
          String cheDockerCustomExternalTemplate,
      @Nullable @Named("che.docker.server_evaluation_strategy.custom.external.protocol")
          String cheDockerCustomExternalProtocol,
      @Named("che.port") String chePort,
      WorkspacesRoutingSuffixProvider cheWorkspacesRoutingSuffixProvider) {
    super(
        cheDockerIp,
        cheDockerIpExternal,
        cheDockerCustomExternalTemplate,
        cheDockerCustomExternalProtocol,
        chePort,
        cheWorkspacesRoutingSuffixProvider);
  }

  @Override
  protected Map<String, String> getInternalAddressesAndPorts(ContainerInfo containerInfo,
      String internalHost) {
    return super.getExternalAddressesAndPorts(containerInfo, internalHost);
  }
}
