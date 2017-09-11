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

import static org.eclipse.che.keycloak.shared.KeycloakConstants.AUTH_SERVER_URL_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.CLIENT_ID_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.GITHUB_ENDPOINT_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.OSO_ENDPOINT_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.PASSWORD_ENDPOINT_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.PROFILE_ENDPOINT_SETTING;
import static org.eclipse.che.keycloak.shared.KeycloakConstants.REALM_SETTING;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.commons.annotation.Nullable;

@Singleton
public class KeycloakConfiguration {

  @Inject
  @Named(AUTH_SERVER_URL_SETTING)
  String serverURL;

  @Inject
  @Named(REALM_SETTING)
  String realm;

  @Inject
  @Named(CLIENT_ID_SETTING)
  String clientId;

  @Deprecated
  @Inject
  @Nullable
  @Named(OSO_ENDPOINT_SETTING)
  String osoEndpoint;

  @Deprecated
  @Inject
  @Nullable
  @Named(GITHUB_ENDPOINT_SETTING)
  String gitHubEndpoint;

  @Deprecated
  public String getOsoEndpoint() {
    return osoEndpoint;
  }

  @Deprecated
  public String getGitHubEndpoint() {
    return gitHubEndpoint;
  }

  public String getServerURL() {
    return serverURL;
  }

  public String getRealm() {
    return realm;
  }

  public String getClientId() {
    return clientId;
  }

  public String getProfileEndpoint() {
    return serverURL + "/realms/" + realm + "/account";
  }

  public String getPassoworfEndpoint() {
    return serverURL + "/realms/" + realm + "/account/password";
  }

  public Map<String, String> getPublicSettings() {
    return ImmutableMap.of(
        AUTH_SERVER_URL_SETTING,
        serverURL,
        CLIENT_ID_SETTING,
        clientId,
        REALM_SETTING,
        realm,
        PROFILE_ENDPOINT_SETTING,
        getProfileEndpoint(),
        PASSWORD_ENDPOINT_SETTING,
        getPassoworfEndpoint());
  }
}
