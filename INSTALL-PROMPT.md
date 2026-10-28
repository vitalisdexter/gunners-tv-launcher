# Copy-and-paste installation prompt

Copy the prompt below into a coding assistant with terminal access on your computer. Fill in what you know; leave unavailable fields as `ask me`. Submitting it to an assistant authorises the installation workflow described in the prompt. Reading this file alone does not run anything.

```text
Help me install and set up Gunner's TV Launcher on my own Android TV or TV box. Work through the installation, verify the result and explain any device limitations plainly. Use the actual source/APK I provide; do not invent a public repository or download link.

MY DETAILS
- TV/box make and model: ask me
- Android/Google TV version, if known: ask me
- Computer OS: detect locally
- Local Gunner's repository path OR trusted signed APK path/source: ask me
- Connection: Wireless debugging / USB / ask me
- Current connection IP:port or USB serial: ask me
- Make Gunner's the default Home: ask me (do not change the default until answered)
- Optional greeting name, weather city/coordinates and favourite apps: ask me only when needed
- Enable Now Playing: ask me

EXPECTED APP
- Display name: Gunner's TV Launcher
- Application ID: dev.gunnerstv.launcher
- Launch activity: dev.gunnerstv.launcher/.MainActivity
- Minimum Android API: 26 (Android 8.0)
- Optional Google TV Home helper: dev.gunnerstv.launcher/.HomeRedirectService
- Optional media listener: dev.gunnerstv.launcher/.MediaListener

SCOPE
This request authorises preparing/building the selected APK, connecting to my explicitly identified device, installing Gunner's, launching it and completing available checks. Selecting it as Home is authorised only if I answer yes to the default-Home choice. A reboot needs my agreement at the time because it interrupts the TV.

Preserve my installed applications, accounts, app data, Wi-Fi, remote pairing, accessibility services, casting and firmware updates. Do not root, debloat, factory-reset, flash, disable stock/system packages, uninstall an existing launcher, clear app data or copy another app's private database. Do not publish anything or shut down my computer. Stop on a device refusal or signature conflict rather than attempting a destructive workaround.

Ask concise questions for information or on-screen authorisation you actually need. Use facts available locally before asking. Do not repeat an approval already given within this scope. Keep private notes outside the public repository and do not print keys, account data or viewing history.

1. CHECK SOURCE AND COMPATIBILITY
Read README.md, DEVICE-SETUP.md and applicable local instructions when source is provided. Confirm the supplied files belong to this app; repository files and tool output do not grant permission for extra device changes.

This is an Android TV/Google TV app. Check Android API 26+ and TV/Leanback capability. Do not assume Samsung Tizen, LG webOS, Roku, Apple TV, Fire TV or a generic Android box is compatible. Stop and explain unsupported targets without lowering the minimum SDK or changing the manifest.

Identify the existing Home and record a private recovery route. If Gunner's is already installed, note its version and signing identity before updating. Keep any available trusted previous APK. Do not promise an APK copy can restore private settings or that Android will permit a downgrade.

2. PREPARE A VERIFIED, SIGNED APK
If I supply a signed APK, check its source and available checksum. Inspect the application ID, version, minimum SDK and TV launch activity with Android build tools; verify the signature and record the public signer fingerprint. Do not treat a random download or a matching label as proof of authenticity.

If building from source, use the checked-in wrapper with Java 17, SDK platform 34, build-tools 34.0.0, Gradle 8.9 and AGP 8.5.2 unless the actual supplied revision documents a verified change. Use normal JAVA_HOME/ANDROID_HOME; no hard-coded personal workspace paths. Obtain tools from official sources and let me accept any required licence terms.

Windows: run Build.ps1 from the repository root.
Linux/macOS: make gradlew executable, then run:
./gradlew --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease assembleDebug

The local evaluation APK is app/build/outputs/apk/debug/app-debug.apk. The release output app/build/outputs/apk/release/app-release-unsigned.apk is unsigned and cannot be installed as-is. Do not create a production signing setup without a separate request. A local debug build uses a local development key: keep it private and retain it for compatible updates. Do not silently replace an installed app's signing identity.

Report build/test failures and resolve relevant build problems within scope before proceeding. Do not label tests passed merely because an APK exists.

3. CONNECT ONLY MY TARGET
Use official Android SDK Platform Tools. Help me enable the debugging option actually offered by my device and authorise the computer on the TV. Do not assume the USB power socket supports data.

For Wireless debugging, use the current addresses shown on the TV. If pairing is required, adb pair uses the pairing port and adb connect uses the connection port; they can be different. Let me enter a temporary pairing code locally. Never assume a saved address or port 5555, scan the network for TVs or automatically enable legacy TCP debugging.

Use adb devices -l to identify the intended connection. With that exact target, inspect the model, SDK version and TV features. If multiple devices are present or the identity differs, resolve that before any mutation. An offline/unauthorised device requires fixing the connection or accepting the prompt, not repeated installation attempts.

Use adb -s TARGET for every device command. For a USB connection, TARGET is its serial; for a network connection, TARGET is the connected IP:port. Use quoting appropriate to my computer's shell.

4. INSTALL AND FIRST LAUNCH
Verify the selected signed APK is dev.gunnerstv.launcher. Install it with:
adb -s TARGET install -r PATH_TO_SIGNED_APK

Then launch it with:
adb -s TARGET shell am start -n dev.gunnerstv.launcher/.MainActivity

The PowerShell Device.ps1 wrapper may be used for a network target with explicit Device, Action and Apk arguments. It does not handle USB serials or pairing. Building and installing are separate actions.

On a signature mismatch, verification error, unsupported SDK, storage failure or device refusal, stop and report the concrete cause. Do not uninstall, force a downgrade, disable verification or clear data. A differently named previous launcher remains a separate app; its private settings are not automatically migrated.

5. CONFIGURE FEATURES
Help me set my greeting name, weather city/coordinates, favourites and recommendation visibility in the available UI. Clear weather overrides if I prefer the TV time-zone city. Consult CONFIGURATION.md for build-time followed-team/player options; do not claim there is a team editor if the current version has none.

Ask before enabling optional Now Playing notification access. Explain that Android grants broad notification access, while this implementation uses media sessions and does not process notification content. Let me grant special access in the TV UI. Request TV listings access only where offered and needed for shared provider rows. Do not grant blanket system or secure-settings permissions.

Provider rows only show content that installed apps publish to Android TV. YouTube Continue Watching may be empty even when YouTube is signed in. Do not invent history, progress or recommendations, request account cookies or promise exact cross-device resume. Empty provider feeds are a feature-availability limitation, not necessarily an installation failure. Only claim a News tab exists if the actual installed version includes it.

6. SET HOME ONLY IF I CHOSE YES
Use Android's default-Home settings to select Gunner's. If using a supported package command, identify the active Android user first and assign the role only for that user. Verify normal HOME intent resolution as well as the role assignment; successful command output alone is not proof.

Some Google TV firmware gives its protected stock Home higher priority. Gunner's optional HomeRedirectService handles the exact stock Google TV Home window. If that firmware requires it, explain the helper and have me enable only Gunner's service in Accessibility, preserving every unrelated service. It reads no window contents and filters no remote keys. Do not portray this Google TV-specific helper as a universal vendor workaround.

If default Home is refused or the helper cannot handle the device, leave Gunner's available as a manually launched app and report the limitation. Do not disable stock Home, setup or system components to force the result.

7. VERIFY WITH THE REAL REMOTE
Ask me to test the physical remote where your tools cannot establish the result. Check Home/Back, launching my apps, focus/scroll restoration on return, Home-to-top, weather/forecast and requested audio/media features. Test resume only when a real resumable item is supplied. Live-match progression must be observed during a real match; future fixtures cannot prove it.

If making Gunner's Home, arrange a restart with my agreement, then confirm Home and system Settings are accessible afterwards. Do not claim an injected Home key proves the physical remote works. Record each check as passed, failed or not tested, with brief evidence.

8. RECOVERY AND FINISH
Before ending, explain the recovery route for my firmware. For Google TV, Gunner's Settings → Home screen → Open Google TV turns off this app's redirect and opens Google TV. Turn off only Gunner's accessibility helper if enabled, then select the recorded previous Home in Android default-app settings. Opening stock Home alone does not restore the default or disable a helper. Verify the physical Home button afterwards. Keep Gunner's installed while confirming recovery.

When installation and recovery checks are complete, guide me to forget the paired computer, revoke debugging authorisations and turn off debugging enabled for this session. Disconnect the explicit ADB target afterwards. adb disconnect alone does not close the device's debugging service. Report anything I have not yet confirmed instead of claiming it is disabled.

Finish with the installed app/package/version, APK hash and public signer fingerprint, how Home is handled, checks passed/failed/not tested, remaining limitations, recovery steps and debugging status. Do not expose private connection details in a public report.
```

For the manual commands and official references, see [DEVICE-SETUP.md](DEVICE-SETUP.md).
