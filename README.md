# Telugu News

Telugu live-news app for one viewer. Portrait: video on top, a 3x3 grid of big channel tiles (no scrolling for 9 channels), one tap switches. Landscape: fullscreen; one tap shows two buttons (Channels, Reload), the channel strip opens only from the Channels button, and picking a channel closes it at once.
Kotlin + Compose + Media3, minSdk 29, sideload only. Dark UI, amber = the selected channel.

Logo: the Telugu letter తె (first syllable of "Telugu") traced from Noto Sans Telugu Bold (OFL) plus a red "live" dot. Vector sources are `app/src/main/res/drawable/ic_launcher_*.xml`.

## How playback picks a stream

Per channel, `sources` in `app/src/main/assets/channels.json` are tried in order:
1. `hls`: direct CDN playlist (no ads).
2. `youtube`: channel id (`UC…`) or handle (`@name`); the app finds the current live video from the channel's `/live` page and gets its HLS URL via NewPipeExtractor.

A source that errors or buffers for 20 s is skipped. All sources failed: "No signal" screen, auto-retry every 15 s, big Reload button.

## Channel fields

`id`, `name` (English, shown in the UI; the app is English-only), `group` (the tab: "News" or "Devotional"; missing = "News"; tabs appear when there is more than one group), `logo` (https URL), optional `plate` (`#RRGGBB` behind the logo, default white; 10TV needs a dark one because its "10" is white), `sources`.

More channels we could add, with a liveness check: see [CHANNELS.md](CHANNELS.md).

## Channel list updates

`manifestUrl` in `gradle.properties` (blank = bundled list only). When set, the app fetches that raw JSON on every start, validates it, caches it, and uses it if its `version` is >= the one it has. Bump `version` on every edit. Invalid entries (non-https, unknown source type) are dropped.

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
