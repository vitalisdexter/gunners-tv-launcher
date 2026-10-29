# Configuration

- Favourites: hold OK on an app to arrange favourites; `default_favourites` in `app/src/main/res/values/launcher_config.xml` supplies a fresh-install starter list. Missing apps are skipped. No streaming APK is bundled.
- Owner name: Settings → Your name; empty by default, stored only locally.
- Weather: Settings → Weather location accepts a city, latitude and longitude. Clear all three to return to the TV time-zone city. Inputs are validated; this is not GPS or postcode lookup. Empty resource defaults use the time-zone main city, not precise location. Full-screen historical forecast uses Celsius; the utility widget supports its existing temperature setting. No private default location is included.
- Football: first-run setup or Settings → Football setup. No clubs/players are preselected. Explicitly choose Arsenal, another main club or no main club, then search by name for up to 12 clubs and 8 players. Confirm the result's league to distinguish similarly named teams. Each player has club/international/both scope and confirmed team associations. Search/profile/bio responses come from the public ESPN directory; unknown/unavailable names are not invented. No resource edits, provider-ID entry, accounts or subscriptions are required.
- Runtime permissions: TV listings enable provider rows. Notification access optionally enables Now Playing media-session controls. The listener does not process notification content. Accessibility HOME redirect is optional and firmware-dependent.

Future migration option: a user-initiated settings export/import with explicit field allowlist and schema version; it is not implemented here. Never copy another app’s private database.

## Football setup questions

Before selecting or building an installation, ask the person:

1. Would you like Arsenal to be your main club? If not, would you prefer another main club or no main club?
2. Which clubs would you like to keep tabs on for fixtures? Several clubs or none are valid choices.
3. Which players would you like to follow, and should their matches include club games, international games or both? No players is also valid.

Clarify ambiguous names and summarise the choices. The app resolves provider IDs from search results; do not make the person find IDs or edit source. A chosen main club is included in the followed list. There are no starter clubs/players to retain silently.

Version 1.1.0 applies these choices in the app. Choose Save to persist them; Cancel discards the draft. A main club is also followed; removing it clears the fallback. When no selected game is live, the top match widget shows the chosen main club's next scheduled game. With no main club, it shows selected live games only. All supplied competitions are included within the 28-day future/two-day past window, ordered by kickoff. Kickoff times use the TV's local time zone.

For players, the provider's current club is refreshed periodically to reflect transfers. Reported national sides are presented for confirmation when ambiguous; citizenship is not used to guess representation. If no national metadata is available, search for the represented side manually or choose club-only scope. Retired players, incomplete rosters and unannounced selections cannot establish future participation: fixtures remain tentative until a lineup/appearance confirms it. Search coverage and endpoints can change.

Upgrading from 1.0.x opens the new setup once because the old preset was compiled into the app, not stored as a user selection. Choose the desired list explicitly; the old six-club/two-player preset is not silently imported. Existing name, weather, favourites and row settings are kept. Later 1.1.x updates retain saved football choices. No account or private database migration is performed.

## Now Playing

Grant optional notification access, start playback in the provider app and return Home. The launcher observes media sessions and connection lifecycle only, not notification contents. Supported sessions may expose explicit play/pause or media-button toggle controls; unavailable skip controls are disabled. Some YouTube Music TV entry points play through YouTube, and some providers stop/remove their session on Home. If a paused controllable session remains, Play can request resume; the provider decides whether it accepts. The launcher never resumes it automatically or forces background playback. Use Music help in the empty widget to distinguish that limitation from access/session problems. Artwork downloads are bounded and decoded off the UI thread. Google's current [TV-device guidance](https://support.google.com/youtubemusic/answer/6308244?hl=en) describes the YouTube/YouTube Music device and background-play limitations.
