# Changelog

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
