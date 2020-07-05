# Smart Mirror

This is the source code for the prototype UI of my smart mirror projects outlined in
[this article](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba)
and its [follow-up](https://medium.com/@maxbraun/smarter-mirrors-and-how-theyre-made-327997b9eff7).

[![Smart Mirror 1](mirror-1.jpg)](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba)

[![Smart Mirror 2](mirror-2.jpg)](https://medium.com/@maxbraun/smarter-mirrors-and-how-theyre-made-327997b9eff7)

To get started, import the whole project into
[Android Studio](http://developer.android.com/tools/studio/index.html), then build and run the apk.
The first prototype uses
[`HomeActivity.java`](app/src/main/java/net/maxbraun/mirror/HomeActivity.java) and the second one
uses [`CompactHomeActivity.java`](app/src/main/java/net/maxbraun/mirror/CompactHomeActivity.java).

While the time, date, and news show up without any additional changes, you need to first enable the
respective APIs in order to see the weather, commute, and body measures. Edit
[`keys.xml`](app/src/main/res/values/keys.xml) and enter the key for the
[AccuWeather API](https://developer.accuweather.com), the key for the
[Google Maps Directions API](https://developers.google.com/maps/documentation/directions/start), and
multiple keys for the [Nokia Health API](https://developer.health.nokia.com/oauth2/).

The home and work addresses and the travel mode for the commute are pulled from a
[Firebase Realtime Database](https://firebase.google.com/docs/database/) and can be edited in the
[Firebase Console](https://console.firebase.google.com/) after
[adding Firebase to your Android Studio project](https://firebase.google.com/docs/android/setup).
The client expects a `commute_settings` path with string children `home`, `work`, and `travel_mode`.
The composition of the [compact UI](app/src/main/java/net/maxbraun/mirror/CompactHomeActivity.java)
is also configured via the Firebase Database: The `compact_ui_settings` path has boolean children
for `body`, `commute`, `time`, and `weather` that determine which ones are shown. See
[`database-example.json`](database-example.json) for a sample layout.
