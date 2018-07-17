package net.maxbraun.mirror;

import com.github.scribejava.core.builder.api.ClientAuthenticationType;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.builder.api.OAuth2SignatureType;

/**
 * An OAuth 2.0 API spec for the Nokia Health API.
 */
public class NokiaHealthApi extends DefaultApi20 {
  private NokiaHealthApi() {
  }

  private static class InstanceHolder {
    private static final NokiaHealthApi INSTANCE = new NokiaHealthApi();
  }

  public static NokiaHealthApi instance() {
    return InstanceHolder.INSTANCE;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return "https://account.health.nokia.com/oauth2/token?grant_type=authorization_code";
  }

  @Override
  public String getRefreshTokenEndpoint() {
    return "https://account.health.nokia.com/oauth2/token?grant_type=refresh_token";
  }

  @Override
  protected String getAuthorizationBaseUrl() {
    return "https://account.health.nokia.com/oauth2_user/authorize2";
  }

  @Override
  public OAuth2SignatureType getSignatureType() {
    return OAuth2SignatureType.BEARER_URI_QUERY_PARAMETER;
  }

  @Override
  public ClientAuthenticationType getClientAuthenticationType() {
    return ClientAuthenticationType.REQUEST_BODY;
  }
}
