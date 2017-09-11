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

import java.io.IOException;
import java.security.PublicKey;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.che.commons.auth.token.RequestTokenExtractor;
import org.eclipse.che.keycloak.shared.KeycloakConstants;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakAuthenticationFilter extends org.keycloak.adapters.servlet.KeycloakOIDCFilter {

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakAuthenticationFilter.class);

  private String authServerUrl;
  private String realm;
  private PublicKey publicKey = null;
  private RequestTokenExtractor tokenExtractor;

  @Inject
  public KeycloakAuthenticationFilter(
      @Named(KeycloakConstants.AUTH_SERVER_URL_SETTING) String authServerUrl,
      @Named(KeycloakConstants.REALM_SETTING) String realm,
      KeycloakConfigResolver configResolver,
      RequestTokenExtractor tokenExtractor) {
    super(configResolver);
    this.authServerUrl = authServerUrl;
    this.realm = realm;
    this.tokenExtractor = tokenExtractor;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    final String token = tokenExtractor.getToken(request);
    if (request.getScheme().startsWith("ws") || (token != null && token.startsWith("machine"))) {
      chain.doFilter(req, res);
      return;
    } else {
      final String requestURI = request.getRequestURI();
      LOG.debug("No 'Authorization' header for {}", requestURI);
      super.doFilter(req, res, chain);
      return;
    }
  }

  @Override
  public void destroy() {}
}
