package net.maxbraun.mirror;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.maxbraun.mirror.DataUpdater.UpdateListener;
import net.maxbraun.mirror.Weather.WeatherData;

import java.util.Locale;

/**
 * A compact version of {@link HomeActivity}.
 */
public class CompactHomeActivity extends Activity {
  private static final String TAG = CompactHomeActivity.class.getSimpleName();

  /**
   * The path to the Firebase Database for the UI settings.
   */
  private static final String UI_SETTINGS_PATH = "compact_ui_settings";

  /**
   * The child path under {@link #UI_SETTINGS_PATH} for the weather boolean.
   */
  private static final String UI_SETTING_WEATHER = "weather";

  /**
   * The child path under {@link #UI_SETTINGS_PATH} for the time boolean.
   */
  private static final String UI_SETTING_TIME = "time";

  /**
   * The child path under {@link #UI_SETTINGS_PATH} for the commute boolean.
   */
  private static final String UI_SETTING_COMMUTE = "commute";

  /**
   * The child path under {@link #UI_SETTINGS_PATH} for the body boolean.
   */
  private static final String UI_SETTING_BODY = "body";

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

  /**
   * The listener for Firebase Database UI settings updates.
   */
  private ValueEventListener uiSettingsListener = new ValueEventListener() {
    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
      Boolean weatherSetting = dataSnapshot.child(UI_SETTING_WEATHER).getValue(Boolean.class);
      if (weatherSetting != null && weatherSetting) {
        showWeather();
      } else {
        hideWeather();
      }

      Boolean timeSetting = dataSnapshot.child(UI_SETTING_TIME).getValue(Boolean.class);
      if (timeSetting != null && timeSetting) {
        showTime();
      } else {
        hideTime();
      }

      Boolean commuteSetting = dataSnapshot.child(UI_SETTING_COMMUTE).getValue(Boolean.class);
      if (commuteSetting != null && commuteSetting) {
        showCommute();
      } else {
        hideCommute();
      }

      Boolean bodySetting = dataSnapshot.child(UI_SETTING_BODY).getValue(Boolean.class);
      if (bodySetting != null && bodySetting) {
        showBody();
      } else {
        hideBody();
      }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
      Log.e(TAG, "Failed to load UI settings.", databaseError.toException());
    }
  };

  private View weatherView;
  private TextView temperatureView;
  private ImageView iconView;
  private TextClock timeView;
  private View commuteView;
  private TextView commuteTextView;
  private ImageView travelModeView;
  private ImageView trafficTrendView;
  private BodyView bodyView;

  private Weather weather;
  private Commute commute;
  private Body body;
  private Util util;
  private DatabaseReference uiSettings;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_compact);

    // Weather
    weatherView = findViewById(R.id.weather);
    temperatureView = findViewById(R.id.temperature);
    iconView = findViewById(R.id.icon);

    // Time
    timeView = findViewById(R.id.time);

    // Commute
    commuteView = findViewById(R.id.commute);
    commuteTextView = findViewById(R.id.commute_text);
    travelModeView = findViewById(R.id.travel_mode);
    trafficTrendView = findViewById(R.id.traffic_trend);

    // Body
    bodyView = findViewById(R.id.body);

    util = new Util(this);
    uiSettings = FirebaseDatabase.getInstance().getReference(UI_SETTINGS_PATH);
  }

  @Override
  protected void onStart() {
    super.onStart();

    // The listener will show the enabled UI elements.
    uiSettings.addValueEventListener(uiSettingsListener);
  }

  @Override
  protected void onStop() {
    uiSettings.removeEventListener(uiSettingsListener);

    hideWeather();
    hideCommute();
    hideBody();

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

  /**
   * Shows the weather UI and starts regular updates.
   */
  private void showWeather() {
    if (weather == null) {
      weather = new Weather(CompactHomeActivity.this, weatherUpdateListener);
      weather.start();
    }
    weatherView.setVisibility(View.VISIBLE);
  }

  /**
   * Hides the weather UI and stops regular updates.
   */
  private void hideWeather() {
    if (weather != null) {
      weather.stop();
      weather = null;
    }
    weatherView.setVisibility(View.GONE);
  }

  /**
   * Shows the time UI.
   */
  private void showTime() {
    timeView.setVisibility(View.VISIBLE);
  }

  /**
   * Hides the time UI.
   */
  private void hideTime() {
    timeView.setVisibility(View.GONE);
  }

  /**
   * Shows the commute UI and starts regular updates.
   */
  private void showCommute() {
    if (commute == null) {
      commute = new Commute(CompactHomeActivity.this, commuteUpdateListener);
      commute.start();
    }
    commuteView.setVisibility(View.VISIBLE);
  }

  /**
   * Hides the commute UI and stops regular updates.
   */
  private void hideCommute() {
    if (commute != null) {
      commute.stop();
      commute = null;
    }
    commuteView.setVisibility(View.GONE);
  }

  /**
   * Shows the body UI and starts regular updates.
   */
  private void showBody() {
    if (body == null) {
      body = new Body(CompactHomeActivity.this, bodyUpdateListener);
      body.start();
    }
    bodyView.setVisibility(View.VISIBLE);
  }

  /**
   * Hides the body UI and stops regular updates.
   */
  private void hideBody() {
    if (body != null) {
      body.stop();
      body = null;
    }
    bodyView.setVisibility(View.GONE);
  }
}
