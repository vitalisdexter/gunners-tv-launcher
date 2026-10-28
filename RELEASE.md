# Release preparation

Current source version: **1.0.1 / versionCode 2**. Adds the owner's optional Buy Me a Coffee link in README and Settings/About, with an offline TV QR code and browser fallback. No new runtime library or permission. Earlier 1.0.0 preparation notes remain below for provenance.

Draft description: Six favourites and installed-app discovery, clock/greeting, utility widgets, weather/forecast, Now Playing, Wi-Fi/audio/Google Home shortcuts, and app-published content. Vertical content rows with visibility switches and restored focus; selected-club and player fixtures across competitions, live priority/Arsenal fallback, match centre with H2H, lineups and provider commentary.

Draft release: Gunner's TV Launcher 1.0.0 — independently buildable public preparation. Personal defaults removed; independent build and recovery documentation; required component licences retained. New package, original cannon-inspired icon/banner, neutral About/licences, validated weather configuration and bounded network/artwork handling.

Debug builds use only a local QA identity. Release builds are unsigned. Production signing needs a separate owner-approved key held outside Git and a documented secure signing process. No key, password or signed production binary is published here. Source-only CI never accesses signing secrets.

Public source: [vitalisdexter/gunners-tv-launcher](https://github.com/vitalisdexter/gunners-tv-launcher), branch `main`; `v1.0.0-preparation` records the original source-preparation milestone. Installation documentation may be newer than that tag. The reusable [installation prompt](INSTALL-PROMPT.md) builds the selected source revision for local evaluation.

No signed production APK or GitHub Release is currently published. Check the actual [Releases page](https://github.com/vitalisdexter/gunners-tv-launcher/releases) before selecting a prebuilt download. Publishing a signed production APK requires an approved signing process; source publication alone does not authorise a TV installation or default-Home change. Local source/artifact hashes and checks were supplied separately for review.
