# Release preparation

Current source version: **1.3.0 / versionCode 5**. Adds Hold OK → Move app with directional rearranging, a highlighted/scrolling draft, OK to save and Back/Home to cancel. Works on Home's six visible apps and the full Apps grid. Existing quick moves and prior content/football functionality remain. No new runtime library or permission.

Draft 1.3.0 release notes: Arrange apps with the remote's direction buttons and confirm once with OK. Back cancels without saving; selection returns to the chosen app. Home supports horizontal rearranging; Apps supports vertical swaps too. The original order is untouched until confirmation. Build with the existing installation's signer for a compatible update. Physical remote/scroll/persistence checks remain for the user's reinstall.

Draft description: Six favourites and installed-app discovery, clock/greeting, utility widgets, weather/forecast, Now Playing, Wi-Fi/audio/Google Home shortcuts, and app-published content. Vertical content rows with visibility switches and restored focus; on-TV club/player/main-club setup, all-competition fixtures, live priority/chosen-main-club fallback, match centre with H2H, lineups, provider commentary and recent creator videos.

Draft 1.2.0 release notes: Refresh/readable-title visibility fixes, reachable shared-content setup, clearer hidden/access/empty feed status, and club-filtered YouTube football News. Starter creators apply only to explicitly followed clubs and can be edited/removed. News is public recent uploads, not personalized history. Use the same existing signer for an update; a mismatch is not a reason to uninstall or clear data. Physical TV navigation, exact resume and video launch/return remain to be verified after reinstall.

Draft release: Gunner's TV Launcher 1.0.0 — independently buildable public preparation. Personal defaults removed; independent build and recovery documentation; required component licences retained. New package, original cannon-inspired icon/banner, neutral About/licences, validated weather configuration and bounded network/artwork handling.

Debug builds use only a local QA identity. Release builds are unsigned. Production signing needs a separate owner-approved key held outside Git and a documented secure signing process. No key, password or signed production binary is published here. Source-only CI never accesses signing secrets.

Public source: [vitalisdexter/gunners-tv-launcher](https://github.com/vitalisdexter/gunners-tv-launcher), branch `main`; `v1.0.0-preparation` records the original source-preparation milestone. Installation documentation may be newer than that tag. The reusable [installation prompt](INSTALL-PROMPT.md) builds the selected source revision for local evaluation.

No signed production APK or GitHub Release is currently published. Check the actual [Releases page](https://github.com/vitalisdexter/gunners-tv-launcher/releases) before selecting a prebuilt download. Publishing a signed production APK requires an approved signing process; source publication alone does not authorise a TV installation or default-Home change. Local source/artifact hashes and checks were supplied separately for review.
