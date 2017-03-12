package net.maxbraun.mirror;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * A helper class for making network requests.
 */
public abstract class Network {
  private static final String TAG = Network.class.getSimpleName();

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
   * Makes a network request at the specified URL, expecting a JSON response.
   */
  public static JSONObject getJson(String requestUrl) throws JSONException {
    String response = Network.get(requestUrl);
    if (response != null) {
      return new JSONObject(response);
    } else {
      Log.w(TAG, "Empty response.");
      return null;
    }
  }
}
