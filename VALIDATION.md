# Local verification

Documentation update: product/setup docs were expanded and component legal files organised under `legal/`. Markdown links, branding exceptions and unchanged runtime/build source were checked separately. The build results below apply to the executable source, which this documentation update does not change; no additional TV test is claimed.

Fresh independent checkout of source commit `299c55c7d398d6f53c62bb2e981fbb6e8c7cc53b`. Java 17, Android SDK 34, wrapper 8.9, AGP 8.5.2. Commands: `testReleaseUnitTest lintRelease assembleRelease assembleDebug`. The verification machine used an ordinary warm dependency cache and an installed SDK outside the checkout; source does not depend on their paths. A new-machine online dependency download and hosted CI are not claimed.

- **68 tests passed**, zero failures/errors.
- Lint: **0 errors**, **63 warnings**, 0 informational findings. No new blanket suppression. 
- Debug QA and unsigned release APKs assembled; package, manifest components and signing state inspected with Android build tools. 
- Source, Git index/history (118 unique blobs), filenames and assets checked for known private markers/credential patterns. Required licences are retained. Scans are bounded, not a guarantee against every possible secret format.
- PowerShell scripts parse; mocked device script verified explicit targeting without executing ADB.

## Warning triage

Counts by lint ID: `{"DiscouragedApi": 2, "DrawAllocation": 11, "RtlHardcoded": 11, "SetJavaScriptEnabled": 1, "SetTextI18n": 23, "UnusedResources": 3, "VectorRaster": 1, "ViewConstructor": 11}`.

UI text/RTL warnings reflect the existing English-first TV interface; drawing allocations and custom view constructors need on-device profiling before claiming performance improvements. Dynamic resources such as time-zone lookup and wallpaper identifiers are retained despite unused-resource warnings. Reflection/device settings fallbacks are firmware-dependent. The disabled House Control branch retains its original local WebView renderer; JavaScript is unreachable while houseInstalled() is false, not blanket-suppressed. Shared horizontal paging remains active. Static match-dialog retention was removed and teardown wired to the owning activity; runtime rotation/process-death checks remain.

## Not tested here

No emulator/system image was installed. No TV was contacted, no app installed, no settings changed. Physical remote/focus, actual rendered screens, reboot/default HOME behaviour, media access, live match transitions, provider playback/resume, Google Home, audio selection and phone casting still require a separately approved device test. Synthetic documentation images are not test evidence. No production signing or hosted CI run.
