package net.maxbraun.mirror;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.maxbraun.mirror.DataUpdater.UpdateListener;
import net.maxbraun.mirror.Weather.WeatherData;

import java.util.Locale;

/**
 * A compact version of {@link HomeActivity}.
 */
public class CompactHomeActivity extends Activity {

  /**
   * The listener used to populate the UI with weather data.
   */
  private final UpdateListener<WeatherData> weatherUpdateListener =
      new UpdateListener<WeatherData>() {
    @Override
    public void onUpdate(WeatherData data) {
      if (data != null) {

        // Populate the current temperature rounded to a whole number.
        String temperature = String.format(Locale.US, "%d°", Math.round(data.currentTemperature));
        temperatureView.setText(temperature);

        // Populate the icon for the current weather.
        iconView.setImageResource(data.currentIcon);

        // Show all the views.
        temperatureView.setVisibility(View.VISIBLE);
        iconView.setVisibility(View.VISIBLE);
      } else {

        // Hide everything if there is no data.
        temperatureView.setVisibility(View.GONE);
        iconView.setVisibility(View.GONE);
      }
    }
  };

  private TextView temperatureView;
  private ImageView iconView;

  private Weather weather;
  private Util util;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_compact);

    temperatureView = (TextView) findViewById(R.id.temperature);
    iconView = (ImageView) findViewById(R.id.icon);

    weather = new Weather(this, weatherUpdateListener);
    util = new Util(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    weather.start();
  }

  @Override
  protected void onStop() {
    weather.stop();
    super.onStop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    util.hideNavigationBar(temperatureView);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return util.onKeyUp(keyCode, event);
  }
}
