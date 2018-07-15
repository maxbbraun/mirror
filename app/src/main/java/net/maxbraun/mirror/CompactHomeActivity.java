package net.maxbraun.mirror;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import net.maxbraun.mirror.DataUpdater.UpdateListener;
import net.maxbraun.mirror.Weather.WeatherData;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A compact version of {@link HomeActivity}.
 */
public class CompactHomeActivity extends Activity {

  /**
   * Available elements for the UI.
   */
  private enum UiElement {
    WEATHER,
    TIME,
    COMMUTE,
    BODY,
  }

  /**
   * The set of currently active UI elements.
   */
  private Set<UiElement> uiElements = new HashSet<UiElement>() {{
    add(UiElement.TIME);
    add(UiElement.COMMUTE);
  }};

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

  /**
   * The listener used to populate the UI with the commute summary.
   */
  private final UpdateListener<Commute.CommuteSummary> commuteUpdateListener =
      new UpdateListener<Commute.CommuteSummary>() {
        @Override
        public void onUpdate(Commute.CommuteSummary summary) {
          if (summary != null) {
            commuteTextView.setText(summary.text);
            commuteTextView.setVisibility(View.VISIBLE);
            travelModeView.setImageDrawable(summary.travelModeIcon);
            travelModeView.setVisibility(View.VISIBLE);
            if (summary.trafficTrendIcon != null) {
              trafficTrendView.setImageDrawable(summary.trafficTrendIcon);
              trafficTrendView.setVisibility(View.VISIBLE);
            } else {
              trafficTrendView.setVisibility(View.GONE);
            }
          } else {
            commuteTextView.setVisibility(View.GONE);
            travelModeView.setVisibility(View.GONE);
            trafficTrendView.setVisibility(View.GONE);
          }
        }
      };

  /**
   * The listener used to populate the UI with body measurements.
   */
  private final UpdateListener<Body.BodyMeasure[]> bodyUpdateListener =
      new UpdateListener<Body.BodyMeasure[]>() {
        @Override
        public void onUpdate(Body.BodyMeasure[] bodyMeasures) {
          if (bodyMeasures != null) {
            bodyView.setBodyMeasures(bodyMeasures);
            bodyView.setVisibility(View.VISIBLE);
          } else {
            bodyView.setVisibility(View.GONE);
          }
        }
      };

  private TextView temperatureView;
  private ImageView iconView;
  private TextClock timeView;
  private TextView commuteTextView;
  private ImageView travelModeView;
  private ImageView trafficTrendView;
  private BodyView bodyView;

  private Weather weather;
  private Commute commute;
  private Body body;
  private Util util;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_compact);

    // Weather
    temperatureView = (TextView) findViewById(R.id.temperature);
    iconView = (ImageView) findViewById(R.id.icon);
    findViewById(R.id.weather)
        .setVisibility(uiElements.contains(UiElement.WEATHER) ? View.VISIBLE : View.GONE);
    if (uiElements.contains(UiElement.WEATHER)) {
      weather = new Weather(this, weatherUpdateListener);
    }

    // Time
    timeView = (TextClock) findViewById(R.id.time);
    timeView.setVisibility(uiElements.contains(UiElement.TIME) ? View.VISIBLE : View.GONE);

    // Commute
    commuteTextView = (TextView) findViewById(R.id.commuteText);
    travelModeView = (ImageView) findViewById(R.id.travelMode);
    trafficTrendView = (ImageView) findViewById(R.id.trafficTrend);
    findViewById(R.id.commute)
        .setVisibility(uiElements.contains(UiElement.COMMUTE) ? View.VISIBLE : View.GONE);
    if (uiElements.contains(UiElement.COMMUTE)) {
      commute = new Commute(this, commuteUpdateListener);
    }

    // Body
    bodyView = (BodyView) findViewById(R.id.body);
    bodyView.setVisibility(uiElements.contains(UiElement.BODY) ? View.VISIBLE : View.GONE);
    if (uiElements.contains(UiElement.BODY)) {
      body = new Body(this, bodyUpdateListener);
    }

    util = new Util(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (uiElements.contains(UiElement.WEATHER)) {
      weather.start();
    }
    if (uiElements.contains(UiElement.COMMUTE)) {
      commute.start();
    }
    if (uiElements.contains(UiElement.BODY)) {
      body.start();
    }
  }

  @Override
  protected void onStop() {
    if (uiElements.contains(UiElement.WEATHER)) {
      weather.stop();
    }
    if (uiElements.contains(UiElement.COMMUTE)) {
      commute.stop();
    }
    if (uiElements.contains(UiElement.BODY)) {
      body.stop();
    }
    super.onStop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    util.hideNavigationBar(timeView);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return util.onKeyUp(keyCode, event);
  }
}
