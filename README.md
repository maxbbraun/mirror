# Smart Mirror

This is the source code for the prototype UI of my smart mirror project outlined in [this article](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba#.4exmyxt0w).

[!["Smart Mirror"](mirror.jpg)](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba#.4exmyxt0w)

Simply import the whole project into [Android Studio](http://developer.android.com/tools/studio/index.html), then build and run the apk.

While the time, date, and news show up without any additional changes, you need to first enable the respective APIs in order to see the weather, commute, and body measures. Edit [`keys.xml`](app/src/main/res/values/keys.xml) and enter the API key for the [Dark Sky API](https://darksky.net/dev/), the key for the [Google Maps Directions API](https://developers.google.com/maps/documentation/directions/), and multiple keys for the [Nokia Health API](https://developer.health.nokia.com/api). (Follow [step 1](https://developer.health.nokia.com/api#step1) through [step 4](https://developer.health.nokia.com/api#step4) and copy all keys from the last output.) The home and work addresses for the commute need to be entered in [`commute.xml`](app/src/main/res/values/commute.xml).

## License

Copyright 2016 Max Braun

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
