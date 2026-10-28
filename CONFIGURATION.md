# Configuration

- Favourites: hold OK on an app to arrange favourites; `default_favourites` in `app/src/main/res/values/launcher_config.xml` supplies a fresh-install starter list. Missing apps are skipped. No streaming APK is bundled.
- Owner name: Settings → Your name; empty by default, stored only locally.
- Weather: Settings → Weather location accepts a city, latitude and longitude. Clear all three to return to the TV time-zone city. Inputs are validated; this is not GPS or postcode lookup. Empty resource defaults use the time-zone main city, not precise location. Full-screen historical forecast uses Celsius; the utility widget supports its existing temperature setting. No private default location is included.
- Football: edit `followed_club_ids` and `followed_player_ids` in the same XML. The public starter preset follows Arsenal, Chelsea, Man City, Man United, Liverpool and Spurs, plus Ronaldo (22774) and Mbappé (231388). All club/international competitions are considered within the 28-day future/two-day past window. Remove either player item to stop following. Other player IDs require an explicit corresponding name/national-team mapping in FootballSource; unknown IDs are not silently supported. Arsenal remains the documented upcoming fallback. No account history is imported.
- Runtime permissions: TV listings enable provider rows. Notification access optionally enables Now Playing media-session controls. The listener does not process notification content. Accessibility HOME redirect is optional and firmware-dependent.

Future migration option: a user-initiated settings export/import with explicit field allowlist and schema version; it is not implemented here. Never copy another app’s private database.

## Football setup questions

Before selecting or building an installation, ask the person:

1. Would you like Arsenal to be your main club? If not, would you prefer another main club or no main club?
2. Which clubs would you like to keep tabs on for fixtures? Several clubs or none are valid choices.
3. Which players would you like to follow, and should their matches include club games, international games or both? No players is also valid.

Clarify ambiguous names, summarise their choices and avoid silently retaining starter selections they did not request. Include a chosen main club in the followed-club list. Resolve supported club names to the provider's IDs when building; do not make the person find IDs themselves.

The current release provides followed-club/player arrays at build time, not a football selection screen. Its upcoming-game fallback is fixed to Arsenal; there is no configurable alternate main club or off switch for that fallback yet. The implemented player mappings cover Ronaldo and Mbappé across both club and international fixtures; other players and separate club/international player filters need further implementation. These questions collect preferences rather than adding those missing app features. Explain unsupported choices and agree on a supported subset or defer setup. A prebuilt APK with no editor retains its preset; supported custom selections require a source build.
