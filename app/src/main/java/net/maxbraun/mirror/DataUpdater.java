package net.maxbraun.mirror;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An abstract class that continuously queries a data source on a background thread via
 * {@link #getData()} and updates the {@link UpdateListener} on the main thread with the result.
 */
public abstract class DataUpdater<Data> {

  /**
   * The {@link ScheduledExecutorService} used to query data on a background thread. All requests
   * are handled sequentially.
   */
  private final ScheduledExecutorService scheduledBackgroundExecutor =
      Executors.newSingleThreadScheduledExecutor();

  /**
   * The current task on the {@link #scheduledBackgroundExecutor} or {@code null} if there is none.
   */
  private ScheduledFuture updateTask;

  /**
   * A {@link Handler} on the main thread.
   */
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  /**
   * The {@link UpdateListener} called each time there is new data.
   */
  private final UpdateListener<Data> updateListener;

  /**
   * The time in milliseconds between each update.
   */
  private final long updateIntervalMillis;

  /**
   * The generic interface used for data updates.
   */
  public interface UpdateListener<Data> {

    /**
     * Called when there is new data.
     *
     * @param data The latest {@link Data} or {@code null} if there was an error.
     */
    void onUpdate(Data data);
  }

  /**
   * When creating a new {@link DataUpdater}, provide a non-{@code null} {@link UpdateListener} and
   * an update interval in milliseconds.
   */
  public DataUpdater(UpdateListener<Data> updateListener, long updateIntervalMillis) {
    if (updateListener == null) {
      throw new IllegalArgumentException("A listener is required.");
    }

    this.updateListener = updateListener;
    this.updateIntervalMillis = updateIntervalMillis;
  }

  /**
   * Starts the regular background updates.
   */
  public void start() {
    Log.d(getTag(), "Starting.");

    // Remember the task so we can cancel it later.
    updateTask = scheduledBackgroundExecutor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        update();
      }
    }, 0, updateIntervalMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Stops the regular background updates.
   */
  public void stop() {
    Log.d(getTag(), "Stopping.");

    // If there is a running task, cancel any repetition while allowing the current one to finish.
    if (updateTask != null) {
      updateTask.cancel(false);
      updateTask = null;
    }
  }

  /**
   * Performs the update by retrieving the data and updating the listener.
   */
  private void update() {
    Log.d(getTag(), "Updating.");

    final Data data = getData();
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        updateListener.onUpdate(data);
      }
    });
  }

  /**
   * Implement this to query the data source and return a {@link Data} instance or {@code null}.
   */
  protected abstract Data getData();

  /**
   * Implement this to provide a tag for logging.
   */
  protected abstract String getTag();
}
