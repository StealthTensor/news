# Channel backlog

Telugu channels we **could** add. Nothing here is in the app yet.

Source: the iptv-org Telugu playlist, checked 2026-10-17 from an Indian IP. **up** = the HLS playlist answered with `#EXTM3U`. It does not prove the picture plays or is ad-free, and the YouTube fallback was not checked. Third-party restreams can vanish any day. 56 of 70 were up.

Already in the app: TV9, NTV, ABN, TV5, Sakshi, 10TV, Mahaa, ETV AP, iNews (News tab); SVBC, Bhakthi TV, Mahaa Bhakti (Devotional tab).

Skipped on purpose (Telangana-focused): ETV Telangana, T News, V6 News. AP 24x7: YouTube channel exists but no live stream and no CDN source found.

## News (14 up / 14)

| Channel | iptv-org id | Playlist |
|---|---|---|
| 4 SiDES TV | `4SiDESTV` | up |
| 6 TV Telugu | `6TVTelugu` | up |
| 9 Plus News | `9PlusNews` | up |
| 99TV | `99TV` | up |
| BIG TV | `BIGTV` | up |
| BRK News | `BRKNews` | up |
| CVR News | `CVRNews` | up |
| ETV News | `ETVNews` | up |
| HMTV (India) | `HMTV` | up |
| Mojo TV | `MojoTV` | up |
| Prime9 News | `Prime9News` | up |
| Raj News Telugu | `RajNewsTelugu` | up |
| Swatantra TV | `SwatantraTV` | up |
| Zee Telugu News | `ZeeTeluguNews` | up |

## Religious (7 up / 8)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Aastha Telugu | `AasthaTelugu` | up |
| CVR OM Spiritual | `CVROMSpiritual` | up |
| Divyavani TV | `DivyavaniTV` | up |
| Gospel TV India | `GospelTVIndia` | up |
| Nireekshana TV | `NireekshanaTV` | up |
| PMC Telugu | `PMCTelugu` | up |
| Subhavaartha TV | `SubhavaarthaTV` | up |
| Hindu Dharmam | `HinduDharmam` | down |

## Entertainment (9 up / 12)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Discovery Channel (India) | `DiscoveryChannel` | up |
| ETV Josh | `ETVJosh` | up |
| Mahaa Max | `MahaaMax` | up |
| Mango Mobile TV | `MangoMobileTV` | up |
| Star Maa HD | `StarMaa` | up |
| Studio Yuva | `StudioYuva` | up |
| Telugu One | `TeluguOne` | up |
| Vanitha TV | `VanithaTV` | up |
| Vissa TV | `VissaTV` | up |
| ETV Plus | `ETVPlus` | down |
| ETV Telugu HD | `ETVTelugu` | down |
| Zee Telugu HD | `ZeeTelugu` | down |

## Movies (3 up / 6)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Star Maa Movies HD | `StarMaaMovies` | up |
| Studio One + | `StudioOnePlus` | up |
| Tolly TV | `TollyTV` | up |
| ETV Cinema | `ETVCinema` | down |
| Sony Pix HD | `SonyPix` | down |
| Zee Cinemalu HD | `ZeeCinemalu` | down |

## Music (2 up / 3)

| Channel | iptv-org id | Playlist |
|---|---|---|
| ETV Beats | `ETVBeats` | up |
| Raj Musix Telugu | `RajMusixTelugu` | up |
| ETV Music | `ETVMusic` | down |

## Comedy (1 up / 1)

| Channel | iptv-org id | Playlist |
|---|---|---|
| ETV Comedy | `ETVComedy` | up |

## Lifestyle (1 up / 2)

| Channel | iptv-org id | Playlist |
|---|---|---|
| CVR Health | `CVRHealth` | up |
| ETV Life | `ETVLife` | down |

## Kids (5 up / 7)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Discovery Kids | `DiscoveryKids` | up |
| ETV Bal Bharat | `ETVBalBharat` | up |
| Hungama TV | `HungamaTV` | up |
| Nickelodeon (India) | `Nickelodeon` | up |
| Sonic | `Sonic` | up |
| Disney Channel (India) HD | `DisneyChannel` | down |
| Disney Junior (India) | `DisneyJunior` | down |

## Animation (4 up / 5)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Cartoon Network (India) | `CartoonNetwork` | up |
| Cartoon Network HD+ | `CartoonNetworkHDPlus` | up |
| Super Hungama | `SuperHungama` | up |
| WOW Kidz Telugu | `WOWKidz` | up |
| Sony Yay! | `SonyYay` | down |

## Sports (2 up / 3)

| Channel | iptv-org id | Playlist |
|---|---|---|
| DD Sports | `DDSports` | up |
| Star Sports 2 Telugu HD | `StarSports2Telugu` | up |
| Star Sports 2 | `StarSports2` | down |

## Documentary (3 up / 4)

| Channel | iptv-org id | Playlist |
|---|---|---|
| History TV18 | `HistoryTV18` | up |
| National Geographic Wild (India) HD | `NationalGeographicWild` | up |
| Sony BBC Earth HD | `SonyBBCEarth` | up |
| National Geographic (India) (576p) [Geo-blocked] | `NationalGeographic` | down |

## General (4 up / 4)

| Channel | iptv-org id | Playlist |
|---|---|---|
| DD Saptagiri | `DDSaptagiri` | up |
| DD Yadagiri | `DDYadagiri` | up |
| Metro TV (India) | `MetroTV` | up |
| Sun Gemini | `SunGemini` | up |

## Undefined (1 up / 1)

| Channel | iptv-org id | Playlist |
|---|---|---|
| Calvary TV | `CalvaryTV` | up |

## How to add one

1. Find its stream URL: open the iptv-org Telugu playlist (`https://iptv-org.github.io/iptv/languages/tel.m3u`) and search for the id above.
2. Find its official YouTube channel id for the fallback: `yt-dlp --flat-playlist --print "%(channel)s | %(channel_id)s | %(live_status)s" "ytsearch5:<name> live"`, then confirm with `yt-dlp --print "%(channel)s %(is_live)s" https://www.youtube.com/channel/<id>/live`. Pick the one with the most subscribers.
3. Add an entry to `app/src/main/assets/channels.json` (`group` picks the tab) and bump `version`.
4. Run `LIVE_SMOKE=1 ./gradlew :app:testDebugUnitTest --tests '*LiveSourcesSmokeTest*'` and read the per-source results in the XML report.
