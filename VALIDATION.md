# Local verification

## 1.2.0 shared content and football News

On 29 October 2026, local 	estReleaseUnitTest lintRelease assembleRelease assembleDebug passed: **118 tests, zero failures/errors; lint 0 errors/72 warnings**. Both APKs assembled as dev.gunnerstv.launcher, version 1.2.0/code 4, minimum API 26/target 34. Debug QA uses the same private local evaluation signer; release assembly is unsigned. GitHub Actions verifies the public source separately.

The 21 added tests cover current-feed Home row visibility despite an empty startup cache, removal of stale cached rows, explicit hidden-row preservation, safe access/empty-table diagnostics, unfinished playback with a missing Watch Next type, YouTube IDs/URLs/channel identity, unsafe XML rejection, publish-date freshness rather than update time, deduplication/order, cache timing, and source gating to explicitly followed clubs. Starter creators never create football selections. No API key, account cookies, runtime library or permission was added.

A read-only smoke test using the actual compiled Java feed reader verified all six starter channel identities and recent posts (AFTV 15, Blue Lions TV 3, City Xtra 3, The United Stand 15, The Anfield Wrap 15, WeAreTottenhamTV 15) on that date. Counts change naturally. Handle-page resolution encountered a regional non-feed redirect in this environment; UC-ID/RSS fetching succeeded. The editor explains its UC-channel fallback; consent/account flows are not automated. Synthetic parser tests cover matching/mismatching canonical and RSS identities. No raw feeds, user history or videos were committed.

Lint counts: VectorRaster:1, DiscouragedApi:2, SetJavaScriptEnabled:1, DrawAllocation:11, UnusedResources:3, ViewConstructor:14, SetTextI18n:29, RtlHardcoded:11. The seven additional warnings comprise two programmatic-only view constructors and five English UI status-text findings. They are recorded, not suppressed. Existing categories/triage below still apply.

Source review checked background requests, response/XML/image limits, channel identity, fixed YouTube video links, cache fallback and callback invalidation on source changes/detachment. Match refresh no longer detaches an active News panel. No emulator or TV was used. The user's positive Content sources counts establish readable titles, but the precise physical failure was not reproduced locally: current-feed composition and explicit row re-enabling address confirmed cache/visibility paths. DPAD reachability, on-TV rendering, source editing, actual video launch/Back and provider-specific resume still need the user's reinstall test. Missing private provider history cannot be remedied through public creator RSS.

## 1.1.0 football setup, focus and media sessions

On 29 October 2026, local `testReleaseUnitTest lintRelease assembleRelease assembleDebug` passed: **97 tests, zero failures/errors; lint 0 errors/65 warnings**. Debug and unsigned release APKs assembled as `dev.gunnerstv.launcher`, version `1.1.0`/code `3`, minimum API 26/target 34. The debug signer remains the same private local QA identity; release assembly is unsigned. Hosted results are available in [GitHub Actions](https://github.com/vitalisdexter/gunners-tv-launcher/actions).

New regression cases cover viewport edges/halo/reorder positions, empty and persisted football selections, main-club changes/removal, bounded selection counts, player scope/team associations, football UID filtering, similarly named clubs, arbitrary player profiles, national-side classification, media metadata fallback, strict playback-state priority and capability-based commands. Live read-only public-directory smoke resolved Lamine Yamal (Barcelona/Spain) and Bukayo Saka (Arsenal/England), without fixed player mappings or bundled raw feeds.

Warning counts remain the original categories: `VectorRaster:1`, `DiscouragedApi:2`, `SetJavaScriptEnabled:1`, `DrawAllocation:11`, `UnusedResources:3`, `ViewConstructor:12`, `SetTextI18n:24`, `RtlHardcoded:11`. Compared with 1.0.1, the two additional warnings concern the programmatically created `FocusScrollView` constructor (not inflated from XML) and an English media-status text path. They are recorded rather than suppressed; existing warning triage below still applies.

No TV was contacted. Actual DPAD/keyboard interaction, focus visibility while scrolling/reordering, setup save/relaunch and live provider controls still require a reinstall/device test. The reported YouTube Music playback stop on Home is **not claimed fixed**: session reconnection/controls can help only when the provider leaves a usable session. The app does not force background playback or read notification contents. The installation export/photo were reviewed privately and are not bundled.

## 1.0.1 support link

Local verification on 28 October 2026 ran `testReleaseUnitTest lintRelease assembleRelease assembleDebug`: **68 tests passed, zero failures; lint 0 errors/63 warnings** (same warning IDs/counts as the preparation baseline). Both APKs assembled with package `dev.gunnerstv.launcher`, version `1.0.1`/code `2`. Debug signing verified with the same private local QA identity; the release is unsigned. These APKs are local evaluation artefacts, not a production download.

The support QR was independently decoded to the exact approved `https://buymeacoffee.com/vitalisdexter` URL, including resized 204/306/408-pixel images. The asset is bundled and displayed offline; browser navigation is user-initiated and catches missing/blocked handlers. Source review checked overlay focus/Back handling, the two support entry points, no new permissions/dependencies and asset hashes. Physical TV rendering, phone-camera scanning, browser return and no-browser fallback have not been tested on a device.

The first sandbox build could not access a local SDK build-tools JAR. Verification succeeded after using the authorized SDK access; this was a local environment failure. Current hosted results are linked in [GitHub Actions](https://github.com/vitalisdexter/gunners-tv-launcher/actions).

## 1.0.0 preparation baseline

Earlier documentation updates expanded product/setup docs and organised component legal files under `legal/`. Markdown links, branding exceptions and unchanged executable source were checked separately at that point; no TV test was claimed.

Fresh independent checkout of source commit `299c55c7d398d6f53c62bb2e981fbb6e8c7cc53b`. Java 17, Android SDK 34, wrapper 8.9, AGP 8.5.2. Commands: `testReleaseUnitTest lintRelease assembleRelease assembleDebug`. The local verification machine used an ordinary warm dependency cache and an installed SDK outside the checkout; source does not depend on their paths. Hosted verification is recorded separately below.

- **68 tests passed**, zero failures/errors.
- Lint: **0 errors**, **63 warnings**, 0 informational findings. No new blanket suppression. 
- Debug QA and unsigned release APKs assembled; package, manifest components and signing state inspected with Android build tools. 
- Source, Git index/history (118 unique blobs), filenames and assets checked for known private markers/credential patterns. Required licences are retained. Scans are bounded, not a guarantee against every possible secret format.
- PowerShell scripts parse; mocked device script verified explicit targeting without executing ADB.

## Hosted verification

On 28 October 2026, [GitHub Actions run 40774656944](https://github.com/vitalisdexter/gunners-tv-launcher/actions/runs/40774656944) passed for commit `7f1d75a9a1d76b751086a130a5ff067c16df50be`, using a fresh Ubuntu 24.04 runner, Java 17 and SDK 34. SDK component setup and `testReleaseUnitTest lintRelease assembleRelease assembleDebug` all succeeded. This verifies the public checkout/build route, not TV behaviour or production signing. The workflow does not publish APK downloads.

The first hosted runs stopped before compilation because the moving `ubuntu-latest` image lacked `sdkmanager`. The workflow now selects Ubuntu 24.04 and calls the installed SDK manager by its explicit path.

## Warning triage

Counts by lint ID: `{"DiscouragedApi": 2, "DrawAllocation": 11, "RtlHardcoded": 11, "SetJavaScriptEnabled": 1, "SetTextI18n": 23, "UnusedResources": 3, "VectorRaster": 1, "ViewConstructor": 11}`.

UI text/RTL warnings reflect the existing English-first TV interface; drawing allocations and custom view constructors need on-device profiling before claiming performance improvements. Dynamic resources such as time-zone lookup and wallpaper identifiers are retained despite unused-resource warnings. Reflection/device settings fallbacks are firmware-dependent. The disabled House Control branch retains its original local WebView renderer; JavaScript is unreachable while houseInstalled() is false, not blanket-suppressed. Shared horizontal paging remains active. Static match-dialog retention was removed and teardown wired to the owning activity; runtime rotation/process-death checks remain.

## Not tested here

No emulator/system image was installed. No TV was contacted, no app installed, no settings changed. Physical remote/focus, actual rendered screens, reboot/default HOME behaviour, media access, live match transitions, provider playback/resume, Google Home, audio selection and phone casting still require a separately approved device test. Synthetic documentation images are not test evidence. No production signing.
