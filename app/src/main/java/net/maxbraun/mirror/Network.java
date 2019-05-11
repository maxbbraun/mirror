package net.maxbraun.mirror;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
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
   * The shared preferences key suffix for the refresh token.
   */
  private static final String KEY_REFRESH_TOKEN = "refresh_token";

  /**
   * The shared preferences key suffix for the lifetime (in seconds) of the access token.
   */
  private static final String KEY_EXPIRES_IN = "expires_in";

  /**
   * The shared preferences key suffix for the Unix time (in seconds) when the access token as been
   * refreshed.
   */
  private static final String KEY_REFRESH_TIME = "refresh_time";

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
  }

  /**
   * A version of {@link OAuth2AccessToken} that knows when it should be refreshed.
   */
  private static class AccessToken extends OAuth2AccessToken {
    /**
     * The additional time in seconds to subtract from the expiration time.
     */
    private static final long REFRESH_TIME_BUFFER = 60;

    /**
     * The Unix time (in seconds) at which this access token was last refreshed.
     */
    private final long refreshTime;

    /**
     * Creates a new access token from the basic data.
     */
    public AccessToken(String accessToken, Integer expiresIn, String refreshToken,
                       long refreshTime) {
      super(accessToken, null, expiresIn, refreshToken, null, null);
      this.refreshTime = refreshTime;
    }

    /**
     * Creates a new access token by wrapping a {@link OAuth2AccessToken}.
     */
    public AccessToken(OAuth2AccessToken accessToken, long refreshTime) {
      this(accessToken.getAccessToken(), accessToken.getExpiresIn(), accessToken.getRefreshToken(),
          refreshTime);
    }

    /**
     * @return whether it's time to refresh this access token now.
     */
    public boolean shouldRefreshNow() {
      long currentTime = System.currentTimeMillis() / 1000;
      // Subtract some additional time to refresh earlier than the last second.
      return refreshTime + getExpiresIn() - REFRESH_TIME_BUFFER <= currentTime;
    }
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
  public static String get(Activity activity, String urlString, DefaultApi20 api,
                           OAuthDataProvider data) {
    if (urlString == null) {
      return null;
    }
    Log.d(TAG, "Requesting OAuth URL: " + urlString);

    try {
      OAuth20Service service = new ServiceBuilder(data.getClientId())
          .apiSecret(data.getClientSecret())
          .build(api);

      // Look for any saved access token. If there is none, refresh using the initial refresh token.
      // If there is one but it is expired, refresh using the saved refresh token.
      AccessToken accessToken = loadAccessToken(activity, data);
      if ((accessToken == null) || accessToken.shouldRefreshNow()) {
        Log.w(TAG, "Refreshing access token.");

        // Figure out which refresh token to use.
        String refreshToken;
        if (accessToken == null) {
          Log.d(TAG, "Using initial refresh token.");
          refreshToken = data.getRefreshToken();
        } else {
          Log.d(TAG, "Using saved refresh token.");
          refreshToken = accessToken.getRefreshToken();
        }

        // Get the new access token.
        long refreshTime = System.currentTimeMillis() / 1000;
        accessToken = new AccessToken(service.refreshAccessToken(refreshToken), refreshTime);

        // Save it for next time.
        saveAccessToken(activity, data, accessToken, refreshTime);
      }

      // Make the authenticated request.
      OAuthRequest request = new OAuthRequest(Verb.GET, urlString);
      service.signRequest(accessToken, request);
      Response response = service.execute(request);

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
  public static JSONObject getJson(Activity activity, String requestUrl, DefaultApi20 api,
                                   OAuthDataProvider data)
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
  private static AccessToken loadAccessToken(Activity activity, OAuthDataProvider data) {
    // Check if all keys are present.
    SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    String accessTokenKey = getScopedKey(data, KEY_ACCESS_TOKEN);
    String refreshTokenKey = getScopedKey(data, KEY_REFRESH_TOKEN);
    String expiresInKey = getScopedKey(data, KEY_EXPIRES_IN);
    String refreshTimeKey = getScopedKey(data, KEY_REFRESH_TIME);
    if (!preferences.contains(accessTokenKey) || !preferences.contains(refreshTokenKey)
        || !preferences.contains(expiresInKey) || !preferences.contains(refreshTimeKey)) {
      return null;
    }

    // Load the access token data from shared preferences.
    String accessToken = preferences.getString(accessTokenKey, null);
    String refreshToken = preferences.getString(refreshTokenKey, null);
    int expiresIn = preferences.getInt(expiresInKey, 0);
    long refreshTime = preferences.getLong(refreshTimeKey, 0);

    // Create the access token from the data.
    return new AccessToken(accessToken, expiresIn, refreshToken, refreshTime);
  }

  /**
   * Saves an access token to shared preferences.
   */
  private static void saveAccessToken(Activity activity, OAuthDataProvider data,
                                      AccessToken accessToken, long refreshTime) {
    // Save the access token data to shared preferences.
    SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
    editor.putString(getScopedKey(data, KEY_ACCESS_TOKEN), accessToken.getAccessToken());
    editor.putString(getScopedKey(data, KEY_REFRESH_TOKEN), accessToken.getRefreshToken());
    editor.putInt(getScopedKey(data, KEY_EXPIRES_IN), accessToken.getExpiresIn());
    editor.putLong(getScopedKey(data, KEY_REFRESH_TIME), refreshTime);
    editor.commit();
  }

  /**
   * Creates a unique shared preferences key scoped to one service.
   */
  private static String getScopedKey(OAuthDataProvider data, String key) {
    return data.getServiceId() + "_" + key;
  }
}
