# Install Gunner's TV Launcher

This guide installs `dev.gunnerstv.launcher`. For assistant-led setup, use [INSTALL-PROMPT.md](INSTALL-PROMPT.md). Choose the target device and APK deliberately; the build scripts never install automatically.

## 0. Get the source from GitHub

Public source: [vitalisdexter/gunners-tv-launcher](https://github.com/vitalisdexter/gunners-tv-launcher). From a new parent folder on Windows, macOS or Linux:

```text
git clone https://github.com/vitalisdexter/gunners-tv-launcher.git gunners-tv-launcher
cd gunners-tv-launcher
git remote get-url origin
git rev-parse HEAD
```

No GitHub login/token is needed to clone the public repository. Record the commit and keep any later personal configuration changes private. Reuse an existing checkout only after inspecting its origin and local changes; do not overwrite them. If Git is unavailable, install it from its official source or obtain the source ZIP from this exact repository. The assistant prompt handles locating or installing the required build tools on each computer OS.

## 1. Check the target

You need an Android TV or Google TV device with Android 8.0/API 26 or newer, a working remote, and access to system Settings. Manufacturer-specific Android boxes need their TV features checked. Tizen, webOS, Roku and Apple TV cannot run this APK directly. Fire TV support and Home replacement are not certified by this project.

Leave the existing launcher available. Record the current default Home in a private local note. If Gunner's is already installed, record its version and signing certificate before updating. Keep a previously trusted signed APK for recovery if available; that is not a backup of private app data. Do not clear data or uninstall to resolve a signature conflict.

## 2. Choose a signed APK

Ask whether Arsenal should be the person's main club, which clubs they want fixtures for, and which players they want to follow (club matches, international matches or both). Allow no main club, no clubs or no players. Version 1.1.0 applies these choices in first-run setup or Settings → Football setup, without rebuilding. Check the actual APK version; older releases do not have this editor. See [CONFIGURATION.md](CONFIGURATION.md).

Build locally by default. There is currently no published signed production APK. If choosing a prebuilt later, check the actual [Releases page](https://github.com/vitalisdexter/gunners-tv-launcher/releases) and its published SHA256/signer, or use a trusted signed file supplied deliberately. Do not assume that a source ZIP, unsigned release build or random search result is an official installable APK.

Alternatively, build locally with Java 17, Android SDK platform 34 and build-tools 34.0.0:

Football choices are now app settings; no personal source patch is needed. Find the actual tool installations and set `JAVA_HOME` and `ANDROID_HOME`; ensure `sdkmanager` and `adb` are available via their real paths. Install missing SDK packages with Google's command-line tools or Android Studio and accept their licences. `sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"` supplies the required SDK components. The first build also downloads public Gradle dependencies.

```powershell
# Windows, from this repository's root
.\Build.ps1
```

If PowerShell script policy prevents `Build.ps1`, run `.\gradlew.bat --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease assembleDebug` directly rather than weakening machine policy.

```sh
# Linux/macOS, from this repository's root
chmod +x gradlew
./gradlew --no-daemon --max-workers=2 testReleaseUnitTest lintRelease assembleRelease assembleDebug
```

Use `app/build/outputs/apk/debug/app-debug.apk` for local evaluation. It is signed by the local debug key. Keep that key private and available for later compatible updates. `app-release-unsigned.apk` cannot be installed until separately signed. Production signing is described in [RELEASE.md](RELEASE.md).

Before installing, inspect the APK with Android SDK build-tools:

```text
apksigner verify --print-certs PATH_TO_SIGNED_APK
aapt dump badging PATH_TO_SIGNED_APK
```

Use the full tool path if needed. Confirm `dev.gunnerstv.launcher`, minimum SDK 26, Gunner's app label and a TV launch activity. Save the APK hash and public signer fingerprint in private installation notes. These checks identify a package; trust still depends on where it came from.

## 3. Connect the intended device

Install Google's [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools). Enable the device's supported debugging option in Developer options. Menu names vary; do not assume a particular build-number screen or network port.

For Wireless debugging, connect the computer and TV to the same trusted network. If the device offers pairing, use the pairing address/port and enter the temporary code locally:

```text
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:CONNECTION_PORT
adb devices -l
```

The pairing port and connection port can differ and can change. Use the values shown by this device; do not assume port 5555. If Wireless debugging is absent, use an explicitly supported manufacturer method or a USB **data/debug** port. A power-only USB socket is not a data connection. Do not enable legacy TCP debugging automatically.

For USB, authorise the computer on the TV, run `adb devices -l` and select that device's serial. For either connection, verify the target with:

```text
adb -s TARGET shell getprop ro.product.model
adb -s TARGET shell getprop ro.build.version.sdk
adb -s TARGET shell pm list features
```

Replace `TARGET` with the exact connected serial or `IP:PORT`. Confirm the model and TV capability before continuing. Keep that target on every device command; do not act on other connected devices.

## 4. Install and launch

For any supported connection, use ADB directly:

```text
adb -s TARGET install -r PATH_TO_SIGNED_APK
adb -s TARGET shell am start -n dev.gunnerstv.launcher/.MainActivity
```

Quote paths containing spaces using the current shell's syntax. `-r` updates a compatible same-package installation while retaining its data. A differently named previous launcher remains a separate app. On signature mismatch, incompatible Android version, verification failure or device refusal, stop and report the exact error. Do not force a downgrade, disable verification, uninstall another launcher or lower the app's minimum SDK.

Windows users with a network target can instead use the supplied wrapper:

```powershell
.\Device.ps1 -Device 'TV_IP:CONNECTION_PORT' -Action Install -Apk '.\app\build\outputs\apk\debug\app-debug.apk'
.\Device.ps1 -Device 'TV_IP:CONNECTION_PORT' -Action Launch
```

`Device.ps1` accepts network targets only; use direct ADB for a USB serial. The script does not pair devices, select Home or grant special access.

## 5. Configure the launcher

Set your name and weather location in launcher Settings, then arrange favourites by holding OK on an app. Choose visible recommendations in Settings. In Football setup choose a main club or none, search/add your clubs and players, choose each player's scope, then Save. Reopen setup to check the saved choices. There is no preselected six-club/two-player list; upgrading from 1.0.x also asks for setup once. See [CONFIGURATION.md](CONFIGURATION.md) for coverage and limits.

In 1.3.0+, hold OK → Move app, use arrows, then OK to save or Back to cancel. Home has one six-app row (left/right); Apps also supports up/down across rows. To choose a different Home favourite, move it earlier in the Apps grid; Home skips apps explicitly hidden from it. Verify both save/persistence and cancel, including the first/last positions and an incomplete final Apps row.

Grant TV listings access only if offered and wanted for shared content. Now Playing uses optional notification access to media sessions; the implementation does not process notification content, although Android's permission is broad. If access is declined or unavailable, leave that feature inactive. Wi-Fi, audio output and Google Home shortcuts depend on the firmware's handlers. No streaming accounts or private playlists need to be entered into this launcher.

Rows appear only when supported apps publish suitable records. A signed-in YouTube app does not guarantee a Continue Watching feed. Do not treat an empty provider row as proof of failed installation.

In 1.2.0+, use Settings → Content sources → Refresh to read current feeds. Grant listings access only if the normal prompt is offered and wanted. When the report lists titles but rows are absent on Home, inspect its hidden-row markers and choose Show shared rows on Home, then scroll below the favourite apps. Report access/table counts separately from positive title counts; private Google TV/provider histories are not available through this route.

For Football News in 1.2.0+, open a fixture → News. Check recent videos for followed clubs, a club filter, and a real YouTube video launch/Back return. Creators can be edited in that tab or Settings → Football News creators. Starter fan channels only apply to clubs already chosen in Football setup. Empty or failed public feeds must not be reported as a successful playback test. See [News sources](docs/NEWS-SOURCES.md).

Now Playing requires a live/controllable media session, even with notification access enabled. YouTube Music may be handled by YouTube on TV; if that app stops playback or removes its session on Home, the launcher cannot supply background play. Music help explains this distinction. Test access, session visibility and control capabilities separately.

## 6. Choose Home, if wanted

Open Android's default-Home settings and select Gunner's. The network wrapper's `-Action HomeSettings` only opens that Settings screen. Where firmware supports it, this command can assign the HOME role:

```text
adb -s TARGET shell cmd package set-home-activity --user 0 dev.gunnerstv.launcher/.MainActivity
adb -s TARGET shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Check the active Android user before using `--user 0`; choose the actual user where appropriate. If the command is unavailable, use the device's Settings. A successful role command alone does not establish which Home activity the firmware will open.

Some onn./Google TV firmware gives its protected stock Home activity higher priority. Gunner's has an optional helper for the exact stock Google TV Home window. If required, enable `dev.gunnerstv.launcher/.HomeRedirectService` manually in Accessibility and keep the redirect enabled in Gunner's Home settings. This helper does not read window contents or filter remote keys. Preserve unrelated accessibility services. It is not a universal workaround for other vendors' launchers.

If replacing another custom launcher, check for an existing Home redirect first. When choosing Gunner's as Home, coordinate switching only the positively identified previous launcher's redirect/helper and retain its original state for recovery. Two redirectors can compete; do not enable both blindly or overwrite the full accessibility-service list. Leave unrelated services alone.

Test the physical remote's Home button and a restart before calling the default change successful. If the firmware refuses, keep Gunner's as a manually launched app and report the limitation. Do not disable stock Home, setup, system services, casting, updates or apps to force replacement.

## 7. Verify and recover

With the remote, check Home/Back, app launching, the same card/scroll position on return, Home-to-top, weather, audio and optional media controls. Verify provider resume only for actual supplied entries. Test live scores during a real match before claiming live progression works. If making Gunner's Home, restart from system Settings with the user's agreement and check that both Home and system Settings remain accessible.

To return to the previous launcher:

1. Open Gunner's Settings → Home screen → Open Google TV where that option applies; it turns off this app's redirect and opens Google TV.
2. In Android Accessibility, turn off only Gunner's Home redirect if enabled.
3. Select the previous Home in Android's default-app settings. For other vendors, choose their recorded Home directly.
4. Press the physical Home button and verify the previous launcher opens. Keep Gunner's installed until recovery is confirmed.

The wrapper's `-Action StockHome` only opens Google TV on supported firmware; it does not restore a default or turn off an accessibility helper. Do not rely on it as a complete rollback.

Once setup/recovery checks are complete, forget the paired computer, revoke debugging authorisations and turn off any debugging enabled for this installation. Disconnect that target with `adb disconnect TV_IP:CONNECTION_PORT`. Disconnecting alone does not disable the TV's debugging service. No PC shutdown is part of setup.

## Reference and verification status

Connection steps follow [Android's ADB documentation](https://developer.android.com/tools/adb); APK requirements follow [Android app signing](https://developer.android.com/studio/publish/app-signing). Follow the capabilities actually available on the device.

[VALIDATION.md](VALIDATION.md) records local builds/tests and outstanding device checks. This guide does not claim that every TV model, physical remote, provider or firmware has been tested.
