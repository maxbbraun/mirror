package net.maxbraun.mirror;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Utility methods.
 */
public class Util {

  private final Context context;

  public Util(Context context) {
    this.context = context;
  }

  /**
   * Ensures that the navigation bar is hidden.
   */
  public void hideNavigationBar(View view) {
    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  /**
   * Launches the system's default settings activity.
   */
  public void launchSettings() {
    Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
    context.startActivity(settingsIntent);
  }

  /**
   * Shows a {@link Toast} with the IPv4 address of the Wifi connection. Useful for debugging,
   * especially when using adb over Wifi.
   */
  @SuppressWarnings("deprecation")
  public void showIpAddress() {
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    String ipAddress = null;
    if (wifiInfo != null) {
      ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
    }
    if (ipAddress == null) {
      ipAddress = context.getString(R.string.unknown_ip_address);
    }
    Toast.makeText(context, ipAddress, Toast.LENGTH_LONG).show();
  }

  /**
   * Uses some standard button presses for easy debugging.
   */
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_DOWN:
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
   * Removes the period from the end of a sentence, if there is one.
   */
  public String stripPeriod(String sentence) {
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
