# WakeUp Warrior

Offline-first native Android alarm and wake-up challenge app.

## Planned capabilities
- Alarm scheduling with exact-alarm permission handling and reboot rescheduling
- User-selectable system/custom ringtones
- Offline quiz challenge
- Camera-based dance challenge with on-device pose analysis
- Lucky Wheel with supported household-object recognition
- Two-minute brushing timer with honest user confirmation
- Streaks, local statistics, settings and emergency override

## Build
Open this project in Android Studio (JDK 17, Android SDK 35) and run the `app` configuration. GitHub Actions builds a debug APK on pushes and pull requests.

Camera challenges require visible camera permission and foreground activity. Brushing duration is timed; the app does not claim to medically or visually prove brushing quality.
