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

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.security.Principal;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.commons.auth.token.RequestTokenExtractor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.representations.IDToken;

/**
 * Sets subject attribute into session based on keycloak authentication data.
 *
 * @author Max Shaposhnik (mshaposhnik@redhat.com)
 */
@Singleton
public class KeycloakEnvironmentInitalizationFilter extends AbstractKeycloakFilter {

  private final UserManager userManager;
  private final AccountManager accountManager;
  private final RequestTokenExtractor tokenExtractor;

  @Inject
  public KeycloakEnvironmentInitalizationFilter(
      UserManager userManager,
      AccountManager accountManager,
      RequestTokenExtractor tokenExtractor) {
    this.userManager = userManager;
    this.accountManager = accountManager;
    this.tokenExtractor = tokenExtractor;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String token = tokenExtractor.getToken(httpRequest);
    if (shouldSkipAuthentication(httpRequest, token)) {
      filterChain.doFilter(request, response);
      return;
    }

    KeycloakSecurityContext context =
        (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
    // In case of bearer token login, there is another object in session
    if (context == null) {
      OidcKeycloakAccount keycloakAccount =
          (OidcKeycloakAccount) httpRequest.getAttribute(KeycloakAccount.class.getName());
      if (keycloakAccount != null) {
        context = keycloakAccount.getKeycloakSecurityContext();
      }
    }
    if (context == null) {
      throw new ServletException("Cannot detect or instantiate user");
    }
    final IDToken idToken =
        context.getIdToken() != null ? context.getIdToken() : context.getToken();
    String tokenString = context.getTokenString();

    final HttpSession session = httpRequest.getSession();
    Subject subject = (Subject) session.getAttribute("che_subject");
    if (subject == null || !subject.getToken().equals(tokenString)) {
      User user =
          getOrCreateUser(idToken.getSubject(), idToken.getEmail(), idToken.getPreferredUsername());
      getOrCreateAccount(idToken.getPreferredUsername(), idToken.getPreferredUsername());

      subject = new SubjectImpl(user.getName(), user.getId(), tokenString, false);
      session.setAttribute("che_subject", subject);
    }

    try {
      EnvironmentContext.getCurrent().setSubject(subject);
      filterChain.doFilter(addUserInRequest(httpRequest, subject), response);
    } finally {
      EnvironmentContext.reset();
    }
  }

  private synchronized User getOrCreateUser(String id, String email, String username)
      throws ServletException {
    try {
      return userManager.getById(id);
    } catch (NotFoundException e) {
      try {
        final UserImpl cheUser = new UserImpl(id, email, username, "secret", emptyList());
        return userManager.create(cheUser, false);
      } catch (ServerException | ConflictException ex) {
        throw new ServletException("Unable to create new user", ex);
      }
    } catch (ServerException e) {
      throw new ServletException("Unable to get user", e);
    }
  }

  private synchronized Account getOrCreateAccount(String id, String namespace)
      throws ServletException {
    try {
      return accountManager.getById(id);
    } catch (NotFoundException e) {
      try {
        Account account = new AccountImpl(id, namespace, "personal");
        accountManager.create(account);
        return account;
      } catch (ServerException | ConflictException ex) {
        throw new ServletException("Unable to create new account", ex);
      }
    } catch (ServerException e) {
      throw new ServletException("Unable to get account", e);
    }
  }

  private HttpServletRequest addUserInRequest(
      final HttpServletRequest httpRequest, final Subject subject) {
    return new HttpServletRequestWrapper(httpRequest) {
      @Override
      public String getRemoteUser() {
        return subject.getUserName();
      }

      @Override
      public Principal getUserPrincipal() {
        return subject::getUserName;
      }
    };
  }
}
