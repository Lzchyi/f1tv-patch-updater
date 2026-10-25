# Tested environment and validation

## Proven TV environment

Tested on **25 October 2026**:

| Item | Tested configuration |
| :--- | :--- |
| TV manufacturer | Sony |
| Model reported by Android | BRAVIA VH1 |
| Operating system | Android 12, API 31 |
| CPU architecture | 32-bit ARM: `armeabi-v7a` / `armeabi` |
| TV capture resolution | 1920 × 1080 |
| Installed F1 TV patch | 3.0.50 (`3.0.50-SP168.9.0-release-R55-tv-UHD`) |
| Updater editions | Original-logo personal build and temporary public-edition installations |

**The updater works on this TV.** Readable F1 TV version detection, matching-build checks without an unnecessary APKM download, D-pad focus, the single-screen layout and disclaimer bounds were checked on the device. Updater upgrades preserved app data and left the installed F1 TV package unchanged. The owner also confirms working F1 TV playback.

This is a verified environment, rather than a promise that every Android TV device behaves identically. Playback is owner-reported; these updater tests do not measure the decoder's 4K resolution, HDR output or colour accuracy.

## 0.3.1 self-update validation

The host suite has **33 tests**, covering the original F1 TV policies plus public-release version comparison, exact asset selection, repository URL/size/SHA-256 checks, rejection of edition changes, changed signing keys and non-newer APKs.

The physical-TV self-update tests use a separate updater package and a controlled feed. They exercise:

- Metadata-only startup and daily checks, with no automatic updater APK download.
- Later surviving restart, with a manual check able to offer the update again.
- Rejection of bad SHA-256, wrong signing keys, wrong app packages and old APKs; the installed updater stays in place.
- Android cancellation keeping a verified APK for retry.
- Retry using the same saved APK without another download.
- Replacing the updater's own process in place and preserving its preferences.
- Cleanup through the package-replaced receiver, including repeated completion callbacks.
- Recognition of the newly installed version on the next check.
- Independent self-update checks before F1 TV is installed, with manual setup still available after Later.
- A one-time installation-permission handoff, returning from TV settings into the requested self-update, and cancellation with the candidate retained.

Personal and permanently signed public builds are checked separately. Only the public edition has a published self-update feed; personal builds retain their private branding and local installation workflow. Device connection details, private signing material and diagnostic logs are not published.

## Previously verified setup and recovery

The 0.3.0 first-install path was exercised on this same physical TV with a separate target and a synthetic bundle: declining setup caused no download; digest mismatches and altered saved files were rejected; approved downloads survived restart; Android cancellation could be retried; a base APK plus two splits installed successfully and the bundle was cleaned up. The real F1 TV installation was preserved throughout.

The 0.2.3 checks verified interrupted-download/extraction cleanup, pending-candidate retention, obsolete-candidate removal and recovery after a simulated missed installer success callback. Host-test counts were 5 in 0.2.2, 20 in 0.2.3 and 25 in 0.3.0.

## Build and CI checks

CI builds the community debug edition, runs the host tests and lints it. Local release verification also checks the personal APK, the signed public APK, their application IDs and retained signing identities. CI debug artifacts are not the signed public release APK.

No simulator was launched. Fresh installation of the actual F1 TV bundle on an empty TV, a newer upstream F1 TV upgrade beyond 3.0.50, reboot scheduling and other device models are outside the results recorded here. Additional successful physical-device reports can extend this record.
