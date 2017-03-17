package net.maxbraun.mirror;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import net.maxbraun.mirror.Body.BodyMeasure;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A {@link View} charting the series of body measures defined by
 * {@link #setBodyMeasures(BodyMeasure[])}.
 */
public class BodyView extends View {
  private static final String TAG = BodyView.class.getSimpleName();

  /**
   * The conversion factor from kilograms to pounds.
   */
  private static final double KG_TO_LBS = 2.20462;

  /**
   * The {@link DateFormat} used with the 12 hour clock setting.
   */
  private static final SimpleDateFormat DATE_FORMAT_12H = new SimpleDateFormat("MMMM d");

  /**
   * The {@link DateFormat} used with the 24 hour clock setting.
   */
  private static final SimpleDateFormat DATE_FORMAT_24H = new SimpleDateFormat("d MMMM");

  /**
   * The {@link Paint} used to draw white dots.
   */
  private final Paint whiteDotPaint;

  /**
   * The {@link Paint} used to draw red dots.
   */
  private final Paint redDotPaint;

  /**
   * The {@link Paint} used to draw green dots.
   */
  private final Paint greenDotPaint;

  /**
   * The {@link Paint} used to draw the line.
   */
  private final Paint linePaint;

  /**
   * The {@link Paint} used to draw the labels.
   */
  private final Paint labelPaint;

  /**
   * The {@link Path} used to daw the line, which we reuse across {@link #onDraw(Canvas)} calls.
   */
  private final Path linePath = new Path();

  private final float dotRadiusPixels;
  private final float labelMarginPixels;

  private BodyMeasure[] bodyMeasures;

  /**
   * The minimum timestamp found in {@link #bodyMeasures}.
   */
  private long minTimestamp;

  /**
   * The maximum timestamp found in {@link #bodyMeasures}.
   */
  private long maxTimestamp;

  /**
   * The minimum weight found in {@link #bodyMeasures}.
   */
  private double minWeight;

  /**
   * The maximum weight found in {@link #bodyMeasures}.
   */
  private double maxWeight;

  /**
   * The weight with the maximum timestamp found in {@link #bodyMeasures}.
   */
  private double maxTimestampWeight;

  public BodyView(Context context) {
    this(context, null);
  }

  public BodyView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BodyView(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public BodyView(final Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    final Resources resources = context.getResources();

    dotRadiusPixels = getResources().getDimension(R.dimen.body_dot_radius);

    whiteDotPaint = new Paint() {{
      setColor(Color.WHITE);
      setAntiAlias(true);
      setStyle(Style.FILL);
    }};

    redDotPaint = new Paint() {{
      setColor(resources.getColor(R.color.red));
      setAntiAlias(true);
      setStyle(Style.FILL);
    }};

    greenDotPaint = new Paint() {{
      setColor(resources.getColor(R.color.green));
      setAntiAlias(true);
      setStyle(Style.FILL);
    }};

    final float lineWidthPixels = getResources().getDimension(R.dimen.body_line_width);
    linePaint = new Paint() {{
      setColor(Color.WHITE);
      setAntiAlias(true);
      setStyle(Style.STROKE);
      setStrokeWidth(lineWidthPixels);
      setStrokeCap(Cap.ROUND);
      setStrokeJoin(Join.ROUND);
    }};

    final float textSize = getResources().getDimension(R.dimen.small_text_size);
    labelPaint = new Paint() {{
      setColor(Color.WHITE);
      setAntiAlias(true);
      setTextSize(textSize);
      setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    }};

    labelMarginPixels = getResources().getDimension(R.dimen.body_label_margin);
  }

  /**
   * Updates the chart with the specified list of {@link BodyMeasure BodyMeasures}.
   */
  public void setBodyMeasures(BodyMeasure[] bodyMeasures) {
    this.bodyMeasures = bodyMeasures;
    Log.d(TAG, String.format("Showing %d body measures.",
        (bodyMeasures != null) ? bodyMeasures.length : 0));

    // Calculate the minimum and maximum timestamps and weights.
    if ((bodyMeasures != null) && (bodyMeasures.length > 0)) {
      BodyMeasure firstBodyMeasure = bodyMeasures[0];
      minTimestamp = firstBodyMeasure.timestamp;
      maxTimestamp = minTimestamp;
      minWeight = firstBodyMeasure.weight;
      maxWeight = minWeight;
      maxTimestampWeight = minWeight;

      for (int i = 1; i < bodyMeasures.length; i++) {
        BodyMeasure bodyMeasure = bodyMeasures[i];
        long timestamp = bodyMeasure.timestamp;
        double weight = bodyMeasure.weight;

        if (timestamp < minTimestamp) {
          minTimestamp = timestamp;
        }
        if (timestamp > maxTimestamp) {
          maxTimestamp = timestamp;
          maxTimestampWeight = weight;
        }

        if (weight < minWeight) {
          minWeight = weight;
        }
        if (weight > maxWeight) {
          maxWeight = weight;
        }
      }
    } else {
      minTimestamp = 0;
      maxTimestamp = 0;
      minWeight = 0.0;
      maxWeight = 0.0;
      maxTimestampWeight = 0.0;
    }

    // Trigger a redraw.
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    // Clear the canvas.
    canvas.drawColor(Color.TRANSPARENT);

    if ((bodyMeasures == null) || (bodyMeasures.length < 2)) {
      Log.w(TAG, "Not enough body measures.");
      return;
    }

    // Calculate the size of the label for the weight with the maximum timestamp now, because it
    // influences the right margin.
    String maxTimestampWeightLabel;
    float maxTimestampWeightLabelWidth;
    if ((maxTimestampWeight != minWeight) && (maxTimestampWeight != maxWeight)) {
      maxTimestampWeightLabel = String.format(Locale.US, "%.0f %s",
          getLocalizedWeight(maxTimestampWeight), getLocalizedWeightUnit());
      maxTimestampWeightLabelWidth = labelPaint.measureText(maxTimestampWeightLabel);
    } else {
      maxTimestampWeightLabel = null;
      maxTimestampWeightLabelWidth = 0.0f;
    }

    // Calculate the margins, which leave room for the dots, the labels, and additional margins.
    FontMetrics fontMetrics = labelPaint.getFontMetrics();
    float labelHeight = fontMetrics.descent - fontMetrics.ascent;
    float leftMargin = dotRadiusPixels;
    float topMargin = dotRadiusPixels + labelHeight + labelMarginPixels;
    float rightMargin = dotRadiusPixels + maxTimestampWeightLabelWidth + labelMarginPixels;
    float bottomMargin = dotRadiusPixels + labelHeight + labelMarginPixels;

    // Iterate over all measures to calculate the chart data.
    float maxWeightDotX = 0;
    float maxWeightDotY = 0;
    String maxWeightLabel = null;
    float maxWeightLabelX = 0;
    float maxWeightLabelY = 0;
    float minWeightDotX = 0;
    float minWeightDotY = 0;
    String minWeightLabel = null;
    float minWeightLabelX = 0;
    float minWeightLabelY = 0;
    float maxTimestampX = 0;
    float maxTimestampY = 0;
    linePath.rewind();
    for (int i = 0; i < bodyMeasures.length; i++) {
      BodyMeasure bodyMeasure = bodyMeasures[i];
      long timestamp = bodyMeasure.timestamp;
      double weight = bodyMeasure.weight;

      // Project the data point onto the available canvas.
      float x = project(timestamp, minTimestamp, maxTimestamp, leftMargin,
          canvas.getWidth() - rightMargin);
      float y = project((float) weight, (float) minWeight, (float) maxWeight,
          canvas.getHeight() - bottomMargin, topMargin);

      // Create a label with the weight and date, positioned as close to the data point as possible.
      String weightLabel = String.format(Locale.US, "%.0f %s · %s", getLocalizedWeight(weight),
          getLocalizedWeightUnit(), getLocalizedDate(timestamp));
      float weightLabelWidth = labelPaint.measureText(weightLabel);
      float weightLabelX = Math.min(Math.max(x - 0.5f * weightLabelWidth, 0.0f),
          canvas.getWidth() - weightLabelWidth);

      // Save the dot coordinates and the label for the maximum and minimum weights, but only once.
      // The weight with the maximum timestamp also gets a dot.
      if ((weight == maxWeight) && (maxWeightLabel == null)) {
        maxWeightDotX = x;
        maxWeightDotY = y;
        maxWeightLabelX = weightLabelX;
        maxWeightLabelY = labelHeight - fontMetrics.descent;
        maxWeightLabel = weightLabel;
      } else if ((weight == minWeight) && (minWeightLabel == null)) {
        minWeightDotX = x;
        minWeightDotY = y;
        minWeightLabelX = weightLabelX;
        minWeightLabelY = canvas.getHeight() - fontMetrics.descent;
        minWeightLabel = weightLabel;
      } else if (timestamp == maxTimestamp) {
        maxTimestampX = x;
        maxTimestampY = y;
      }

      // Append to the line.
      if (linePath.isEmpty()) {
        linePath.moveTo(x, y);
      } else {
        linePath.lineTo(x, y);
      }
    }

    // Draw the line.
    canvas.drawPath(linePath, linePaint);

    // Draw dots and labels for the maximum and minimum weights.
    if (maxWeightLabel != null) {
      canvas.drawCircle(maxWeightDotX, maxWeightDotY, dotRadiusPixels, redDotPaint);
      canvas.drawText(maxWeightLabel, maxWeightLabelX, maxWeightLabelY, labelPaint);
    }
    if (minWeightLabel != null) {
      canvas.drawCircle(minWeightDotX, minWeightDotY, dotRadiusPixels, greenDotPaint);
      canvas.drawText(minWeightLabel, minWeightLabelX, minWeightLabelY, labelPaint);
    }

    // Draw a dot and a label for the weight at the maximum timestamp, unless it is identical to the
    // minimum or maximum weight and shouldn't get a label.
    if (maxTimestampWeightLabel != null) {
      canvas.drawCircle(maxTimestampX, maxTimestampY, dotRadiusPixels, whiteDotPaint);
      float maxTimestampWeightLabelX = canvas.getWidth() - maxTimestampWeightLabelWidth;
      float maxTimestampWeightLabelY = project((float) maxTimestampWeight, (float) minWeight,
          (float) maxWeight, canvas.getHeight() - bottomMargin, topMargin)
          + 0.5f * labelHeight - fontMetrics.descent;
      canvas.drawText(maxTimestampWeightLabel, maxTimestampWeightLabelX, maxTimestampWeightLabelY,
          labelPaint);
    }
  }

  /**
   * Projects a value linearly from one range to another.
   */
  private static float project(float value, float minFrom, float maxFrom, float minTo,
      float maxTo) {
    return (value - minFrom) / (maxFrom - minFrom) * (maxTo - minTo) + minTo;
  }

  /**
   * Picks an abbreviated weight unit, depending on the {@link Locale}.
   */
  private String getLocalizedWeightUnit() {
    // First approximation: pounds for US and kilograms anywhere else.
    return Locale.US.equals(Locale.getDefault()) ? "lb" : "kg";
  }

  /**
   * Converts a weight in kilograms to pounds if necessary, depending on the {@link Locale}.
   */
  private double getLocalizedWeight(double weightKg) {
    // First approximation: pounds for US and kilograms anywhere else.
    return Locale.US.equals(Locale.getDefault()) ? KG_TO_LBS * weightKg : weightKg;
  }

  /**
   * Turns a Unix epoch timestamp in seconds into a month and day format depending on the 24 hour
   * setting (same logic and formats as the clock).
   */
  private String getLocalizedDate(long timestamp) {
    SimpleDateFormat dateFormat =
        DateFormat.is24HourFormat(getContext()) ? DATE_FORMAT_24H : DATE_FORMAT_12H;
    return dateFormat.format(new Date(TimeUnit.SECONDS.toMillis(timestamp)));
  }
}
