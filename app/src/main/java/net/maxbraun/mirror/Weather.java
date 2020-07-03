package net.maxbraun.mirror;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.LruCache;

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

  /**
   * The time in milliseconds between API calls to update the weather.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

  /**
   * The base URL for all AccuWeather API requests.
   */
  private static final String ACCU_WEATHER_BASE_URL = "https://dataservice.accuweather.com";

  /**
   * The size of the location key cache. Should be big enough to cover all typical locations.
   */
  private static final int LOCATION_KEY_CACHE_SIZE = 10;

  /**
   * The context used to load string resources.
   */
  private final Context context;

  /**
   * A cache for the location key to avoid unnecessary API requests.
   */
  private final LruCache<Location, String> locationKeyCache = new LruCache(LOCATION_KEY_CACHE_SIZE);

  /**
   * A {@link Map} from AccuWeather's icon number to the corresponding drawable resource ID.
   * See: https://developer.accuweather.com/weather-icons
   */
  private final Map<Integer, Integer> iconResources = new HashMap<Integer, Integer>() {{
    put(1, R.drawable.clear_day);  // Sunny
    put(2, R.drawable.clear_day);  // Mostly Sunny
    put(3, R.drawable.partly_cloudy_day);  // Partly Sunny
    put(4, R.drawable.partly_cloudy_day);  // Intermittent Clouds
    put(5, R.drawable.partly_cloudy_day);  // Hazy Sunshine
    put(6, R.drawable.cloudy);  // Mostly Cloudy
    put(7, R.drawable.cloudy);  // Cloudy
    put(8, R.drawable.cloudy);  // Dreary (Overcast)
    put(11, R.drawable.fog);  // Fog
    put(12, R.drawable.rain);  // Showers
    put(13, R.drawable.rain);  // Mostly Cloudy w/ Showers
    put(14, R.drawable.rain);  // Partly Sunny w/ Showers
    put(15, R.drawable.rain);  // T-Storms
    put(16, R.drawable.rain);  // Mostly Cloudy w/ T-Storms
    put(17, R.drawable.rain);  // Partly Sunny w/ T-Storms
    put(18, R.drawable.rain);  // Rain
    put(19, R.drawable.snow);  // Flurries
    put(20, R.drawable.snow);  // Mostly Cloudy w/ Flurries
    put(21, R.drawable.snow);  // Partly Sunny w/ Flurries
    put(22, R.drawable.snow);  // Snow
    put(23, R.drawable.snow);  // Mostly Cloudy w/ Snow
    put(24, R.drawable.sleet);  // Ice
    put(25, R.drawable.sleet);  // Sleet
    put(26, R.drawable.sleet);  // Freezing Rain
    put(29, R.drawable.sleet);  // Rain and Snow
    // put(30, R.drawable.hot);  // Hot
    // put(31, R.drawable.cold);  // Cold
    put(32, R.drawable.wind);  // Windy
    put(33, R.drawable.clear_night);  // Clear
    put(34, R.drawable.clear_night);  // Mostly Clear
    put(35, R.drawable.partly_cloudy_night);  // Partly Cloudy
    put(36, R.drawable.partly_cloudy_night);  // Intermittent Clouds
    put(37, R.drawable.partly_cloudy_night);  // Hazy Moonlight
    put(38, R.drawable.partly_cloudy_night);  // Mostly Cloudy
    put(39, R.drawable.rain);  // Partly Cloudy w/ Showers
    put(40, R.drawable.rain);  // Mostly Cloudy w/ Showers
    put(41, R.drawable.rain);  // Partly Cloudy w/ T-Storms
    put(42, R.drawable.rain);  // Mostly Cloudy w/ T-Storms
    put(43, R.drawable.snow);  // Mostly Cloudy w/ Flurries
    put(44, R.drawable.snow);  // Mostly Cloudy w/ Snow
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
     * A human-readable summary of the 24-hour forecast.
     */
    public final String forecastSummary;

    /**
     * The average precipitation probability during the 24-hour forecast as a value between 0 and 1.
     */
    public final double precipitationProbability;

    /**
     * The resource ID of the icon representing the current weather conditions.
     */
    public final int currentIcon;

    public WeatherData(double currentTemperature, String forecastSummary,
        double precipitationProbability, int currentIcon) {
      this.currentTemperature = currentTemperature;
      this.forecastSummary = forecastSummary;
      this.precipitationProbability = precipitationProbability;
      this.currentIcon = currentIcon;
    }
  }

  public Weather(Context context, UpdateListener<WeatherData> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);
    this.context = context;
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

    // Convert the location to a location key required by the API requests.
    String locationKey = getLocationKey(location);

    // Get the latest data from the AccuWeather API.
    try {
      String currentRequestUrl = getCurrentRequestUrl(locationKey);
      String forecastRequestUrl = getForecastRequestUrl(locationKey);

      JSONArray currentResponse = Network.getJsonArray(currentRequestUrl);
      if (currentResponse == null) {
        return null;
      }
      JSONObject forecastResponse = Network.getJsonObject(forecastRequestUrl);
      if (forecastResponse == null) {
        return null;
      }

      // Parse the data we are interested in from the response JSON.
      double currentTemperature = currentResponse
          .getJSONObject(0)
          .getJSONObject("Temperature")
          .getJSONObject("Imperial")
          .getDouble("Value");
      String forecastSummary = forecastResponse
          .getJSONObject("Headline")
          .getString("Text");
      double dayPrecipitationProbability = forecastResponse
          .getJSONArray("DailyForecasts")
          .getJSONObject(0)
          .getJSONObject("Day")
          .getInt("PrecipitationProbability") / 100;
      double nightPrecipitationProbability = forecastResponse
          .getJSONArray("DailyForecasts")
          .getJSONObject(0)
          .getJSONObject("Night")
          .getInt("PrecipitationProbability") / 100;
      double precipitationProbability =
          (dayPrecipitationProbability + nightPrecipitationProbability) / 2;
      int currentIcon = currentResponse
          .getJSONObject(0)
          .getInt("WeatherIcon");

      return new WeatherData(
          currentTemperature,
          forecastSummary,
          precipitationProbability,
          iconResources.get(currentIcon)
      );
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse weather JSON.", e);
      return null;
    }
  }

  /**
   * Retrieves the location key for a particular latitude and longitude or uses a cached version or
   * returns {@code null} if the request fails.
   */
  private String getLocationKey(Location location) {
    if (location == null) {
      return null;
    }

    // Try the cache first.
    String cachedLocationKey = locationKeyCache.get(location);
    if (cachedLocationKey != null) {
      Log.d(TAG, String.format("Using cached location key: %s -> %s", location, cachedLocationKey));
      return cachedLocationKey;
    }

    Log.d(TAG, "Requesting location key.");
    String requestUrl = String.format(
        Locale.US,
        "%s/locations/v1/cities/geoposition/search?apikey=%s&q=%f,%f",
        ACCU_WEATHER_BASE_URL,
        context.getString(R.string.accu_weather_api_key),
        location.getLatitude(),
        location.getLongitude());

    try {
      JSONObject response = Network.getJsonObject(requestUrl);
      if (response == null) {
        return null;
      }

      String locationKey =  response.getString("Key");
      Log.d(TAG, "Using location key: " + locationKey);
      locationKeyCache.put(location, locationKey);
      return locationKey;
    } catch (JSONException e) {
      Log.e(TAG, "Failed to parse location key JSON.", e);
      return null;
    }
  }

  /**
   * Creates the URL for an AccuWeather API request for the current conditions based on the
   * specified location key or {@code null} if the location is unknown.
   */
  private String getCurrentRequestUrl(String locationKey) {
    if (locationKey == null) {
      return null;
    }

    return String.format(
        Locale.US,
        "https://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s",
        locationKey,
        context.getString(R.string.accu_weather_api_key));
  }

  /**
   * Creates the URL for an AccuWeather API request for the daily forecast based on the specified
   * location key or {@code null} if the location is unknown.
   */
  private String getForecastRequestUrl(String locationKey) {
    if (locationKey == null) {
      return null;
    }

    return String.format(
        Locale.US,
        "https://dataservice.accuweather.com/forecasts/v1/daily/1day/%s?apikey=%s&details=true",
        locationKey,
        context.getString(R.string.accu_weather_api_key));
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
