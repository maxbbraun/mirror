package net.maxbraun.mirror;

import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class to look up location by IP.
 */
public abstract class GeoLocation {
  private static final String TAG = GeoLocation.class.getSimpleName();

  /**
   * The URL of the geo location API endpoint.
   */
  private static final String GEO_IP_URL = "http://ip-api.com/json";

  /**
   * The location cached at the last request. Assumed to be static.
   */
  private static Location cachedLocation;

  /**
   * Makes a request to the geo location API and returns the current location or {@code null} on
   * error. Uses an in memory cache after the first request.
   */
  public static Location getLocation() {
    // Always use the cache, if possible.
    if (cachedLocation != null) {
      return cachedLocation;
    }

    // We're using geo location by IP, because many headless Android devices don't return anything
    // useful through the usual location APIs.
    String response = Network.get(GEO_IP_URL);
    if (response == null) {
      Log.e(TAG, "Empty response.");
      return null;
    }

    // Parse the latitude and longitude from the response JSON.
    try {
      JSONObject responseJson = new JSONObject(response);
      double latitude = responseJson.getDouble("lat");
      double longitude = responseJson.getDouble("lon");
      Location location = new Location("");
      location.setLatitude(latitude);
      location.setLongitude(longitude);
      Log.d(TAG, "Using location: " + location);

      // Populate the cache.
      cachedLocation = location;

      return location;
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse geo location JSON.");
      return null;
    }
  }
}
