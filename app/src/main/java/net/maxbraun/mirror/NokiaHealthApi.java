package net.maxbraun.mirror;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.builder.api.OAuth1SignatureType;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.Verb;

/**
 * An OAuth 1.0 API spec for the Nokia Health API.
 */
public class NokiaHealthApi extends DefaultApi10a {
  private static final String BASE_URL = "https://developer.health.nokia.com/account";

  private NokiaHealthApi() {
  }

  private static class InstanceHolder {
    private static final NokiaHealthApi INSTANCE = new NokiaHealthApi();
  }

  public static NokiaHealthApi instance() {
    return InstanceHolder.INSTANCE;
  }

  @Override
  public String getRequestTokenEndpoint() {
    return BASE_URL + "/request_token";
  }

  @Override
  public String getAccessTokenEndpoint() {
    return BASE_URL + "/access_token";
  }

  @Override
  public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
    return String.format("%s/authorize?oauth_token=%s", BASE_URL, requestToken.getToken());
  }

  @Override
  public OAuth1SignatureType getSignatureType() {
    return OAuth1SignatureType.QueryString;
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.GET;
  }

  @Override
  public Verb getRequestTokenVerb() {
    return Verb.GET;
  }
}
