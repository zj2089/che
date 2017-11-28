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

import static com.google.common.base.Strings.isNullOrEmpty;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.commons.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sergii Leshchenko */
@Singleton
public class OpenShiftClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(OpenShiftClientFactory.class);

  private final OpenShiftClient client;

  @Inject
  public OpenShiftClientFactory(
      @Nullable @Named("che.infra.openshift.master_url") String masterUrl,
      @Nullable @Named("che.infra.openshift.username") String username,
      @Nullable @Named("che.infra.openshift.password") String password,
      @Nullable @Named("che.infra.openshift.oauth_token") String oauthToken,
      @Nullable @Named("che.infra.openshift.trust_certs") Boolean doTrustCerts) {
    OpenShiftConfigBuilder configBuilder = new OpenShiftConfigBuilder();
    if (!isNullOrEmpty(masterUrl)) {
      configBuilder.withMasterUrl(masterUrl);
    }

    if (!isNullOrEmpty(username)) {
      configBuilder.withUsername(username);
    }

    if (!isNullOrEmpty(password)) {
      configBuilder.withPassword(password);
    }

    if (!isNullOrEmpty(oauthToken)) {
      configBuilder.withOauthToken(oauthToken);
    }

    if (doTrustCerts != null) {
      configBuilder.withTrustCerts(doTrustCerts);
    }
    this.client = new DefaultOpenShiftClient(configBuilder.build());
  }

  public OpenShiftClient create() {
    return client;
  }

  @PreDestroy
  public void cleanup() {
    try {
      client.close();
    } catch (RuntimeException ex) {
      LOG.error(ex.getMessage());
    }
  }
}
