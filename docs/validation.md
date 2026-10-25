# Validation

## 0.2.1 UI update

The current single-screen UI builds and passes local lint with no errors. The update/download policy is unchanged. Physical UI checks cover visible screen bounds, absence of scroll containers, and the high-contrast focused/unfocused button states. The complete replacement flow and UHD playback remain separate checks.

## Previous 0.2.0 host checks


`gradle --no-daemon assembleDebug testDebugUnitTest lintDebug` passed locally with JDK 17, Gradle 8.9 and Android API 35.

- Debug APK build and APK signing verification passed.
- Five host policy tests passed, with no failures, errors or skipped tests.
- Lint passed with no errors and six warnings (backup rules, redundant API guards, icon shape and text internationalization).
- `git diff --check` passed.
- No simulator was launched and no simulator tests were run.

The host tests cover same-key upgrades, changed-key digest requirements, refusal of arbitrary initial installs, pinned recovery, and downgrade/reinstall rejection.

## Required device checks

Physical installation and UHD playback are separate checks. Before relying on replacement in production, validate:

1. Readable installed version, device ABI selection, download verification and persisted daily scheduling.
2. Same-key update and preservation of app login.
3. Changed-key warning and cancellation without removal.
4. Explicitly authorized removal, Android confirmation, installation and fresh login.
5. Process restart between removal and installation, cached recovery, and retry after cancelled/failed installation.
6. Reboot scheduling and independent 4K/HDR playback validation.

A successful download or passing host tests do not prove the complete replacement flow works on every TV. Device-specific diagnostic records are kept outside the public repository.

## Updater signing

CI debug keys are temporary. Configure durable release signing separately before expecting updater APKs built on different machines/runs to update one another in place. Never publish private keys or passwords.
