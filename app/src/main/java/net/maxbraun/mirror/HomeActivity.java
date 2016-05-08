package net.maxbraun.mirror;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import net.maxbraun.mirror.DataUpdater.UpdateListener;
import net.maxbraun.mirror.Weather.WeatherData;

/**
 * The main {@link Activity} class and entry point into the UI.
 */
public class HomeActivity extends Activity {

  /**
   * The IDs of {@link TextView TextViews} in {@link R.layout#activity_home} which contain the news
   * headlines.
   */
  private static final int[] NEWS_VIEW_IDS = new int[]{
      R.id.news_1,
      R.id.news_2,
      R.id.news_3,
      R.id.news_4,
  };

  /**
   * The listener used to populate the UI with weather data.
   */
  private final UpdateListener<WeatherData> weatherUpdateListener =
      new UpdateListener<WeatherData>() {
    @Override
    public void onUpdate(WeatherData data) {
      if (data != null) {

        // Populate the current temperature rounded to a whole number.
        String temperature = String.format("%d°", Math.round(data.currentTemperature));
        temperatureView.setText(temperature);

        // Populate the 24-hour forecast summary, but strip any period at the end.
        String summary = stripPeriod(data.daySummary);
        weatherSummaryView.setText(summary);

        // Populate the precipitation probability as a percentage rounded to a whole number.
        String precipitation =
            String.format("%d%%", Math.round(100 * data.dayPrecipitationProbability));
        precipitationView.setText(precipitation);

        // Populate the icon for the current weather.
        iconView.setImageResource(data.currentIcon);

        // Show all the views.
        temperatureView.setVisibility(View.VISIBLE);
        weatherSummaryView.setVisibility(View.VISIBLE);
        precipitationView.setVisibility(View.VISIBLE);
        iconView.setVisibility(View.VISIBLE);
      } else {

        // Hide everything if there is no data.
        temperatureView.setVisibility(View.GONE);
        weatherSummaryView.setVisibility(View.GONE);
        precipitationView.setVisibility(View.GONE);
        iconView.setVisibility(View.GONE);
      }
    }
  };

  /**
   * The listener used to populate the UI with news headlines.
   */
  private final UpdateListener<List<String>> newsUpdateListener =
      new UpdateListener<List<String>>() {
    @Override
    public void onUpdate(List<String> headlines) {

      // Populate the views with as many headlines as we have and hide the others.
      for (int i = 0; i < NEWS_VIEW_IDS.length; i++) {
        if ((headlines != null) && (i < headlines.size())) {
          newsViews[i].setText(headlines.get(i));
          newsViews[i].setVisibility(View.VISIBLE);
        } else {
          newsViews[i].setVisibility(View.GONE);
        }
      }
    }
  };

  private TextView temperatureView;
  private TextView weatherSummaryView;
  private TextView precipitationView;
  private ImageView iconView;
  private TextView[] newsViews = new TextView[NEWS_VIEW_IDS.length];

  private Weather weather;
  private News news;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    temperatureView = (TextView) findViewById(R.id.temperature);
    weatherSummaryView = (TextView) findViewById(R.id.weather_summary);
    precipitationView = (TextView) findViewById(R.id.precipitation);
    iconView = (ImageView) findViewById(R.id.icon);
    for (int i = 0; i < NEWS_VIEW_IDS.length; i++) {
      newsViews[i] = (TextView) findViewById(NEWS_VIEW_IDS[i]);
    }

    weather = new Weather(weatherUpdateListener);
    news = new News(newsUpdateListener);
  }

  @Override
  protected void onStart() {
    super.onStart();
    weather.start();
    news.start();
  }

  @Override
  protected void onStop() {
    weather.stop();
    news.stop();
    super.onStop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    hideNavigationBar();
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // Use some standard button presses for easy debugging.
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_CENTER:
        launchSettings();
        return true;
      case KeyEvent.KEYCODE_DPAD_UP:
        showIpAddress();
        return true;
      default:
        return false;
    }
  }

  /**
   * Ensures that the navigation bar is hidden.
   */
  private void hideNavigationBar() {
    temperatureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  /**
   * Launches the system's default settings activity.
   */
  private void launchSettings() {
    Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
    startActivity(settingsIntent);
  }

  /**
   * Shows a {@link Toast} with the IPv4 address of the Wifi connection. Useful for debugging,
   * especially when using adb over Wifi.
   */
  @SuppressWarnings("deprecation")
  private void showIpAddress() {
    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    String ipAddress = null;
    if (wifiInfo != null) {
      ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
    }
    if (ipAddress == null) {
      ipAddress = getString(R.string.unknown_ip_address);
    }
    Toast.makeText(this, ipAddress, Toast.LENGTH_LONG).show();
  }

  /**
   * Removes the period from the end of a sentence, if there is one.
   */
  private String stripPeriod(String sentence) {
    if (sentence == null) {
      return null;
    }
    if ((sentence.length() > 0) && (sentence.charAt(sentence.length() - 1) == '.')) {
      return sentence.substring(0, sentence.length() - 1);
    } else {
      return sentence;
    }
  }
}
