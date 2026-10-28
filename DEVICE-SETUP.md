# Install Gunner's TV Launcher

This guide installs `dev.gunnerstv.launcher`. For assistant-led setup, use [INSTALL-PROMPT.md](INSTALL-PROMPT.md). Choose the target device and APK deliberately; the build scripts never install automatically.

## 1. Check the target

You need an Android TV or Google TV device with Android 8.0/API 26 or newer, a working remote, and access to system Settings. Manufacturer-specific Android boxes need their TV features checked. Tizen, webOS, Roku and Apple TV cannot run this APK directly. Fire TV support and Home replacement are not certified by this project.

Leave the existing launcher available. Record the current default Home in a private local note. If Gunner's is already installed, record its version and signing certificate before updating. Keep a previously trusted signed APK for recovery if available; that is not a backup of private app data. Do not clear data or uninstall to resolve a signature conflict.

## 2. Choose a signed APK

Use a maintainer-provided signed APK from a verified source, checking its published SHA256 and signer when available. This source tree does not name a public release URL. Do not assume that a random search result is an official download.

Alternatively, build locally with Java 17, Android SDK platform 34 and build-tools 34.0.0:

```powershell
# Windows, from this repository's root
.\Build.ps1
```

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

Set your name and weather location in launcher Settings, then arrange favourites by holding OK on an app. Choose visible recommendations in Settings. See [CONFIGURATION.md](CONFIGURATION.md) for supported followed-team/player defaults.

Grant TV listings access only if offered and wanted for shared content. Now Playing uses optional notification access to media sessions; the implementation does not process notification content, although Android's permission is broad. If access is declined or unavailable, leave that feature inactive. Wi-Fi, audio output and Google Home shortcuts depend on the firmware's handlers. No streaming accounts or private playlists need to be entered into this launcher.

Rows appear only when supported apps publish suitable records. A signed-in YouTube app does not guarantee a Continue Watching feed. Do not treat an empty provider row as proof of failed installation.

## 6. Choose Home, if wanted

Open Android's default-Home settings and select Gunner's. The network wrapper's `-Action HomeSettings` only opens that Settings screen. Where firmware supports it, this command can assign the HOME role:

```text
adb -s TARGET shell cmd package set-home-activity --user 0 dev.gunnerstv.launcher/.MainActivity
adb -s TARGET shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Check the active Android user before using `--user 0`; choose the actual user where appropriate. If the command is unavailable, use the device's Settings. A successful role command alone does not establish which Home activity the firmware will open.

Some onn./Google TV firmware gives its protected stock Home activity higher priority. Gunner's has an optional helper for the exact stock Google TV Home window. If required, enable `dev.gunnerstv.launcher/.HomeRedirectService` manually in Accessibility and keep the redirect enabled in Gunner's Home settings. This helper does not read window contents or filter remote keys. Preserve unrelated accessibility services. It is not a universal workaround for other vendors' launchers.

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
