# Sensor Lab

An Android utility app built with Kotlin and Jetpack Compose for discovering, inspecting, testing, and streaming live data from all hardware, software, and vendor sensors on an Android device.

## Features

- **Full Sensor Discovery**: Lists all sensors reported by `SensorManager` (`Sensor.TYPE_ALL`), categorized into **Motion**, **Position**, **Environment**, and **Other**, and tagged by kind (**Hardware**, **Software**, **Vendor**, **Custom**).
- **Search & Category Filtering**: Quickly filter sensors by name, vendor, or category.
- **Live Sensor Streaming & Waveform Visualization**: Stream real-time sensor values, sample counts, accuracy, and live sampling frequency (Hz) with a Canvas-rendered multi-axis waveform chart.
- **Configurable Sampling Rates**: Switch between `Normal`, `UI`, `Game`, and `Fast` (`SENSOR_DELAY_FASTEST`) sampling rates.
- **Sensor Diagnostics & Batch Check**: Test individual sensors or run checks across sensors, with clear status reporting (`Reading`, `No change yet`, `One-shot armed`, `Needs permission`, `Permission denied by default`).
- **One-Shot Trigger Support**: Arms and listens for one-shot trigger sensors (`TriggerEventListener`).
- **Detailed Hardware Specs**: Displays vendor, version, power draw (mA), max range, resolution, min/max delay, reporting mode, wake-up capability, and common use cases.
- **Runtime Permission Handling**: Prompts for `ACTIVITY_RECOGNITION` (step counter/detector on Android 10+) and `BODY_SENSORS` (heart rate/beat) when testing protected sensors.

## Tech Stack

- **Language**: Kotlin 2.3.20 (JVM Toolchain 17)
- **UI**: Jetpack Compose + Material 3 (`compose-bom:2026.03.01`)
- **Navigation**: AndroidX Navigation 3 (`navigation3-runtime`, `navigation3-ui`)
- **Lifecycle**: AndroidX Lifecycle (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`)
- **Ads**: Google Play Services Ads (`play-services-ads`)
- **Build System**: Gradle 9 / AGP 9.0.1 (`compileSdk 36`, `targetSdk 36`, `minSdk 24`)

## Project Structure

```text
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/example/app1/
│       │   │   ├── MainActivity.kt
│       │   │   ├── Navigation.kt
│       │   │   ├── NavigationKeys.kt
│       │   │   ├── sensors/
│       │   │   │   ├── SensorModels.kt
│       │   │   │   └── SensorRepository.kt
│       │   │   └── ui/
│       │   │       ├── main/
│       │   │       │   ├── MainScreen.kt
│       │   │       │   └── MainScreenViewModel.kt
│       │   │       └── theme/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── release/v1.0/          # Prebuilt v1.0 APK, AAB, and Play Store icon
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

## Building & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/vijumenezes/android_sensor_lab.git
   cd android_sensor_lab
   ```
2. Open in Android Studio or build from the command line:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

> **Note**: `AndroidManifest.xml` currently uses the official Google AdMob test App ID (`ca-app-pub-3940256099942544~3347511713`). Replace it with your production AdMob App ID before publishing to Google Play.

## Release Artifacts

Prebuilt v1.0 artifacts are included in `release/v1.0/`:
- `SensorLab-v1.0-installable.apk`
- `SensorLab-v1.0-playstore.aab`
- `SensorLab-v1.0-release-unsigned.apk`
- `SensorLab-playstore-icon-512.png`
