package net.maxbraun.mirror;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import net.maxbraun.mirror.Body.BodyMeasure;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class to regularly retrieve body measurements.
 */
public class Body extends DataUpdater<BodyMeasure[]> {
  private static final String TAG = Body.class.getSimpleName();

  /**
   * The time in milliseconds between API calls to update the body measures.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

  /**
   * The activity used to load string resources and access shared preferences.
   */
  private final Activity activity;

  /**
   * The OAuth keys for the Nokia Health API.
   */
  private final Network.OAuthDataProvider nokiaHealthApiData = new Network.OAuthDataProvider() {
    @Override
    public String getClientId() {
      return activity.getString(R.string.nokia_health_client_id);
    }

    @Override
    public String getClientSecret() {
      return activity.getString(R.string.nokia_health_client_secret);
    }

    @Override
    public String getRefreshToken() {
      return activity.getString(R.string.nokia_health_refresh_token);
    }

    @Override
    public String getServiceId() {
      return "nokia_health_api";
    }
  };

  /**
   * A timestamped body measure data point.
   */
  public static class BodyMeasure {

    /**
     * The unix timestamp in seconds when the measure was taken.
     */
    public final long timestamp;

    /**
     * The body weight in kilograms.
     */
    public final double weight;

    public BodyMeasure(long timestamp, double weight) {
      this.timestamp = timestamp;
      this.weight = weight;
    }
  }

  public Body(Activity activity, UpdateListener<BodyMeasure[]> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
    this.activity = activity;
  }

  @Override
  protected BodyMeasure[] getData() {
    // Get the latest data from the Nokia Health API.
    String requestUrl = getRequestUrl();

    // Parse the data we are interested in from the response JSON.
    try {
      JSONObject response = Network.getJson(activity, requestUrl, NokiaHealthApi.instance(),
          nokiaHealthApiData);
      if (response != null) {
        return parseBodyMeasures(response);
      } else {
        return null;
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse body JSON.", e);
      return null;
    }
  }

  /**
   * Reads the body measure data points from the API response. API documentation:
   * https://developer.health.nokia.com/api/doc
   */
  private static BodyMeasure[] parseBodyMeasures(JSONObject response) throws JSONException {
    int status = response.getInt("status");
    if (status != 0) {
      Log.e(TAG, "Error status in response: " + status);
      return null;
    }

    JSONObject body = response.getJSONObject("body");
    JSONArray measureGroups = body.getJSONArray("measuregrps");

    // Iterate over all measures in the response.
    List<BodyMeasure> bodyMeasures = new ArrayList<>();
    for (int i = 0; i < measureGroups.length(); i++) {
      JSONObject measureGroup = measureGroups.getJSONObject(i);
      long date = measureGroup.getLong("date");
      JSONArray measures = measureGroup.getJSONArray("measures");
      for (int j = 0; j < measures.length(); j++) {
        JSONObject measure = measures.getJSONObject(j);

        // We only care about the weight.
        int type = measure.getInt("type");
        if (type != 1) {
          continue;
        }

        // Decode the weight.
        int value = measure.getInt("value");
        int unit = measure.getInt("unit");
        double weight = value * Math.pow(10, unit);

        // Add this measure to the list.
        BodyMeasure bodyMeasure = new BodyMeasure(date, weight);
        bodyMeasures.add(bodyMeasure);
      }
    }

    // Make sure the measures are sorted by ascending timestamp.
    Collections.sort(bodyMeasures, (lhs, rhs) -> Long.compare(lhs.timestamp, rhs.timestamp));

    return bodyMeasures.toArray(new BodyMeasure[bodyMeasures.size()]);
  }

  /**
   * Creates the URL for a Nokia Health API request based on the current time.
   */
  private String getRequestUrl() {
    return String.format(Locale.US, "https://api.health.nokia.com/measure" +
        "?action=getmeas" +
        "&startdate=%d",
        getStartTimestamp());
  }

  /**
   * Calculates the start timestamp (six months before today) in Unix epoch seconds.
   */
  private static long getStartTimestamp() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.add(Calendar.MONTH, -6);
    return calendar.getTime().getTime() / 1000;
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
