package net.maxbraun.mirror;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignature;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignatureURIQueryParameter;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

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
    return "https://account.withings.com/oauth2/token?grant_type=authorization_code";
  }

  @Override
  public String getRefreshTokenEndpoint() {
    return "https://account.withings.com/oauth2/token?grant_type=refresh_token";
  }

  @Override
  protected String getAuthorizationBaseUrl() {
    return "https://account.withings.com/oauth2_user/authorize2";
  }

  @Override
  public BearerSignature getBearerSignature() {
    return BearerSignatureURIQueryParameter.instance();
  }

  @Override
  public ClientAuthentication getClientAuthentication() {
    return RequestBodyAuthenticationScheme.instance();
  }
}
