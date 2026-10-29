# Football News sources

Open a fixture's football centre and choose **News**. The tab shows public YouTube creator videos for your followed clubs. Use **Creators** there, or **Settings → Football News creators**, to add/remove sources. Select a club, then enter a public `https://www.youtube.com/@handle` URL, `/channel/UC…` URL or UC channel ID. The app checks YouTube's public channel/RSS identity. If handle discovery is unavailable (including a regional consent redirect), use the UC channel URL or ID instead; the launcher does not complete a consent/account flow. Removing a starter source persists; it is not automatically restored on the next refresh.

The starter catalogue below applies only to clubs you explicitly followed in Football setup. It does not select clubs, players or a main club for you. Other clubs can have creators added manually. Player-only following does not choose a fan channel automatically; add a followed club to configure its creators. Channels are independent creators, not an endorsement or an assurance of editorial accuracy. Titles are displayed as published; videos can include opinion, spoilers or advertising.

| Followed club | Starter creator | Public channel ID |
| --- | --- | --- |
| Arsenal | [AFTV](https://www.youtube.com/@AFTVmedia) | `UCBTy8j2cPy6zw68godcE7MQ` |
| Chelsea | [Blue Lions TV](https://www.youtube.com/@BlueLionsTVNiiNiiFC) | `UCxGkVbPmzcMLbPj7OPcRAWg` |
| Manchester City | [City Xtra](https://www.youtube.com/@CityXtra) | `UCSZfggT235t8_uaMREZ8IPQ` |
| Manchester United | [The United Stand](https://www.youtube.com/@UnitedStand) | `UCMmVPVb0BwSIOWVeDwlPocQ` |
| Liverpool | [The Anfield Wrap](https://www.youtube.com/@TheAnfieldWrap) | `UCc5C5dNupCMbyutNatfBujQ` |
| Tottenham | [WeAreTottenhamTV](https://www.youtube.com/@WeAreTottenhamTV) | `UCUz_XIKFQOrliSqPZWJBq2g` |

The corresponding public feeds were checked on 29 October 2026 at `https://www.youtube.com/feeds/videos.xml?channel_id=CHANNEL_ID`. Channel branding/availability can change. No raw feeds or videos are bundled.

## Limits and refresh

- Up to 12 club/creator mappings; the same channel is fetched once per refresh even if linked to several clubs.
- Only items published in the previous seven days are retained. Updating an old video's metadata does not make it recent. Duplicate video IDs are merged and newest uploads appear first.
- YouTube's RSS response currently provides only its latest 15 entries; it is not a complete archive and has no launcher-specific availability guarantee. A busy channel may therefore omit some recent uploads. Regular videos, Shorts and public stream posts can appear; this feed is not an editorial or football-relevance classifier.
- The combined model keeps up to 60 videos; the selected club filter displays the newest 24 in a remote-navigable grid.
- Feeds normally cache for 30 minutes. Manual Refresh has a one-minute minimum between attempts per channel. Failed requests retain valid cached posts, still limited to seven days. With no recent/cached posts, an honest empty state is shown.
- Standard public channel-page RSS metadata is used only to resolve handles. HTTPS, host/identity checks, request deadlines, redirect and response-size limits apply. XML declarations of external entities/DTDs are rejected. No account sign-in, cookies, API key, YouTube private endpoint or paid service is used.
- Thumbnails are bounded downloads from YouTube's image service. Selecting a card opens its actual video in YouTube, with a browser fallback. The launcher does not download/play videos, extract account history, infer resume positions or bypass provider restrictions.

This tab is separate from Home's provider-specific recommendations and Continue Watching. Those require records the installed TV apps share with Android TV; a public creator feed cannot substitute for private viewing history.
