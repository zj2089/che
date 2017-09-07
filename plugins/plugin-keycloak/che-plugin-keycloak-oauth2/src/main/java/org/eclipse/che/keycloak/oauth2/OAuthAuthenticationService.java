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
package org.eclipse.che.keycloak.oauth2;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.keycloak.shared.KeycloakConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RESTful wrapper for OAuthAuthenticator. */
@Path("oauth")
public class OAuthAuthenticationService {
  private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticationService.class);

  @Context protected UriInfo uriInfo;

  @Context protected SecurityContext security;

  @Inject
  @Named(KeycloakConstants.AUTH_SERVER_URL_SETTING)
  String serverURL;

  @Named(KeycloakConstants.REALM_SETTING)
  String realm;

  //  @Named(KeycloakConstants.CLIENT_ID_SETTING)
  //  String clientId;

  /**
   * Redirect request
   *
   * @return typically Response that redirect user for OAuth provider site
   */
  @GET
  @Path("authenticate")
  public Response authenticate(
      @Required @QueryParam("oauth_provider") String oauthProvider,
      @Required @QueryParam("redirect_after_login") String redirectAfterLogin,
      @Context HttpServletRequest request)
      throws ForbiddenException, BadRequestException {

    String nonce = UUID.randomUUID().toString();
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    Jws<Claims> jwt = (Jws<Claims>) request.getAttribute("token");
    String sessionState = jwt.getBody().get("sessionState", String.class);
    String clientId = jwt.getBody().getIssuer();

    String input = nonce + sessionState + clientId + oauthProvider;
    byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
    String hash = Base64Url.encode(check);

    String redirectUri = redirectAfterLogin;
    String accountLinkUrl =
        KeycloakUriBuilder.fromUri(serverURL)
            .path("/realms/{realm}/broker/{provider}/link")
            .queryParam("nonce", nonce)
            .queryParam("hash", hash)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .build(realm, oauthProvider)
            .toString();

    return Response.temporaryRedirect(URI.create(accountLinkUrl)).build();
  }

  //  @GET
  //  @Path("callback")
  //  public Response callback(@QueryParam("errorValues") List<String> errorValues)
  //      throws OAuthAuthenticationException, BadRequestException {
  //    URL requestUrl = getRequestUrl(uriInfo);
  //    Map<String, List<String>> params = getQueryParametersFromState(getState(requestUrl));
  //    if (errorValues != null && errorValues.contains("access_denied")) {
  //      return Response.temporaryRedirect(
  //              uriInfo.getRequestUriBuilder().replacePath(errorPage).replaceQuery(null).build())
  //          .build();
  //    }
  //    final String providerName = getParameter(params, "oauth_provider");
  //    OAuthAuthenticator oauth = getAuthenticator(providerName);
  //    final List<String> scopes = params.get("scope");
  //    oauth.callback(requestUrl, scopes == null ? Collections.<String>emptyList() : scopes);
  //    final String redirectAfterLogin = getParameter(params, "redirect_after_login");
  //    return Response.temporaryRedirect(URI.create(redirectAfterLogin)).build();
  //  }

  //  /**
  //   * Gets list of installed OAuth authenticators.
  //   *
  //   * @return list of installed OAuth authenticators
  //   */
  //  @GET
  //  @Produces(MediaType.APPLICATION_JSON)
  //  public Set<OAuthAuthenticatorDescriptor> getRegisteredAuthenticators() {
  //    Set<OAuthAuthenticatorDescriptor> result = new HashSet<>();
  //    final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().clone().path(getClass());
  //    for (String name : providers.getRegisteredProviderNames()) {
  //      final List<Link> links = new LinkedList<>();
  //      links.add(
  //          LinksHelper.createLink(
  //              HttpMethod.GET,
  //              uriBuilder.clone().path(getClass(), "authenticate").build().toString(),
  //              null,
  //              null,
  //              "Authenticate URL",
  //              newDto(LinkParameter.class)
  //                  .withName("oauth_provider")
  //                  .withRequired(true)
  //                  .withDefaultValue(name),
  //              newDto(LinkParameter.class)
  //                  .withName("mode")
  //                  .withRequired(true)
  //                  .withDefaultValue("federated_login")));
  //      result.add(newDto(OAuthAuthenticatorDescriptor.class).withName(name).withLinks(links));
  //    }
  //    return result;
  //  }

  /**
   * Gets OAuth token for user.
   *
   * @param oauthProvider OAuth provider name
   * @return OAuthToken
   * @throws ServerException
   */
  @GET
  @Path("token")
  @Produces(MediaType.APPLICATION_JSON)
  public OAuthToken token(@Required @QueryParam("oauth_provider") String oauthProvider)
      throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
    //OAuthAuthenticator provider = getAuthenticator(oauthProvider);
    final Subject subject = EnvironmentContext.getCurrent().getSubject();
    //    try {
    //      //      OAuthToken token = provider.getToken(subject.getUserId());
    //      //      if (token == null) {
    //      //        token = provider.getToken(subject.getUserName());
    //      //      }
    //      //      if (token != null) {
    //      //        return token;
    //      //      }
    //      throw new NotFoundException("OAuth token for user " + subject.getUserId() + " was not found");
    //    } catch (IOException e) {
    //      throw new ServerException(e.getLocalizedMessage(), e);
    //    }
    return null;
  }

  //  @DELETE
  //  @Path("token")
  //  public void invalidate(@Required @QueryParam("oauth_provider") String oauthProvider)
  //      throws BadRequestException, NotFoundException, ServerException, ForbiddenException {
  //
  //    OAuthAuthenticator oauth = getAuthenticator(oauthProvider);
  //    final Subject subject = EnvironmentContext.getCurrent().getSubject();
  //    try {
  //      if (!oauth.invalidateToken(subject.getUserId())) {
  //        throw new NotFoundException(
  //            "OAuth token for user " + subject.getUserId() + " was not found");
  //      }
  //    } catch (IOException e) {
  //      throw new ServerException(e.getMessage());
  //    }
  //  }

  //  protected OAuthAuthenticator getAuthenticator(String oauthProviderName)
  //      throws BadRequestException {
  //    OAuthAuthenticator oauth = providers.getAuthenticator(oauthProviderName);
  //    if (oauth == null) {
  //      LOG.warn("Unsupported OAuth provider {} ", oauthProviderName);
  //      throw new BadRequestException("Unsupported OAuth provider " + oauthProviderName);
  //    }
  //    return oauth;
  //  }
}
