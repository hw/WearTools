# ToolsFTW

ToolsFTW is a standalone WearOS utility app with a compass and a spirit level. It is designed
for quick use directly on a watch without a companion phone app.

## Features

### Compass

- Magnetic and true-north headings
- Red true-north pointer and white magnetic-north pointer
- Rotating compass dial with cardinal and intercardinal labels
- Current time display
- Latitude and longitude display when location is available
- Altitude above sea level (ASL) when the platform provides mean-sea-level altitude
- Tap the ASL value to switch between meters and feet
- Tap elsewhere on the compass to switch between true north and magnetic north

True north, coordinates, and ASL depend on location permission and available location data.
ASL is displayed only when Android reports mean-sea-level altitude; ellipsoid altitude is not
presented as ASL.

### Spirit Level

- Two-axis bullseye level
- Tap to set the current angle as zero
- Long press to reset the zero calibration

### Help

- On-watch usage summary
- Build date and time in Singapore Time (SGT)

Swipe horizontally to move between compass, spirit level, and help.

## Persisted Settings

ToolsFTW stores the following preferences on the watch:

- Last-used tool
- True-north or magnetic-north mode
- ASL unit: meters or feet
- Spirit-level zero calibration

## Requirements

- WearOS device or emulator
- Android Studio with a compatible Android SDK
- JDK 11 or newer

The app targets Android SDK 36 and supports Android API 30 and newer.

## Permissions

The app requests coarse and fine location permission for:

- True-north declination
- Latitude and longitude
- Mean-sea-level altitude, when available

The compass and accelerometer sensors are optional. The app reports when a required sensor is
unavailable.

## Build

From the project root:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

To install the debug build on a connected watch:

```bash
./gradlew :app:installDebug
```

## Release Signing

Release builds are signed when all signing credentials are available. Provide them through a
git-ignored `keystore.properties` file at the project root:

```properties
storeFile=release-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

The `storeFile` path is resolved relative to the project root. Alternatively, set these environment
variables:

```bash
RELEASE_STORE_FILE=/path/to/release-keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

If credentials are absent, `assembleRelease` still builds an unsigned release APK.

## Package

```text
app.tanh.toolsftw
```
