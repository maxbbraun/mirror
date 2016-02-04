package net.maxbraun.mirror;

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
   * The "Top Headlines" news feed from the Associated Press.
   */
  private static final String AP_TOP_HEADLINES_URL =
      "http://hosted2.ap.org/atom/APDEFAULT/3d281c11a96b4ad082fe88aa0db04305";

  /**
   * The time in milliseconds between API calls to update the news.
   */
  private static final long UPDATE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);

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

    // Get the latest headlines from the AP news feed.
    String response = Network.get(AP_TOP_HEADLINES_URL);
    if (response == null) {
      Log.w(TAG, "Empty response.");
      return null;
    }

    // Parse just the headlines from the XML.
    try {
      parser.setInput(new StringReader(response));
      parser.nextTag();
      parser.require(XmlPullParser.START_TAG, null, "feed");
      List<String> headlines = new ArrayList<>();
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        if (parser.getName().equals("entry")) {
          String title = readEntryTitle();
          if (title != null) {
            headlines.add(title);
          }
        } else {
          skipTags();
        }
      }
      return headlines;
    } catch (IOException | XmlPullParserException e) {
      Log.e(TAG, "Parsing news XML response failed: " + response, e);
      return null;
    }
  }

  /**
   * Reads the contents of a {@code <title>} tag within an {@code <entry} tag at the current parser
   * position.
   */
  private String readEntryTitle() throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, "entry");

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
