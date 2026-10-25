# Changelog

## 0.3.1 — 2026-10-25

### Added
- Public-edition self-update checks against this repository's stable GitHub releases, on opening, manual checks and the daily job.
- A separate Update the updater? popup with Update now and Later, retaining the existing three F1 TV controls.
- Verified in-place updater installation with a bounded download, pinned SHA-256, matching package/signing identity, newer Android code and Android confirmation.
- Independent self-update staging and preferences, retained cancellation/retry, and cleanup after replacing the updater's own process.

### Documentation
- Updated the contributor-added FAQ, installation guide and README for both on-TV update paths and the one-time upgrade from earlier updater releases.
- Added the verified Sony BRAVIA VH1 / Android 12 / 32-bit ARM environment and explicit test results, distinguishing owner-reported playback from other-device or 4K/HDR claims.
- Kept the original-logo personal build local; only the public APK is distributed through public releases.

## 0.3.0 — 2026-10-25

### Added
- First-time setup through a simple “Download F1 TV?” prompt, retaining Check for updates, Install update and Open F1 TV.
- Direct installation of Alexvbp's APKM bundle, with no AntiSplit-M or separate installer required.
- Explicit approval for the initial download, mandatory GitHub SHA-256 verification and approval pinned to the saved candidate for retries.
- A short personal-use and valid-subscription notice inside the app, plus subscription requirements and upstream terms in the public documentation.

### Behaviour
- Not now dismisses automatic setup; Check for updates can offer it again.
- Saved setup/recovery downloads are reused. Background checks never start a first-time download.
- Open F1 TV is unavailable while F1 TV is missing, and remote focus skips unavailable actions.
- Existing update, signing-key replacement, Android confirmation and automatic cleanup policies remain in place.

### Documentation
- Added a beginner installation guide for phone transfers, USB sticks and Wi-Fi ADB, with a copyable prompt for an AI assistant running on the user's computer.
- Added clear download choices, pairing versus legacy network debugging guidance, troubleshooting and first-time setup steps.

## 0.2.3 — 2026-10-25

### Fixed
- Check installed and upstream version/build metadata before downloading the full patch bundle.
- Remember verified “already current” results, including the release asset and installed app identity, so repeated checks do not re-download the same bundle.
- Recheck replaced release assets rather than trusting an old cached result.
- Remove interrupted downloads, abandoned extraction folders, and bundles made obsolete by an installation outside the updater.
- Reset stale “Downloading” and “Verifying” states after an interrupted process.
- Recover successful installation cleanup when a callback was missed, while preserving valid pending/recovery bundles and active Android confirmation sessions.

### Validation
- Regression coverage for version decisions, candidate retention, narrowly scoped file cleanup and symbolic-link safety.

## 0.2.2 — 2026-10-25

The first packaged public release.

### Added
- Public/community edition with its own app identity and permanent release signing key.
- Separate personal edition, retaining the existing TV app identity and local branding.
- A short non-affiliation notice inside the app.
- Download-first README, real TV screenshots, MIT licence for updater code, and upstream credits.
- SHA-256 checksum alongside the public APK.

### Changed
- Public releases use the community branding; personal assets are excluded from the public source tree and release packages.
- CI checks the community edition. Public release packaging refuses to proceed without explicit release signing.

## 0.2.1 — 2026-10-25

- Replaced the scrolling page with a single-screen TV layout.
- Added three horizontal actions with a solid red focus, thick white outline, and chevron.
- Improved D-pad navigation and skipped unavailable actions.
- Simplified status messages and the fresh-install explanation.
- Added launcher/banner branding and short readable version numbers.

## 0.2.0 — 2026-10-25

- Displayed the installed F1 TV version name instead of its internal Android version code.
- Added an explicit, user-confirmed replacement flow for unrelated upstream signing keys.
- Required the upstream SHA-256 digest for changed-key replacements and pinned cached bundles for recovery.
- Preserved downloaded candidates through cancellation and installation failure.
- Added minimum Android version checks and five host policy tests.

## 0.1.1 — 2026-10-21

- Fixed Android ABI-name normalization when choosing compatible APK splits.

## 0.1.0 — 2026-10-21

- Initial Android TV updater prototype with daily stable-release checks, APKM download, split selection, and Android-confirmed installation.
