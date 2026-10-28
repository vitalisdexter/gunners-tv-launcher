# Gunner's TV Launcher

A football-focused home screen for Android TV and Google TV: your apps, fixtures, weather and music in one place.

Version: **1.0.0 / 1**. Android package: `dev.gunnerstv.launcher`. Minimum Android API: **26 (Android 8.0)**.

## Features

- Six favourite apps, installed-app discovery and remote-friendly navigation.
- Clock, time-of-day greeting, configurable weather and animated forecast.
- Utility widgets above the apps; recommendations and football fixtures below.
- Separate recommendations and Continue Watching rows for supported providers when they share content with Android TV. Individual rows can be turned off.
- Now Playing and shortcuts for Wi-Fi, audio output, Google Home and system settings, where the device supports them.
- All-competition fixtures for followed clubs, plus supported player club/international tracking.
- Live-match priority, upcoming Arsenal fallback, and a match centre with overview, head-to-head results, lineups and provider commentary.
- Saved navigation position and Home-to-top behaviour.

## Install

Follow [DEVICE-SETUP.md](DEVICE-SETUP.md), or copy [INSTALL-PROMPT.md](INSTALL-PROMPT.md) into a coding assistant with local terminal access. The prompt guides device identification, APK verification, installation, setup, recovery and checks.

Supported targets are Android TV/Google TV devices running Android 8.0 or newer. Other Android boxes need their TV capabilities checked individually. This APK does not install directly on Samsung Tizen, LG webOS, Roku or Apple TV; those televisions can use a compatible external Android TV box. Fire TV and manufacturer-specific Home restrictions need separate compatibility checks.

Use a signed APK from a source you trust or build a debug APK locally. No public repository/download URL is configured in this source tree. The release build is unsigned and requires separate signing before installation.

## Build independently

Install Java 17 and Android SDK platform 34/build-tools 34.0.0. Set `JAVA_HOME` and `ANDROID_HOME`. The checked-in Gradle 8.9 wrapper uses AGP 8.5.2. No sibling checkout, personal SDK path or signing key is required. First build downloads public dependencies; offline builds require a previously populated ordinary Gradle cache.

Windows: `./Build.ps1`. Linux/macOS:

```sh
chmod +x gradlew
./gradlew --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease assembleDebug
```

Debug QA APK: `app/build/outputs/apk/debug/app-debug.apk`, signed with the local Android debug identity. This is not a production signing key. Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`. It cannot be installed until signed separately. No signing credentials are included. See [RELEASE.md](RELEASE.md).

## Configuration and safe setup

See [CONFIGURATION.md](CONFIGURATION.md) for name, weather, favourites and following. Builds do not connect to a TV. Device actions require an explicit target and deliberate invocation. Keep the same signing identity for future updates of an installed package.

This application has a new package identity and is a fresh setup, not an in-place upgrade. Existing launcher settings/databases are not migrated or removed.

## Limits

Content comes only from Android TV records that installed apps publish. Netflix/YouTube/Prime/Apple TV may expose recommendations without resumable titles, or nothing. YouTube Continue Watching is supported by the launcher but depends on YouTube publishing suitable records; the launcher cannot retrieve YouTube watch history. Exact resume is controlled by each provider. Music rows do not access Spotify history or promise Premium features. Football endpoints are undocumented public ESPN endpoints with incomplete/delayed coverage and no service guarantee. Future player participation is tentative until a lineup/appearance is reported. Weather uses Open-Meteo; its service terms apply independently of this code licence.

The proposed YouTube football News tab is not part of this version. Planned features are not represented as installed.

No private app databases, account passwords, playlist credentials or analytics are collected. See [PRIVACY.md](PRIVACY.md). Runtime club/provider logos are their owners’ marks, not a claim of endorsement. This project is not affiliated with Arsenal, any league, provider or TV manufacturer.

Local verification and remaining physical checks are recorded in [VALIDATION.md](VALIDATION.md). A successful build is not proof of correct behaviour on every TV. Hosted CI has not run until this repository is published with approval.

## Licence and contributions

Gunner's is independently branded and includes open-source components. Required attribution and component records are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [legal/SOURCE-PROVENANCE.md](legal/SOURCE-PROVENANCE.md). Artwork records are in [ASSETS.md](ASSETS.md). See [CONTRIBUTING.md](CONTRIBUTING.md) for changes and [CHANGELOG.md](CHANGELOG.md) for release notes.

## Illustrative preview

![Synthetic layout illustration, not a device screenshot](docs/demo-preview.png)

The diagram communicates the layout family only; labels, weather and scores are synthetic. It is not runtime validation.
