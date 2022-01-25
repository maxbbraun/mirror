package net.maxbraun.mirror;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignature;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignatureURIQueryParameter;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;
import com.github.scribejava.core.utils.Preconditions;

import java.io.IOException;

/**
 * An OAuth 2.0 API spec for the Withings API.
 */
public class WithingsApi extends DefaultApi20 {
  private WithingsApi() {
  }

  private static class InstanceHolder {
    private static final WithingsApi INSTANCE = new WithingsApi();
  }

  public static WithingsApi instance() {
    return InstanceHolder.INSTANCE;
  }

  /**
   * An access token extractor to handle the Withings API's nonstandard response format.
   */
  private static class AccessTokenExtractor extends OAuth2AccessTokenJsonExtractor {
    @Override
    public OAuth2AccessToken extract(Response response) throws IOException {
      final String responseBody = response.getBody();
      Preconditions.checkEmptyString(responseBody, "Empty response body");

      if (response.getCode() != 200) {
        generateError(responseBody);
      }

      final JsonNode responseJson = OBJECT_MAPPER.readTree(responseBody);

      final JsonNode status = responseJson.get("status");
      Preconditions.checkNotNull(status,
          "Missing status key in response JSON: " + responseJson);
      if (status.asInt() != 0) {
        throw new IllegalArgumentException("Invalid status in response: " + status);
      }

      final JsonNode body = responseJson.get("body");
      Preconditions.checkNotNull(body,
          "Missing body key in response JSON: " + responseJson);

      final JsonNode expiresInNode = body.get("expires_in");
      final JsonNode refreshToken = body.get(OAuthConstants.REFRESH_TOKEN);
      final JsonNode scope = body.get(OAuthConstants.SCOPE);
      final JsonNode tokenType = body.get("token_type");
      final JsonNode accessToken = extractRequiredParameter(
          body, OAuthConstants.ACCESS_TOKEN, responseBody);

      return createToken(
          accessToken.asText(),
          tokenType == null ? null : tokenType.asText(),
          expiresInNode == null ? null : expiresInNode.asInt(),
          refreshToken == null ? null : refreshToken.asText(),
          scope == null ? null : scope.asText(),
          body,
          responseBody);
    }

    private static final AccessTokenExtractor INSTANCE = new AccessTokenExtractor();
  }

  @Override
  public String getAccessTokenEndpoint() {
    return "https://wbsapi.withings.net/v2/oauth2" +
        "?action=requesttoken" +
        "&grant_type=authorization_code";
  }

  @Override
  public String getRefreshTokenEndpoint() {
    return "https://wbsapi.withings.net/v2/oauth2" +
        "?action=requesttoken" +
        "&grant_type=refresh_token";
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

  @Override
  public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
    return AccessTokenExtractor.INSTANCE;
  }
}
