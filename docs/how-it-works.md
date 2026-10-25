# How updates work

1. Check the latest stable GitHub release from `Alexvbp/f1tv-4k-patch`.
2. Download the expected `f1tv-uhd-patched.apkm` asset over HTTPS into app-private storage. Enforce download-size limits and verify GitHub's SHA-256 when supplied.
3. Extract with traversal/duplicate checks and decompression limits. Select the TV's supported ABI, density and language splits.
4. Validate the base package, minimum Android version, version code and signing identity. Check selected split metadata when Android exposes it; Android's package installer enforces consistency for the complete session.
5. Save the verified candidate and pin its digest, version, signer and release identity. Revalidate before installation or any user-initiated removal.
6. Install through Android's `PackageInstaller` after the user chooses Install update and accepts Android's prompt.

## Signing-key changes

Matching signing keys use the normal update path. A recognized signing lineage can also use that path. An unrelated key requires a separate replacement, with a verified upstream release digest, an explicit data-loss warning, and Android uninstall confirmation. Background jobs never remove F1 TV.

Removal clears F1 TV's login and local settings. The verified bundle stays in the updater's storage, so the user can retry installation after cancellation or interruption. An empty device cannot use this as an arbitrary first-install tool: recovery requires a previously pinned, user-initiated replacement.

## Two editions

Both editions share the same update logic. The public/community edition has a separate Android application ID and release signing identity. Personal branding is a private local resource overlay and isn't shipped in the public APK. Updating either updater edition does not itself replace F1 TV.

## Limits

Jobs run daily on an unmetered connection, subject to Android scheduling. There is no silent installation, root access, automatic rollback, download resume, or guarantee of UHD/HDR playback. The app trusts the specified upstream repository; a matching GitHub digest verifies that downloaded bytes match its release asset, not that the upstream account can never be compromised.
