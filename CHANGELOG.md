# Changelog

## 1.2.0 — shared content visibility and football News

- Home row composition now uses the current readable feed rather than an obsolete availability cache, and discovery rebuilds a mismatched Home layout. Explicit per-row visibility preferences remain respected.
- Content sources now offers Refresh and Show shared rows on Home, identifies hidden rows, and reports safe access/table counts. Empty feeds retain a reachable setup/status card when supported apps are installed. Listings permission results trigger a fresh provider read; public rows are no longer blocked solely by an absent permission when actually readable.
- Added the football centre's News tab, club filters, bounded video cards and editable public YouTube fan-channel sources. Starter creators apply only to already-followed clubs. Recent videos use public channel RSS, seven-day freshness, caching and actual YouTube links; no account history is fabricated.
- Added regressions for real-feed Home visibility, access diagnostics, genuine unfinished progress with missing Watch Next type, channel/feed validation, publication freshness, cache timing and followed-club source selection. No new runtime library or permission. No TV installation is part of this update.

## 1.1.0 — football setup and remote navigation fixes

- Bounded the Home widgets editor with a scrolling viewport and focus margins, including row reorder and motion-off handling, so selection stays on screen.
- Added first-run and Settings football setup: explicit main-club/no-main choice, provider name search for clubs/players, player scope and represented national side, local persistence and empty selections. Removed the compiled six-club/two-player preset and fixed Arsenal fallback. Supports up to 12 clubs/8 players within provider coverage; upgrading 1.0.x asks for setup once.
- Added media-listener reconnect/retry, empty-title fallback, capability-aware explicit/media-button play-pause and disabled unsupported skip actions. Added Music help explaining provider session/background-play limits. Media artwork is bounded and decoded on the artwork worker.
- Isolated football requests from weather/content workers, abandoned obsolete-selection loads between requests and changed fixture display from forced UK time to the TV's local time zone.
- Updated the GitHub installation prompt to configure football on the TV and verify weather and provider-specific music results explicitly. No TV installation is performed by a build/source update.

## 1.0.1 — optional support link

Added Vitalis's Buy Me a Coffee link to the README and launcher Settings/About. The TV screen displays an offline QR code and an optional browser action with a no-browser fallback. Added no app permission, runtime dependency, payment integration or paid feature. Physical TV navigation/scanning/browser checks remain to be performed.

## Unreleased — documentation and repository presentation

Updated the installation prompt and manual setup guide to start from the public GitHub repository on Windows, macOS or Linux without GitHub login. Moved football resource customisation before compilation, documented tool setup, signer retention and replacement of a previous launcher redirect, and replaced outdated unpublished-repository statements.

Pinned hosted verification to Ubuntu 24.04 and an explicit SDK manager path after the first GitHub run stopped with `sdkmanager: command not found` on `ubuntu-latest`.

Added installation intake questions for Arsenal as main club, followed clubs, followed players and club/international fixture preferences, with explicit current configuration limits.

Expanded the product README, installation/recovery guide and reusable assistant installation prompt. Moved component provenance and standalone component licence files under `legal/`, preserving their text. Legacy product/developer references are confined to legal attribution and historical Git records. No runtime code, package identity or APK was changed in this documentation update.

## 1.0.0 — public preparation

Independent build root; private defaults removed; unsigned release/debug QA separation; licence and provenance documentation. Separate Gunner identity and focused network/configuration fixes.
