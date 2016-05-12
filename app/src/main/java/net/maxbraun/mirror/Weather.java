package net.maxbraun.mirror;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.maxbraun.mirror.Weather.WeatherData;

/**
 * A helper class to regularly retrieve weather information.
 */
public class Weather extends DataUpdater<WeatherData> {
  private static final String TAG = Weather.class.getSimpleName();

  // TODO: Replace the API key with a valid one from https://developer.forecast.io
  /**
   * The key used for the Forecast API.
   */
  private static final String FORECAST_IO_API_KEY = "bf2d9e782a47a7bb172f7800f73c566f";

  /**
   * The time in milliseconds between API calls to update the weather.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

  /**
   * A {@link Map} from Forecast's icon code to the corresponding drawable resource ID.
   */
  private final Map<String, Integer> iconResources = new HashMap<String, Integer>() {{
    put("clear-day", R.drawable.clear_day);
    put("clear-night", R.drawable.clear_night);
    put("cloudy", R.drawable.cloudy);
    put("fog", R.drawable.fog);
    put("partly-cloudy-day", R.drawable.partly_cloudy_day);
    put("partly-cloudy-night", R.drawable.partly_cloudy_night);
    put("rain", R.drawable.rain);
    put("sleet", R.drawable.sleet);
    put("snow", R.drawable.snow);
    put("wind", R.drawable.wind);
  }};

  /**
   * The current location, which is assumed to be static.
   */
  private Location location;

  /**
   * The data structure containing the weather information we are interested in.
   */
  public class WeatherData {

    /**
     * The current temperature in degrees Fahrenheit.
     */
    public final double currentTemperature;

    /**
     * The current precipitation probability as a value between 0 and 1.
     */
    public final double currentPrecipitationProbability;

    /**
     * A human-readable summary of the 24-hour forecast.
     */
    public final String daySummary;

    /**
     * The average precipitation probability during the 24-hour forecast as a value between 0 and 1.
     */
    public final double dayPrecipitationProbability;

    /**
     * The resource ID of the icon representing the current weather conditions.
     */
    public final int currentIcon;

    /**
     * The resource ID of the icon representing the weather conditions during the 24-hour forecast.
     */
    public final int dayIcon;

    public WeatherData(double currentTemperature, double currentPrecipitationProbability,
        String daySummary, double dayPrecipitationProbability, int currentIcon, int dayIcon) {
      this.currentTemperature = currentTemperature;
      this.currentPrecipitationProbability = currentPrecipitationProbability;
      this.daySummary = daySummary;
      this.dayPrecipitationProbability = dayPrecipitationProbability;
      this.currentIcon = currentIcon;
      this.dayIcon = dayIcon;
    }
  }

  public Weather(UpdateListener<WeatherData> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
  }

  @Override
  protected WeatherData getData() {

    // Lazy load the location.
    if (location == null) {
      // We're using geo location by IP, because many headless Android devices don't return anything
      // useful through the usual location APIs.
      location = GeoLocation.getLocation();
      Log.d(TAG, "Using location for weather: " + location);
    }


    // Get the latest data from the Forecast API.
    String requestUrl = getRequestUrl(location);

    // Parse the data we are interested in from the response JSON.
    // Forecast API documentation: https://developer.forecast.io/docs/v2
    try {
      JSONObject response = makeRequest(requestUrl);
      if (response != null) {
        return new WeatherData(
            parseCurrentTemperature(response),
            parseCurrentPrecipitationProbability(response),
            parseDaySummary(response),
            parseDayPrecipitationProbability(response),
            parseCurrentIcon(response),
            parseDayIcon(response));
      } else {
        return null;
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse weather JSON.", e);
      return null;
    }
  }

  /**
   * Creates the URL for a Forecast API request based on the specified {@link Location} or
   * {@code null} if the location is unknown.
   */
  private static String getRequestUrl(Location location) {
    if (location != null) {
      return String.format(Locale.ENGLISH, "https://api.forecast.io/forecast/%s/%f,%f?units=auto", FORECAST_IO_API_KEY,
                           location.getLatitude(), location.getLongitude());
    } else {
      return null;
    }
  }

  /**
   * Makes a network request at the specified URL, expecting a JSON response.
   */
  private static JSONObject makeRequest(String requestUrl) throws JSONException {
    String response = Network.get(requestUrl);
    if (response != null) {
      return new JSONObject(response);
    } else {
      Log.w(TAG, "Empty response.");
      return null;
    }
  }

  /**
   * Reads the current temperature from the API response.
   */
  private Double parseCurrentTemperature(JSONObject response) throws JSONException {
    JSONObject currently = response.getJSONObject("currently");
    return currently.getDouble("temperature");
  }

  /**
   * Reads the current precipitation probability from the API response.
   */
  private Double parseCurrentPrecipitationProbability(JSONObject response) throws JSONException {
    JSONObject currently = response.getJSONObject("currently");
    return currently.getDouble("precipProbability");
  }

  /**
   * Reads the 24-hour forecast summary from the API response.
   */
  private String parseDaySummary(JSONObject response) throws JSONException {
    JSONObject hourly = response.getJSONObject("hourly");
    return hourly.getString("summary");
  }

  /**
   * Reads the 24-hour forecast precipitation probability from the API response.
   */
  private Double parseDayPrecipitationProbability(JSONObject response) throws JSONException {
    JSONObject hourly = response.getJSONObject("hourly");
    JSONArray data = hourly.getJSONArray("data");

    // Calculate the average over the whole day.
    double sum = 0;
    for (int i = 0; i < data.length(); i++) {
      double probability = data.getJSONObject(i).getDouble("precipProbability");
      sum += probability;
    }
    return sum / data.length();
  }

  /**
   * Reads the current weather icon code from the API response.
   */
  private Integer parseCurrentIcon(JSONObject response) throws JSONException {
    JSONObject currently = response.getJSONObject("currently");
    String icon = currently.getString("icon");
    return iconResources.get(icon);
  }

  /**
   * Reads the 24-hour forecast weather icon code from the API response.
   */
  private Integer parseDayIcon(JSONObject response) throws JSONException {
    JSONObject hourly = response.getJSONObject("hourly");
    String icon = hourly.getString("icon");
    return iconResources.get(icon);
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
