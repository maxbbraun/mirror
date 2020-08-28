package net.maxbraun.mirror;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.maxbraun.mirror.Air.AirData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class to regularly retrieve air quality information.
 */
public class Air extends DataUpdater<AirData> {
  private static final String TAG = Air.class.getSimpleName();

  /**
   * The time in milliseconds between API calls to update the air quality.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);

  /**
   * The base URL for all AirNow API requests.
   */
  private static final String AIR_NOW_BASE_URL = "https://www.airnowapi.org";

  /**
   * The context used to load string resources.
   */
  private final Context context;

  /**
   * A {@link Map} from the air quality index category the corresponding drawable resource ID.
   */
  private final Map<Integer, Integer> iconResources = new HashMap<Integer, Integer>() {{
    put(1, R.drawable.aqi_good);
    put(2, R.drawable.aqi_moderate);
    put(3, R.drawable.aqi_usg);
    put(4, R.drawable.aqi_unhealthy);
    put(5, R.drawable.aqi_very_unhealthy);
    put(6, R.drawable.aqi_hazardous);
  }};

  /**
   * The data structure containing the air quality information we are interested in.
   */
  public class AirData {

    /**
     * The air quality index number.
     */
    public final int aqi;

    /**
     * The air quality index category name.
     */
    public final String category;

    /**
     * The air quality index category icon.
     */
    public final int icon;

    public AirData(int aqi, String category, int icon) {
      this.aqi = aqi;
      this.category = category;
      this.icon = icon;
    }
  }

  public Air(Context context, UpdateListener<AirData> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
    this.context = context;
  }

  @Override
  protected AirData getData() {
    Location location = GeoLocation.getLocation();
    Log.d(TAG, "Using location for air quality: " + location);

    // Get the latest data from the AirNow API.
    try {
      String requestUrl = getRequestUrl(location);

      JSONArray response = Network.getJsonArray(requestUrl);
      if (response == null) {
        return null;
      }

      // Parse the data we are interested in from the response JSON.
      JSONObject forecast = response.getJSONObject(0);
      int aqi = forecast
          .getInt("AQI");
      String category = forecast
          .getJSONObject("Category")
          .getString("Name");
      int categoryNumber = forecast
          .getJSONObject("Category")
          .getInt("Number");

      return new AirData(
          aqi,
          category,
          iconResources.get(categoryNumber)
      );
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse air quality JSON.", e);
      return null;
    }
  }

  /**
   * Creates the URL for an AirNow API request based on the specified location or {@code null} if
   * the location is unknown.
   */
  private String getRequestUrl(Location location) {
    if (location == null) {
      return null;
    }

    return String.format(
        Locale.US,
        "%s/aq/forecast/latLong/" +
            "?format=application/json" +
            "&latitude=%f" +
            "&longitude=%f" +
            "&API_KEY=%s",
        AIR_NOW_BASE_URL,
        location.getLatitude(),
        location.getLongitude(),
        context.getString(R.string.air_now_api_key));
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
