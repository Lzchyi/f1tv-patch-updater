# F1TV Patch Updater (Android TV prototype)

An independent Android TV updater for the **already-installed, patched** F1 TV app from [Alexvbp/f1tv-4k-patch](https://github.com/Alexvbp/f1tv-4k-patch). This project does **not** patch F1 TV, bypass DRM, or include F1 TV binaries.

**Status: source prototype. Not compiled or tested on a Sony X9000H.** The environment used to generate this project has no Android SDK/Gradle and cannot access their download servers. Do not assume the APK will build without corrections or that installation succeeds until CI and device tests pass.

## Implemented in source

- Native leanback Android TV launcher, remote-focused button UI (Java; no third-party runtime libraries).
- Android JobScheduler daily stable GitHub release check, with unmetered network constraint; an immediate check on first launch and manual checks.
- Automatic download of the upstream `f1tv-uhd-patched.apkm` bundle.
- Bounded ZIP extraction with traversal prevention and archive-size limits.
- ABI, density and language split selection for known upstream bundle formats.
- Package-name, version-code and signing-certificate checks against the installed F1 TV app.
- Multi-APK installation through `PackageInstaller` with Android user confirmation.
- Never automatically uninstall or downgrade existing F1 TV or request root/ADB.

## Critical caveats

1. **Initial installation:** The patched F1 TV app must already be installed. The updater refuses a first install because it cannot authenticate a trusted signing key on an empty device.
2. **Same signer required:** If upstream signs a later release with a different key, an in-place update is impossible and the updater blocks it. Keep F1 TV login data intact rather than uninstalling.
3. **Sony compatibility:** X9000H ABI, firmware and F1 TV UHD playback remain untested. Correct installation does not mean 4K playback works.
4. **Background scheduling:** Android may defer jobs when the TV sleeps. The first-launch/manual check works when the TV is awake. Jobs are best-effort, not precise.
5. **Updates need consent:** Installing an update requires allowing this app as an unknown-app source once, then accepting the platform's installation confirmation per update. The updater does not force foreground popups during playback.
6. **Split handling:** Only known ABI, density and locale splits are selected. Unknown mandatory splits cause a safe rejection; the upstream bundle format must be tested with a real release.
7. **Split signatures:** The base APK signer is verified against the installed app. Configuration APK signatures are checked when Android's archive parser exposes them. Android's package installer enforces package-wide signing consistency on commit.
8. **Resource consumption:** The default job checks daily on an unmetered connection and downloads when needed; a release download may temporarily consume significant storage and bandwidth.
9. **Prototype limitations:** No notification, pause/resume downloader, ETag cache, automatic rollback, update-history screen or robust battery/storage profiling yet.

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
- Build → Build APK(s).
- Expected file after successful build: `app/build/outputs/apk/debug/app-debug.apk`.

The workflow uses Android Gradle Plugin 8.7.3 and Gradle 8.9.

## Minimum device test plan

1. Record firmware level, `Build.SUPPORTED_ABIS` and `com.formulaone.production` version/signing certificate.
2. Launch updater; confirm leanback remote navigation works.
3. Confirm stable-only latest-release response and APKM filename.
4. Verify downloaded APKM contains the expected `base.apk`, ABI, locale and density splits.
5. Confirm the app refuses an APK with the wrong package or signing identity.
6. Confirm the user installer prompt is displayed and cancellation leaves the working app intact.
7. With a known compatible newer release, confirm upgrade and verify F1 TV login survives.
8. Reboot and test daily background checks and HDMI responsiveness.
9. Validate HD/4K and HLG playback separately.

## Attribution and licence considerations

The upstream `f1tv-4k-patch` repository is maintained separately. This source project is independent and is not endorsed by Formula 1, F1 TV or the upstream author. Review upstream and service terms before redistribution.
