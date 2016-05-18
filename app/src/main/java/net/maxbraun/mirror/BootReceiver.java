package net.maxbraun.mirror;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

/**
 * A {@link BroadcastReceiver} for {@link Intent#ACTION_BOOT_COMPLETED}.
 */
public class BootReceiver extends BroadcastReceiver {
  private static final String TAG = BootReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) {
      Log.w(TAG, "Unexpected action:" + action);
      return;
    }

    // Launch HomeActivity once boot is complete. This is useful on platforms where we aren't
    // allowed to replace the launcher, e.g. an unrooted Fire TV Stick. It's not necessary if we are
    // already default for the home intent.
    if (isMirrorHome(context)) {
      Log.d(TAG, "Already using mirror home.");
    } else {
      Log.d(TAG, "Starting mirror home.");
      Intent homeIntent = new Intent(context, HomeActivity.class);
      homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(homeIntent);
    }
  }

  /**
   * Determines whether a mirror activity is already default for the home intent.
   */
  private boolean isMirrorHome(Context context) {
    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
    homeIntent.addCategory(Intent.CATEGORY_HOME);
    PackageManager packageManager = context.getPackageManager();
    ResolveInfo homeInfo = packageManager.resolveActivity(homeIntent, 0);
    String homePackageName = homeInfo.activityInfo.packageName;
    return context.getPackageName().equals(homePackageName);
  }
}
