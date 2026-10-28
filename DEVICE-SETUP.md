# Deliberate installation and recovery

These instructions are for a later, explicitly approved device test. Nothing runs automatically. Keep a working stock Home and remote, and record your previous default before changing it.

1. Build a signed debug QA APK locally, or arrange secure production signing separately. Enable/pair debugging yourself if you choose to test.
2. `./Device.ps1 -Device HOST:PORT -Action Install -Apk ./app/build/outputs/apk/debug/app-debug.apk`
3. `./Device.ps1 -Device HOST:PORT -Action Launch` opens `dev.gunnerstv.launcher/.MainActivity`.
4. Choose the default manually through `-Action HomeSettings`. On firmware where role selection is supported, `adb -s HOST:PORT shell cmd package set-home-activity --user 0 dev.gunnerstv.launcher/.MainActivity` assigns the role. Verify normal resolution with `adb -s HOST:PORT shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`. A successful role command is not proof of changed resolution.
5. On the tested onn. firmware, protected Google TV Home has higher intent priority, so a narrow accessibility redirect was required despite the HOME role. If needed, manually enable only `dev.gunnerstv.launcher/.HomeRedirectService` in Accessibility (`-Action Accessibility`). Preserve all other accessibility services. Stop on refusal. Never disable Google TV, setup, firmware or system packages to force Home.
6. To return to stock, choose the TV default in launcher Home settings and turn off this app’s HomeRedirectService in Accessibility, then choose the previous default in Android Home settings. `-Action StockHome` only opens Google TV on supported firmware; it does not change the default or disable a helper. If this launch fails, use Android Settings. Keep debugging available until navigation/recovery is verified, then disable/revoke it yourself.

Linux/macOS equivalents use `adb connect HOST:PORT`, then `adb -s HOST:PORT install -r PATH_TO_SIGNED_APK` and the component above. Never omit the explicit target when more than one device is connected. Signature conflict means stop; there is no automatic uninstall or data migration.

Before release: test real remote Home/Back, reboot recovery, focus restoration, app launches/resume, weather, audio output, media access, casting and a real live match on the target firmware. No physical TV checks of these new projects have been performed.
