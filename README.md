# Gunner's TV Launcher

Six favourites and installed-app discovery, clock/greeting, utility widgets, weather/forecast, Now Playing, Wi-Fi/audio/Google Home shortcuts, and app-published content. Vertical content rows with visibility switches and restored focus; selected-club and player fixtures across competitions, live priority/Arsenal fallback, match centre with H2H, lineups and provider commentary.

Version: **1.0.0 / 1**. Android package: `dev.gunnerstv.launcher`. Source provenance and required attributions: [PROVENANCE.md](PROVENANCE.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [ASSETS.md](ASSETS.md).

## Build independently

Install Java 17 and Android SDK platform 34/build-tools 34.0.0. Set `JAVA_HOME` and `ANDROID_HOME`. The checked-in Gradle 8.9 wrapper uses AGP 8.5.2. No sibling checkout, personal SDK path or signing key is required. First build downloads public dependencies; offline builds require a previously populated ordinary Gradle cache.

Windows: `./Build.ps1`. Linux/macOS:

```sh
chmod +x gradlew
./gradlew --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease assembleDebug
```

Debug QA APK: `app/build/outputs/apk/debug/app-debug.apk`, signed with the local Android debug identity. This is not a production signing key. Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`. It cannot be installed until signed separately. No signing credentials are included. See [RELEASE.md](RELEASE.md).



## Configuration and safe setup

See [CONFIGURATION.md](CONFIGURATION.md) for name, weather, favourites and following. [DEVICE-SETUP.md](DEVICE-SETUP.md) explains manual installation and recovery. Builds do not connect to a TV. Device actions require an explicit address and deliberate invocation. No application is uninstalled or disabled by the supplied script.

This application has a new package identity and is a fresh setup, not an in-place upgrade. Existing launcher settings/databases are not migrated or removed.

## Limits

Content comes only from Android TV records that installed apps publish. Netflix/YouTube/Prime/Apple TV may expose recommendations without resumable titles, or nothing. Exact resume is controlled by each provider. Music rows do not access Spotify history or promise Premium features. Football endpoints, where used, are undocumented public ESPN endpoints with incomplete/delayed coverage and no service guarantee. Future player participation is tentative until a lineup/appearance is reported. Weather uses Open-Meteo; its service terms apply independently of this code licence.

No private app databases, account passwords, playlist credentials or analytics are collected. See [PRIVACY.md](PRIVACY.md). Runtime club/provider logos are their owners’ marks, not a claim of endorsement. This project is not affiliated with Arsenal, any league, provider or TV manufacturer.

Local verification and remaining physical checks are recorded in [VALIDATION.md](VALIDATION.md). A successful build is not proof of correct behaviour on every TV. Hosted CI has not run until this repository is published with approval.

## Illustrative preview

![Synthetic layout illustration, not a device screenshot](docs/demo-preview.png)

The diagram communicates the layout family only; labels, weather and scores are synthetic. It is not runtime validation.
