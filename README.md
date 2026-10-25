# Telugu News

Telugu live-news app for one viewer. Portrait: video on top, a 3x3 grid of big channel tiles (no scrolling for 9 channels), one tap switches. Landscape: fullscreen; one tap shows two buttons (Channels, Reload), the channel strip opens only from the Channels button, and picking a channel closes it at once.
Kotlin + Compose + Media3, minSdk 29, sideload only. Dark UI, amber = the selected channel.

Logo: the Telugu letter తె (first syllable of "Telugu") traced from Noto Sans Telugu Bold (OFL) plus a red "live" dot. Vector sources are `app/src/main/res/drawable/ic_launcher_*.xml`.

## How playback picks a stream

Per channel, `sources` in `app/src/main/assets/channels.json` are tried in order:
1. `hls`: direct CDN playlist (no ads).
2. `youtube`: channel id (`UC…`) or handle (`@name`) plus a `title` hint. Channels run several lives at once (the 24/7 feed plus special coverage, sometimes with a broken slate), so the app lists the channel's live streams, picks the one whose title contains the hint (case-insensitive), and gets its HLS URL via NewPipeExtractor. No match: it falls back to whatever YouTube shows at `/live`.

A source that errors or buffers for 20 s is skipped. All sources failed: "No signal" screen, auto-retry every 15 s, big Reload button.

## Channel fields

`id`, `name` (English, shown in the UI; the app is English-only), `group` (the tab: "News", "Devotional" or "Sports"; missing = "News"; tabs appear when there is more than one group), `logo` (https URL), optional `plate` (`#RRGGBB` behind the logo, default white; 10TV needs a dark one because its "10" is white), optional `liveOnly` (see below), `sources`.

`liveOnly: true` is for event channels that only stream on match days (ICC, FIFA, Pro Kabaddi…). The app asks YouTube which of them are live when it opens and every 3 minutes after, and lists only those. The one being watched stays listed until he switches away, and a tab with nothing live disappears. A failed check keeps the last answer. Use YouTube sources only; keep the `title` hint off, so it plays whatever `/live` shows.

Sports HLS sources are third-party restreams of pay channels (Star Sports 2, Eurosport) and can die any day; only `https` ones work, since the app blocks cleartext.

More channels we could add, with a liveness check: see [CHANNELS.md](CHANNELS.md).

## Channel list updates

`manifestUrl` in `gradle.properties` points at this repo's own `channels.json` on GitHub (blank = bundled list only), so editing and pushing that file updates every installed phone. The app fetches that raw JSON each time it opens, validates it, caches it, and uses it if its `version` is >= the one it has. Bump `version` on every edit. Invalid entries (non-https, unknown source type) are dropped.

## Commands

```
./gradlew :app:testDebugUnitTest                 # unit tests
LIVE_SMOKE=1 ./gradlew :app:testDebugUnitTest --tests '*LiveSourcesSmokeTest*'
                                                 # hits the network; run from an Indian IP
./gradlew :app:assembleDebug                     # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Smoke-test results per source are in `app/build/test-results/testDebugUnitTest/TEST-app.vaartalu.data.LiveSourcesSmokeTest.xml`.

## When a channel dies

1. Run the smoke test to see which source failed.
2. Fix the URL in `channels.json` (and the remote copy), bump `version`.
3. YouTube fallback broken for every channel at once: bump NewPipeExtractor in `app/build.gradle.kts`.

The direct HLS links come from third-party CDNs (smartplaytv, pishow, yupptv), not the channels, and can disappear without notice.
