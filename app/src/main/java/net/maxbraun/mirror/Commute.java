package net.maxbraun.mirror;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.maxbraun.mirror.Commute.CommuteSummary;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A helper class to regularly retrieve commute time estimates.
 */
public class Commute extends DataUpdater<CommuteSummary> {
  private static final String TAG = Commute.class.getSimpleName();

  /**
   * The time in milliseconds between API calls to update the commute time.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

  /**
   * The time in milliseconds from now which is used as the future traffic reference point.
   */
  private static final long FUTURE_DELTA_MILLIS = TimeUnit.MINUTES.toMillis(15);

  /**
   * The time delta in seconds below which the traffic trend is considered flat.
   */
  private static final long TREND_THRESHOLD_SECONDS = TimeUnit.MINUTES.toSeconds(2);

  /**
   * The travel mode using standard driving directions using the road network.
   */
  private static final String MODE_DRIVING = "driving";

  /**
   * The travel mode using walking directions via pedestrian paths and sidewalks.
   */
  private static final String MODE_WALKING = "walking";

  /**
   * The travel mode using bicycling directions via bicycle paths and preferred streets.
   */
  private static final String MODE_BICYCLING = "bicycling";

  /**
   * The travel mode using directions via public transit routes.
   */
  private static final String MODE_TRANSIT = "transit";

  /**
   * The encoding used for request URLs.
   */
  private static final String URL_ENCODE_FORMAT = "UTF-8";

  /**
   * The context used to load string resources.
   */
  private final Context context;

  /**
   * A summary of current commute data.
   */
  public static class CommuteSummary {

    /**
     * A human-readable text summary.
     */
    public final String text;

    /**
     * The icon representing the travel mode.
     */
    public final Drawable travelModeIcon;

    /**
     * The icon representing the traffic trend or {@code null} if it is flat.
     */
    public final Drawable trafficTrendIcon;

    public CommuteSummary(String text, Drawable travelModeIcon, Drawable trafficTrendIcon) {
      this.text = text;
      this.travelModeIcon = travelModeIcon;
      this.trafficTrendIcon = trafficTrendIcon;
    }
  }

  public Commute(Context context, UpdateListener<CommuteSummary> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
    this.context = context;
  }

  @Override
  protected CommuteSummary getData() {
    // Get the latest data from the Google Maps Directions API for one departure now and one in the
    // future, to compare traffic.
    long nowMillis = System.currentTimeMillis();
    long futureMillis = nowMillis + FUTURE_DELTA_MILLIS;
    String nowRequestUrl = getRequestUrl(nowMillis);
    String futureRequestUrl = getRequestUrl(futureMillis);

    // Parse the data we are interested in from the response JSON.
    try {
      JSONObject nowResponse = Network.getJson(nowRequestUrl);
      JSONObject futureResponse = Network.getJson(futureRequestUrl);
      if ((nowResponse != null) && (futureResponse != null)) {
        return parseCommuteSummary(nowResponse, futureResponse);
      } else {
        return null;
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse directions JSON.", e);
      return null;
    }
  }

  /**
   * Creates the URL for a Google Maps Directions API request based on origin and destination
   * addresses from resources.
   */
  private String getRequestUrl(long departureTimeMillis) {
    try {
      return String.format(Locale.US, "https://maps.googleapis.com/maps/api/directions/json" +
              "?origin=%s" +
              "&destination=%s" +
              "&mode=%s" +
              "&departure_time=%d" +
              "&key=%s",
          URLEncoder.encode(context.getString(R.string.home), URL_ENCODE_FORMAT),
          URLEncoder.encode(context.getString(R.string.work), URL_ENCODE_FORMAT),
          context.getString(R.string.travel_mode),
          departureTimeMillis / 1000,
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
  private CommuteSummary parseCommuteSummary(JSONObject nowResponse, JSONObject futureResponse)
      throws JSONException {
    String nowStatus = nowResponse.getString("status");
    String futureStatus = futureResponse.getString("status");
    if (!"OK".equals(nowStatus) || !"OK".equals(futureStatus)) {
      Log.e(TAG, String.format("Error status in response: %s %s", nowStatus, futureStatus));
      return null;
    }

    // Expect exactly one route.
    JSONArray nowRoutes = nowResponse.getJSONArray("routes");
    JSONObject nowRoute = nowRoutes.getJSONObject(0);
    JSONArray futureRoutes = futureResponse.getJSONArray("routes");
    JSONObject futureRoute = futureRoutes.getJSONObject(0);

    // Expect exactly one leg.
    JSONArray nowLegs = nowRoute.getJSONArray("legs");
    JSONObject nowLeg = nowLegs.getJSONObject(0);
    JSONArray futureLegs = futureRoute.getJSONArray("legs");
    JSONObject futureLeg = futureLegs.getJSONObject(0);

    // Get the duration now, with traffic if available.
    JSONObject nowDuration;
    boolean nowHasTraffic = nowLeg.has("duration_in_traffic");
    if (nowHasTraffic) {
      nowDuration = nowLeg.getJSONObject("duration_in_traffic");
    } else {
      nowDuration = nowLeg.getJSONObject("duration");
    }
    String nowDurationText = nowDuration.getString("text");
    long nowDurationSeconds = nowDuration.getLong("value");
    Log.d(TAG, String.format("Duration now: %s (%s s) %b", nowDurationText,
        nowDurationSeconds, nowHasTraffic));

    // Get the duration in the future, with traffic if available.
    JSONObject futureDuration;
    boolean futureHasTraffic = futureLeg.has("duration_in_traffic");
    if (futureHasTraffic) {
      futureDuration = futureLeg.getJSONObject("duration_in_traffic");
    } else {
      futureDuration = futureLeg.getJSONObject("duration");
    }
    String futureDurationText = futureDuration.getString("text");
    long futureDurationSeconds = futureDuration.getLong("value");
    Log.d(TAG, String.format("Duration future: %s (%d secs) %b", futureDurationText,
        futureDurationSeconds, futureHasTraffic));

    // Create the text summary.
    String nowSummaryText = nowRoute.getString("summary");
    Log.d(TAG, "Summary text: " + nowSummaryText);
    String text;
    if (!TextUtils.isEmpty(nowSummaryText)) {
      text = String.format("%s via %s", nowDurationText, nowSummaryText);
    } else {
      text = nowDurationText;
    }

    // Pick the icon for the travel mode.
    String travelMode = context.getString(R.string.travel_mode);
    int travelModeIconResource;
    if (MODE_DRIVING.equals(travelMode)) {
      travelModeIconResource = R.drawable.driving;
    } else if (MODE_TRANSIT.equals(travelMode)) {
      travelModeIconResource = R.drawable.transit;
    } else if (MODE_WALKING.equals(travelMode)) {
      travelModeIconResource = R.drawable.walking;
    } else if (MODE_BICYCLING.equals(travelMode)) {
      travelModeIconResource = R.drawable.bicycling;
    } else {
      Log.e(TAG, "Unknown travel mode: " + travelMode);
      return null;
    }
    Log.d(TAG, "Using travel mode: " + travelMode);
    Drawable travelModeIcon = context.getDrawable(travelModeIconResource);

    // Check if there is a significant trend and use the corresponding icon.
    Drawable trafficTrendIcon;
    long trendSeconds = futureDurationSeconds - nowDurationSeconds;
    Log.d(TAG, String.format("Traffic trend: %d secs", trendSeconds));
    if (Math.abs(trendSeconds) >= TREND_THRESHOLD_SECONDS) {
      int trendIconResource = trendSeconds > 0 ? R.drawable.trend_up : R.drawable.trend_down;
      trafficTrendIcon = context.getDrawable(trendIconResource);
    } else {
      trafficTrendIcon = null;
    }

    return new CommuteSummary(text, travelModeIcon, trafficTrendIcon);
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
