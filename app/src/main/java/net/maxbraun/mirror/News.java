package net.maxbraun.mirror;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A helper class to regularly retrieve news headlines.
 */
public class News extends DataUpdater<List<String>> {
  private static final String TAG = News.class.getSimpleName();

  /**
   * The "Times Wire" RSS feed from the New York Times.
   */
  private static final String NYT_RSS_URL =
      "https://content.api.nytimes.com/svc/news/v3/all/recent.rss";

  /**
   * The time in milliseconds between API calls to update the news.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);

  /**
   * A parser for the news feed XML.
   */
  private final XmlPullParser parser;

  public News(UpdateListener<List<String>> updateListener) {
    super(updateListener, UPDATE_INTERVAL_MILLIS);

    parser = Xml.newPullParser();
    try {
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
    } catch (XmlPullParserException e) {
      Log.e(TAG, "Failed to initialize XML parser.", e);
    }
  }

  @Override
  protected List<String> getData() {
    // Get the latest headlines.
    String response = Network.get(NYT_RSS_URL);
    if (response == null) {
      Log.w(TAG, "Empty response.");
      return null;
    }

    // Parse just the headlines from the XML.
    try {
      parser.setInput(new StringReader(response));
      parser.nextTag();
      parser.require(XmlPullParser.START_TAG, null, "rss");
      List<String> headlines = new ArrayList<>();

      // Seek to the first channel tag.
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        if (parser.getName().equals("channel")) {
          break;
        } else {
          skipTags();
        }
      }

      // Find each item tag and read the title tag within.
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        if (parser.getName().equals("item")) {
          String title = readItemTitle();
          if (!TextUtils.isEmpty(title)) {
            Log.d(TAG, "Headline: " + title);
            headlines.add(title);
          }
        } else {
          skipTags();
        }
      }
      return headlines;
    } catch (IOException | XmlPullParserException e) {
      Log.e(TAG, "Parsing news XML response failed.", e);
      return null;
    }
  }

  /**
   * Reads the contents of a {@code <title>} tag within an {@code <item>} tag at the current parser
   * position.
   */
  private String readItemTitle() throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, "item");

    String title = null;
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }

      if (parser.getName().equals("title")) {
        title = readText("title");
      } else {
        skipTags();
      }
    }

    return title;
  }

  /**
   * Reads the contents of a tag by the specified name at the current parser position.
   */
  private String readText(String name) throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, name);
    String text = "";
    if (parser.next() == XmlPullParser.TEXT) {
      text = parser.getText();
      parser.nextTag();
    }
    parser.require(XmlPullParser.END_TAG, null, name);
    return text;
  }

  /**
   * Skips tags from the current parser position until the starting one is closed.
   */
  private void skipTags() throws IOException, XmlPullParserException {
    if (parser.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException("Not skipping from a start tag.");
    }

    int depth = 1;
    while (depth != 0) {
      switch (parser.next()) {
        case XmlPullParser.END_TAG:
          depth--;
          break;
        case XmlPullParser.START_TAG:
          depth++;
          break;
      }
    }
  }

  @Override
  protected String getTag() {
    return TAG;
  }
}
