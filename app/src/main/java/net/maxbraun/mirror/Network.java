package net.maxbraun.mirror;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

/**
 * A helper class for making network requests.
 */
public abstract class Network {
  private static final String TAG = Network.class.getSimpleName();

  /**
   * The shared preferences key suffix for the access token.
   */
  private static final String KEY_ACCESS_TOKEN = "access_token";

  /**
   The shared preferences key suffix for the refresh token.
   */
  private static final String KEY_REFRESH_TOKEN = "refresh_token";

  /**
   * A provider for additional information about an OAuth API.
   */
  public interface OAuthDataProvider {
    /**
     * @return the client ID.
     */
    String getClientId();

    /**
     * @return the client secret.
     */
    String getClientSecret();

    /**
     * @return the initial refresh token.
     */
    String getRefreshToken();

    /**
     * @return a unique ID for this API instance.
     */
    String getServiceId();

    /**
     * @return the body of a response to an invalid access token.
     */
    String getInvalidTokenResponse();
  }

  /**
   * Makes a HTTP(S) GET request to the specified URL and returns the result as text or
   * {@code null} if there was an error.
   */
  public static String get(String urlString) {
    if (urlString == null) {
      return null;
    }
    Log.d(TAG, "Requesting URL: " + urlString);

    HttpURLConnection connection = null;
    InputStream inputStream = null;
    try {
      URL url = new URL(urlString);
      if (urlString.startsWith("https")) {
        connection = (HttpsURLConnection) url.openConnection();
      } else {
        connection = (HttpURLConnection) url.openConnection();
      }
      inputStream = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
      }
      return result.toString();
    } catch (IOException e) {
      Log.e(TAG, "Request failed.", e);
      return null;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          Log.w(TAG, "Failed to close input stream.");
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * Like {@link #get(String)}, but for OAuth authenticated requests.
   */
  public static String get(Activity activity, String urlString, BaseApi<OAuth20Service> api,
                           OAuthDataProvider data) {
    if (urlString == null) {
      return null;
    }
    Log.d(TAG, "Requesting OAuth URL: " + urlString);

    try {
      OAuth20Service service = new ServiceBuilder(data.getClientId())
          .apiSecret(data.getClientSecret())
          .build(api);

      // Use the saved access token, if there is one. Otherwise use the initial refresh token and
      // save it for next time.
      OAuth2AccessToken accessToken = loadAccessToken(activity, data);
      if (accessToken == null) {
        Log.w(TAG, "No saved access token. Using initial refresh token.");
        accessToken = service.refreshAccessToken(data.getRefreshToken());
        saveAccessToken(activity, data, accessToken);
      }

      // Make the authenticated request.
      Response response = makeOAuthRequest(urlString, service, accessToken);

      // Capture expired access tokens, then refresh and save them.
      if ((response.getCode() == 401)
          || data.getInvalidTokenResponse().equals(response.getBody())) {
        Log.w(TAG, "Authentication failed. Refreshing access token.");
        accessToken = service.refreshAccessToken(accessToken.getRefreshToken());
        saveAccessToken(activity, data, accessToken);
      }

      // Retry the request with the new access token.
      response = makeOAuthRequest(urlString, service, accessToken);

      return response.getBody();
    } catch (IOException | InterruptedException | ExecutionException e) {
      Log.e(TAG, "OAuth request failed.", e);
      return null;
    }
  }

  /**
   * Makes a network request at the specified URL, expecting a JSON response.
   */
  public static JSONObject getJson(String requestUrl) throws JSONException {
    String response = get(requestUrl);
    if (response != null) {
      return new JSONObject(response);
    } else {
      Log.w(TAG, "Empty response.");
      return null;
    }
  }

  /**
   * Like {@link #getJson(String)}, but for OAuth authenticated requests.
   */
  public static JSONObject getJson(Activity activity, String requestUrl,
                                   BaseApi<OAuth20Service> api, OAuthDataProvider data)
      throws JSONException {
    String response = get(activity, requestUrl, api, data);
    if (response != null) {
      return new JSONObject(response);
    } else {
      Log.w(TAG, "Empty OAuth response.");
      return null;
    }
  }

  /**
   * Loads an access token from shared preferences.
   */
  private static OAuth2AccessToken loadAccessToken(Activity activity, OAuthDataProvider data) {
    // Load the keys from shared preferences.
    SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    String accessToken = preferences.getString(getScopedKey(data, KEY_ACCESS_TOKEN), null);
    String refreshToken = preferences.getString(getScopedKey(data, KEY_REFRESH_TOKEN), null);

    // Only return the access token if both keys are present.
    if ((accessToken != null) && (refreshToken != null)) {
      return new OAuth2AccessToken(accessToken, null, 0, refreshToken, null, null);
    } else {
      return null;
    }
  }

  /**
   * Saves an access token to shared preferences.
   */
  private static void saveAccessToken(Activity activity, OAuthDataProvider data,
                                      OAuth2AccessToken accessToken) {
    // Save the keys to shared preferences.
    SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
    editor.putString(getScopedKey(data, KEY_ACCESS_TOKEN), accessToken.getAccessToken());
    editor.putString(getScopedKey(data, KEY_REFRESH_TOKEN), accessToken.getRefreshToken());
    editor.commit();
  }

  /**
   * Creates a unique shared preferences key scoped to one service.
   */
  private static String getScopedKey(OAuthDataProvider data, String key) {
    return data.getServiceId() + "_" + key;
  }

  /**
   * Executes an OAuth request and returns the response.
   */
  private static Response makeOAuthRequest(String urlString, OAuth20Service service,
                                           OAuth2AccessToken accessToken)
      throws InterruptedException, ExecutionException, IOException {
    OAuthRequest request = new OAuthRequest(Verb.GET, urlString);
    service.signRequest(accessToken, request);
    return service.execute(request);
  }
}
