package net.maxbraun.mirror;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} for {@link Intent#ACTION_BOOT_COMPLETED}.
 */
public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {

    // Launch HomeActivity once boot is complete. This is useful on platforms where we aren't
    // allowed to replace the launcher, e.g. an unrooted Fire TV Stick.
    Intent homeIntent = new Intent(context, HomeActivity.class);
    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(homeIntent);
  }
}
