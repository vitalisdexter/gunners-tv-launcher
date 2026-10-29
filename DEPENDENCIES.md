# Dependencies

Build: Gradle wrapper 8.9 (Apache-2.0), Android Gradle Plugin 8.5.2 (Apache-2.0), Android SDK 34, Java 17. Wrapper distribution SHA256 is pinned in gradle-wrapper.properties. No SDK/JDK or cache is bundled.

Runtime: Android framework only; no third-party runtime library added.

Football News uses framework/JDK HTTPS and XML APIs for public YouTube Atom feeds and standard RSS discovery links, with bounded thumbnails and external YouTube playback. No YouTube SDK, RSS library, account SDK or API key is bundled. Remote services have their own terms and availability limits; see [News sources](docs/NEWS-SOURCES.md).

Optional development asset generation: Python `qrcode==8.2` (BSD-3-Clause) and Pillow for `tools/generate_support_qr.py`. These packages are not bundled, used by Gradle or included in the Android app. The generated QR is already checked in, so building the launcher does not require them.

Tests only: JUnit 4.13.2 (EPL-1.0) and Hamcrest (BSD); org.json 20240303 (public domain). Test libraries are downloaded by Gradle, not redistributed in the app or source archive. Dependency services retain their own terms.
