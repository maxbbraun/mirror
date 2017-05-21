# Smart Mirror

This is the source code for the prototype UI of my smart mirror project outlined in [this article](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba#.4exmyxt0w).

[!["Smart Mirror UI"](https://lh3.googleusercontent.com/LehdfgbhTCF7ni0TiaKeXz9xaIeDueIvGR0od-uOstaSduzvPHoC9ErtXCsqpBzsR4zO6C_jTFfSdO3tnh0USri_hV0wKs_JFEeeSNVxNFcS80vrizmM7-5nD1346c4zOl-9itPBZ0O4dO3TGO81RE-TQrUJO2uI-0bhUu5F18G4dgCEqPcb4_7cX5aFqzUuB0pdEvdrVvaaBObSVOk6XD5bme_uWh78yEz5Grd1KJWNkfI5q0UfDxr6m2-M8z6ak5qELuoqBTSU3FW-rrJT-gdDrMoea8ildlFf1AF5wMeuNGrZGIHMWuGqImwMFsnBebEg_wiAilAdPwTFYw_zh30e9YrdSgF-jAUzqGjAXQv-N0DHTA8RfMqkbs58z2xmol6-00suAgTJ6gIKZm8AQogtCpy80qLodimW2fvtrJ_5q1tSu2kJt3dcykAdLLPASzLE9ZNk0SPekjTgXaamKaXsXnfnN2ZBUmM3ZIarL-l-wk8WTYpO-Llwt571cMCUmyb_nb7cCqm8wDPVxYQzKrCLC2FuVePPoExcaYA_o9JH9IRMTabCvmKejLFxncaLYlJ5A5Xf3kaN3GOyaJ6A72_7oof43cSk=s700-no)](https://medium.com/@maxbraun/my-bathroom-mirror-is-smarter-than-yours-94b21c6671ba#.4exmyxt0w)

Simply import the whole project into [Android Studio](http://developer.android.com/tools/studio/index.html), then build and run the apk.

While the time, date, and news show up without any additional changes, you need to first enable the respective APIs in order to see the weather, commute, and body measures. Edit [`keys.xml`](app/src/main/res/values/keys.xml) and enter the API key for the [Dark Sky API](https://darksky.net/dev/) and multiple keys for the [Withings API](http://oauth.withings.com/api). (Follow [step 1](http://oauth.withings.com/api#step1) through [step 4](http://oauth.withings.com/api#step2) and copy all keys from the last output.) The home and work addresses for the commute need to be entered in [`commute.xml`](app/src/main/res/values/commute.xml).

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
