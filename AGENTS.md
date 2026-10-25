# F1TV Patch Updater

- Native Android TV app in `app/src/main/java/com/independent/f1tvupdater`; no third-party runtime dependencies.
- `Updater` owns release downloads, bounded APKM extraction, split selection, pinned candidate metadata, and PackageInstaller sessions. `UpdatePolicy` contains host-testable update/replacement/recovery decisions.
- `MainActivity` owns user confirmation and Android uninstall requests. Background jobs must never uninstall F1 TV or initiate a signing-key replacement.
- Display readable version names; compare numeric version codes for upgrade checks.
- Keep `MainActivity` on one TV screen without scroll containers. Use the red fill, white border and chevron focus treatment; D-pad left/right skips disabled actions, and up/down stays in the button row. Keep user wording simple and retain explicit sign-in/data-loss warnings.
- The supplied bitmap is the TV banner. The adaptive launcher icon wraps that same unmodified bitmap with safe padding.
- Same-key updates preserve data. A different key requires the trusted upstream release SHA-256, a fully cached and verified candidate, an explicit data-loss warning, and Android uninstall confirmation. Only a previously pinned, user-initiated replacement may recover after F1 TV is removed.
- Keep downloaded candidates through cancellation/failure. Never permit a downgrade or accept an arbitrary initial install.
- Build with JDK 17, Gradle 8.9, and Android API 35: `gradle --no-daemon assembleDebug testDebugUnitTest lintDebug`.
- Run host tests/build/lint only. Do not launch simulators or run simulator tests. Physical TV checks use an explicitly authorized ADB connection; destructive replacement requires user consent.
- Never commit APKs, keystores, credentials, local SDK paths, or device/app data. CI debug signing is temporary; do not promise in-place updater upgrades across CI builds.
