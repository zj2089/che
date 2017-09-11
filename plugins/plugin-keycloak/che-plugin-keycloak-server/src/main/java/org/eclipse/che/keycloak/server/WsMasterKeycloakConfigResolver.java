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
package org.eclipse.che.keycloak.server;

import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;

@Singleton
public class WsMasterKeycloakConfigResolver implements KeycloakConfigResolver {

  private final KeycloakDeployment deployment;

  @Inject
  public WsMasterKeycloakConfigResolver(KeycloakConfiguration keycloakConfiguration) {
    AdapterConfig config = new AdapterConfig();
    config.setRealm(keycloakConfiguration.getRealm());
    config.setAuthServerUrl(keycloakConfiguration.getServerURL());
    config.setSslRequired(SslRequired.EXTERNAL.toString().toLowerCase());
    config.setCredentials(ImmutableMap.of("secret", "2c1b2621-d251-4701-82c4-a7dd447faa97"));
    config.setResource("che");
    //    config.setSslRequired(SslRequired.EXTERNAL.toString().toLowerCase());
    //    config.setCors(true);
    //    config.setBearerOnly(false);
    //    config.setPublicClient(true);
    //    config.setConnectionPoolSize(20);
    //    config.setDisableTrustManager(true);
    //    config.setAuthServerUrl(keycloakConfiguration.getServerURL());
    //    config.setRealm(keycloakConfiguration.getRealm());
    //    config.setResource("che");
    //config.setRedirectRewriteRules(ImmutableMap.of("^/wsmaster/api/(.*)$", "/api/$1"));

    deployment = KeycloakDeploymentBuilder.build(config);
  }

  @Override
  public KeycloakDeployment resolve(Request facade) {
    return deployment;
  }
}
