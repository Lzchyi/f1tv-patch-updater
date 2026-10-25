# How updates work

1. Check the latest stable GitHub release from `Alexvbp/f1tv-4k-patch`. Compare the installed readable version and full build with release metadata before downloading. Matching builds and newer installed versions can skip the payload. Ambiguous metadata falls back to verified APK inspection.
2. Download the expected `f1tv-uhd-patched.apkm` asset over HTTPS into app-private storage. Enforce download-size limits and verify GitHub's SHA-256 when supplied.
3. Extract with traversal/duplicate checks and decompression limits. Select the TV's supported ABI, density and language splits.
4. Validate the base package, minimum Android version, version code and signing identity. Check selected split metadata when Android exposes it; Android's package installer enforces consistency for the complete session.
5. Save the verified candidate and pin its digest, version, signer and release identity. Revalidate before installation or any user-initiated removal.
6. Install through Android's `PackageInstaller` after the user chooses Install update and accepts Android's prompt.

“Already current” results are remembered against the release asset ID, digest/size/update timestamp and the installed app's code, name and last-update time. A replaced asset invalidates the old result and is inspected again. Downloaded APK version codes remain authoritative; readable names are a conservative pre-check only.

## Updating this updater

The public edition checks `Lzchyi/f1tv-patch-updater/releases/latest` on opening, manual checks and the daily job. Version metadata is checked before downloading. A newer stable release is offered through a separate popup, keeping the three F1 TV actions unchanged. Later dismisses automatic prompting for that exact asset; a manual check can offer it again.

From 0.3.1.1, the persisted **Updater checks on/off** setting gates automatic startup/daily metadata requests and automatic offers, including cached offers. Manual checks bypass that setting. F1 TV patch scheduling is independent. The first automatic F1 TV check also respects this setting for its accompanying updater check. Personal builds show **Updater: local**.

Only Update now starts the updater APK download and installation. The expected public APK filename and repository URL, a bounded size, GitHub SHA-256, exact application ID, higher Android version code, matching release version and the currently installed signing certificate must all match. A self-update never switches editions, permits a downgrade or removes the updater.

Self-update preferences, APK files and callback action are separate from the F1 TV update path. Android must approve installation. Verified files survive cancellation for retry; partial or completed files are removed. `MY_PACKAGE_REPLACED` and startup reconciliation recover cleanup when replacing our own process loses the installer callback. Personal builds remain private/local and have no public self-update feed.

## File cleanup

After confirmed installation, the downloaded bundle and extracted APKs are deleted. Startup/resume and background checks also remove interrupted `downloading.apkm` files, abandoned extraction folders, and obsolete candidates after an external installation. Valid newer candidates stay available for retry and for recovery after user-confirmed removal.

Maintenance shares the download/install lock, runs off the UI thread, and never follows symlinks during deletion. It preserves sealed/active Android installer sessions, abandons only inactive uncommitted sessions owned by this updater, and resets interrupted transfer labels when there is no live operation. Cleanup failures are retried on a later launch/check.

## First-time setup

When F1 TV and a saved candidate are absent, the activity offers Download or Not now. Automatic prompting happens once; Check for updates can offer setup again. Only Download calls the initial-download entry point. Daily jobs use the normal entry point, which returns without a network download when F1 TV is absent.

First-time setup requires the upstream release SHA-256. The downloaded candidate records the user's initial-install approval, original absence, package identity and digest. Installation revalidates that pinned bundle and checks the current installed package again. If another app installation appears meanwhile, normal update/replacement rules apply; setup consent cannot authorize a downgrade or removal.

The app installs base and selected split APKs together using Android's PackageInstaller. It does not merge or re-sign Alexvbp's APKs, and needs no external split installer. Android permission and installation prompts still apply.

## Signing-key changes

Matching signing keys use the normal update path. A recognized signing lineage can also use that path. An unrelated key requires a separate replacement, with a verified upstream release digest, an explicit data-loss warning, and Android uninstall confirmation. Background jobs never remove F1 TV.

Removal clears F1 TV's login and local settings. The verified bundle stays in the updater's storage, so the user can retry installation after cancellation or interruption. Replacement recovery requires a previously pinned, user-initiated replacement; new setup requires its own explicit download approval.

## Two editions

Both editions share the same update logic. The public/community edition has a separate Android application ID and release signing identity. Personal branding is a private local resource overlay and isn't shipped in the public APK. Updating either updater edition does not itself replace F1 TV.

## Limits

Jobs run daily on an unmetered connection, subject to Android scheduling. There is no silent installation, root access, automatic rollback, download resume, or guarantee of UHD/HDR playback. The app trusts the specified upstream repository; a matching GitHub digest verifies that downloaded bytes match its release asset, not that the upstream account can never be compromised.
