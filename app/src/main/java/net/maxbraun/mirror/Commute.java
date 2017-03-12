package net.maxbraun.mirror;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A helper class to regularly retrieve commute time estimates.
 */
public class Commute extends DataUpdater<String> {
  private static final String TAG = Commute.class.getSimpleName();

  /**
   * The time in milliseconds between API calls to update the commute time.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

  /**
   * The encoding used for request URLs.
   */
  private static final String URL_ENCODE_FORMAT = "UTF-8";

  /**
   * The context used to load string resources.
   */
  private final Context context;

  public Commute(Context context, UpdateListener<String> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
    this.context = context;
  }

  @Override
  protected String getData() {
    // Get the latest data from the Google Maps Directions API.
    String requestUrl = getRequestUrl();

    // Parse the data we are interested in from the response JSON.
    try {
      JSONObject response = Network.getJson(requestUrl);
      if (response != null) {
        return parseCommuteSummary(response);
      } else {
        return null;
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse weather JSON.", e);
      return null;
    }
  }

  /**
   * Creates the URL for a Google Maps Directions API request based on origin and destination
   * addresses from resources.
   */
  private String getRequestUrl() {
    try {
      return String.format(Locale.US, "https://maps.googleapis.com/maps/api/directions/json" +
              "?origin=%s" +
              "&destination=%s" +
              "&mode=%s" +
              "&departure_time=now" +
              "&key=%s",
          URLEncoder.encode(context.getString(R.string.home), URL_ENCODE_FORMAT),
          URLEncoder.encode(context.getString(R.string.work), URL_ENCODE_FORMAT),
          context.getString(R.string.travel_mode),
          context.getString(R.string.google_maps_directions_api_key));
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Failed to create request URL.", e);
      return null;
    }
  }

  /**
   * Reads the duration in traffic and route summary from the response. API documentation:
   * https://developers.google.com/maps/documentation/directions/intro
   */
  private String parseCommuteSummary(JSONObject response) throws JSONException {
    String status = response.getString("status");
    if (!"OK".equals(status)) {
      Log.e(TAG, "Error status in response: " + status);
      return null;
    }

    // Expect exactly one route.
    JSONArray routes = response.getJSONArray("routes");
    JSONObject route = routes.getJSONObject(0);

    // Expect exactly one leg.
    JSONArray legs = route.getJSONArray("legs");
    JSONObject leg = legs.getJSONObject(0);

    // Get the duration in traffic.
    JSONObject duration = leg.getJSONObject("duration_in_traffic");
    String durationText = duration.getString("text");

    // Get the route summary.
    String summaryText = route.getString("summary");
    String summary = String.format("%s via %s", durationText, summaryText);

    Log.d(TAG, "Commute summary: " + summary);
    return summary;
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
