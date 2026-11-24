# DriveHome

[![CI](https://github.com/prakash6018/drivehome/actions/workflows/ci.yml/badge.svg)](https://github.com/prakash6018/drivehome/actions/workflows/ci.yml)

A dashboard-style home screen for Android car head units, built with Kotlin and Jetpack Compose. It replaces the vendor launcher on a cheap Android head unit with one designed for glancing at while driving: big touch targets, a dark glass UI, and the information you need most closest to the driver.

## Features

**Dashboard**
- Glass widgets over an ambient backdrop: a blurred copy of the current album art, an accent mesh, film grain and a vignette, so the screen takes on the colours of the song that's playing.
- Columns ordered by how often you look at them. Speed and trip sit nearest the driver and the map sits furthest away. The dock moves to the driver's side (left-hand or right-hand drive).
- A "showroom" photo of your own car with an accent light pool and a reflection. At night the headlamp glows.

**Speedometer and trip computer**
- Cluster-style gauge with a redline zone and a needle self-test sweep on boot, like a real instrument cluster when the ignition comes on.
- GPS speed from the head unit's own antenna, with a parked threshold so GPS jitter doesn't add phantom kilometres.
- A foreground location service keeps the trip counting while another app, such as Maps, is in front.

**Media**
- Now-playing card for any app with a media session, with controls and artwork.
- Vendor radio, Bluetooth and USB players often have no media session. DriveHome reads their notifications instead and shows the source app's icon in place of artwork.
- Uses Usage Access (optional) to work out which source was last opened, for example the radio launched from the steering-wheel MODE button.

**Map, weather and apps**
- Navigation card with the current area name and one-tap Home and Work routes. It hands off to Google Maps, or to any app that handles `geo:` links.
- Weather from Open-Meteo (free, no API key).
- Four configurable app tiles. If you don't pick an app, DriveHome guesses one from known package names, then from label keywords such as "BT Phone" or "FM Radio".

**Built for a unit with no ADB**
- Crashes are written to a file and shown on a Diagnostics screen (long-press the trip computer).
- A diagnostics report can be exported to `Download/` and copied to a USB stick.
- There's always an escape hatch back to the vendor's home screen.

## Tech

Kotlin · Jetpack Compose · Material 3 · Android location and foreground services · MediaSession and NotificationListenerService · UsageStatsManager · Open-Meteo REST API

## Build

Open the project in Android Studio, or run:

```bash
./gradlew assembleDebug
```

Install the APK on the head unit and choose DriveHome as the default Home app. To show now-playing info for every source, grant notification access. Usage access is optional.

## Fonts

Rajdhani is used under the SIL Open Font License 1.1.
