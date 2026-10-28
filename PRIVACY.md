# Privacy

No analytics or telemetry is added. Settings, recently opened app/focus state, provider feed caches and optional media-session state stay in the app’s private storage; Android backup is disabled. Export no logs or screenshots without reviewing their contents.

Weather requests send configured or time-zone-city coordinates to Open-Meteo. Football requests send team/player/event IDs to ESPN. Artwork requests go to the URLs supplied by content providers; those servers see the device IP and request metadata. This is not a promise of anonymity or of the providers’ privacy practices. Provider-owned deep links open the relevant app. No passwords, cookies, Spotify history, private streaming databases or playlist credentials are read.

Notification access is optional and Android grants broad capability: this implementation uses media sessions, not notification contents. The narrow HOME helper watches only stock HOME window events, without screen-content reads or key filtering. Review special access in Android Settings and disable it when unwanted. Clear app data to erase local settings/caches (after choosing a working stock Home); disconnect special access in Android Settings.

The optional Buy me a coffee screen displays a bundled QR code offline. It contacts no donation service automatically. Choosing Open link launches the public profile in a separate handler/browser; scanning uses your phone. Buy Me a Coffee handles its own site, payments and privacy practices. No payment details, donation status or tracking parameters are collected by this launcher.
