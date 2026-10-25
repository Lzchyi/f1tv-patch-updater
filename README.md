# F1TV Patch Updater (Android TV prototype)

An independent Android TV updater for the **already-installed, patched** F1 TV app from [Alexvbp/f1tv-4k-patch](https://github.com/Alexvbp/f1tv-4k-patch). This project does **not** patch F1 TV, bypass DRM, or include F1 TV binaries.

**Version 0.2.0:** native Android TV updater with readable installed versions, ordinary in-place updates, and an explicit replacement flow when an upstream signing key changes. Build, host tests, lint, installation, and UHD playback are separate checks; see [validation notes](docs/validation.md) for host checks and remaining device tests.

## Implemented in source

- Native leanback Android TV launcher, remote-focused button UI (Java; no third-party runtime libraries).
- Android JobScheduler daily stable GitHub release check, with unmetered network constraint; an immediate check on first launch and manual checks.
- Automatic download of the upstream `f1tv-uhd-patched.apkm` bundle.
- Bounded ZIP extraction with traversal prevention and archive-size limits.
- ABI, density and language split selection for known upstream bundle formats.
- Readable installed version names (for example `3.0.49.4-…-tv-UHD`), with numeric version codes used only for upgrade comparisons.
- Package-name, minimum Android version, version-code, and signing-certificate checks.
- Pinned SHA-256 and signer metadata; revalidation before removal and installation.
- Multi-APK installation through `PackageInstaller` with Android user confirmation.
- Never automatically uninstall or downgrade F1 TV. A changed signing key offers a separate, user-confirmed replacement through Android.
- Preserve the verified download across uninstall, cancellation, process restart, and installation failure so replacement can be retried on the TV.

## Critical caveats

1. **Initial installation:** Patched F1 TV must already be installed to prepare a new candidate. If the user removes it during a verified replacement, the updater can install that pinned candidate. It refuses arbitrary first installs.
2. **Signing-key changes:** Android cannot perform an in-place update between unrelated keys. Same-key updates (and recognized signing lineages) use the ordinary update path. Otherwise the updater requires a SHA-256 digest from the trusted upstream GitHub release, verifies and saves the bundle, explains data loss, and opens Android’s uninstall confirmation only after the user chooses to continue. **Removal erases F1 TV login and local settings.** Sign in again after replacement. If interrupted, reopen the updater and choose **Install downloaded update**. Declining removal leaves the working F1 TV app intact. If upstream changes keys again, another confirmed replacement is necessary.
3. **Device compatibility:** Select the ABI reported by Android’s `Build.SUPPORTED_ABIS`. A 32-bit TV userspace needs the v7a split even if the hardware supports arm64. Correct installation does not establish 4K/HDR playback quality.
4. **Background scheduling:** Android may defer jobs when the TV sleeps. The first-launch/manual check works when the TV is awake. Jobs are best-effort, not precise.
5. **Updates need consent:** Allow this updater as an unknown-app source once, then accept Android’s installation confirmation per update. Changed-key replacements also require the data-loss warning and Android’s removal confirmation. Background checks download and prepare; open the updater to install.
6. **Split handling:** Only known ABI, density and locale splits are selected. Unknown mandatory splits cause a safe rejection; the upstream bundle format must be tested with a real release.
7. **Split signatures:** The base APK signer is verified against the installed app. Configuration APK signatures are checked when Android's archive parser exposes them. Android's package installer enforces package-wide signing consistency on commit.
8. **Trust:** Release URLs are restricted to `Alexvbp/f1tv-4k-patch` over HTTPS. A changed signing key is authenticated by the release asset’s SHA-256, not by the old F1 TV key. Only continue if you trust that upstream repository.
9. **Resource consumption:** The default job checks daily on an unmetered connection and downloads when needed; a release download may temporarily consume significant storage and bandwidth.
10. **Prototype limitations:** No notification, pause/resume downloader, ETag cache, automatic rollback, update-history screen or robust battery/storage profiling yet.

## Build APK with GitHub Actions (does not require Android Studio)

1. Create a repository under **your own GitHub account**, and upload the **contents** of this project as the repository root (including `.github/workflows/build.yml`).
2. Enable **Actions** for that repository if necessary.
3. Open **Actions → Build Android TV APK → Run workflow**. On `main`, the workflow also runs when pushed.
4. Once it succeeds, open the workflow run → **Artifacts** → `f1tv-patch-updater-debug-apk`.
5. Unzip the artifact; `app-debug.apk` is the installable TV prototype.
6. Sideload the updater **once** onto the Sony X9000H using your normal TV transfer/installer method.
7. From then on, the updater should perform release checking and downloading directly on the TV, subject to device validation and signing compatibility.

The CI debug APK uses a temporary Android debug signing key. Different CI builds are **not guaranteed to update the updater app in place**. To distribute durable updates, configure a persistent release signing key on a secure machine or through CI secrets. **Never put a private keystore/password in a public repository.**

### Local build with Android Studio

- Install JDK 17, Android SDK API 35 and Build Tools 35.
- Open project in Android Studio, allow Gradle sync.
- Run `gradle --no-daemon assembleDebug testDebugUnitTest lintDebug`, or use Android Studio’s Build APK action.
- Expected file after successful build: `app/build/outputs/apk/debug/app-debug.apk`.

The workflow uses Android Gradle Plugin 8.7.3 and Gradle 8.9.

## Minimum device test plan

1. Record firmware level, `Build.SUPPORTED_ABIS` and `com.formulaone.production` version/signing certificate.
2. Launch updater; confirm leanback remote navigation works.
3. Confirm stable-only latest-release response and APKM filename.
4. Verify downloaded APKM contains the expected `base.apk`, ABI, locale and density splits.
5. Confirm wrong packages, malformed digests, modified cached bundles, downgrades, and unpinned recovery are refused. A different signing identity must require the explicit replacement flow.
6. Confirm the user installer prompt is displayed and cancellation leaves the working app intact.
7. With a same-key release, confirm upgrade and verify F1 TV login survives. For a changed-key release, cancel removal first and verify the old app remains, then explicitly authorize replacement and confirm fresh login is required. Interrupt/reopen between removal and installation to check cached recovery.
8. Reboot and test daily background checks and HDMI responsiveness.
9. Validate HD/4K and HLG playback separately.

## Attribution and licence considerations

The upstream `f1tv-4k-patch` repository is maintained separately. This source project is independent and is not endorsed by Formula 1, F1 TV or the upstream author. Review upstream and service terms before redistribution.
