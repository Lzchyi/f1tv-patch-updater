# Validation

## 0.3.0

All 25 host tests, personal/community builds and both edition lint checks passed. An isolated test target and locally served synthetic bundle on a physical Android TV exercised declined setup with no network request, digest mismatch rejection, approved download/restart reuse, changed-candidate recovery, Android cancellation/retry, and successful base-plus-two-split installation with cleanup. The final popup focus/navigation and production package display were checked separately.

The production build checked the actual upstream release against an existing F1 TV installation without downloading. Focus skipped unavailable actions, and both disclaimer lines stayed on screen. The user's actual F1 TV installation was preserved; a fresh installation of the real F1 TV bundle and playback on an empty TV were not exercised by the isolated test.

## Previous 0.2.3

Twenty host tests cover the original update policy, release/build pre-checks, changed-asset invalidation, pending/recovery retention, interrupted/obsolete file cleanup, and symlink safety. Personal and signed community release builds and lint are checked separately. Physical diagnostics use a temporary community test installation; the personal app's data and F1 TV installation are preserved.

## Previous 0.2.2

Local checks build the personal debug APK and the permanently signed community release APK, ran all five community host policy tests, and lint both editions. APK signatures and application IDs are checked separately. Physical TV checks cover launch, version/status display, one-screen bounds, focus navigation, footer visibility. Android’s install-source settings were inspected without changing permission.

CI builds the community debug edition, runs its host policy tests, and lints it. CI debug artifacts are not public release APKs; the published APK is signed with the permanent community release identity.

## What the policy tests cover

- Same-key upgrades without removal.
- SHA-256 requirements for changed-key releases.
- First installs only with explicit setup approval and a verified upstream digest.
- Recovery only with a previously verified, pinned replacement.
- Rejection of downgrades, same-version reinstalls, and invalid versions.

## Separate device checks

A successful build, download or UI check does not prove all installation/playback behavior on every TV. Keep these separate:

- Same-key installation and preservation of login.
- Cancellation of the fresh-install warning and Android removal confirmation.
- User-authorized replacement, fresh login, interruption/restart after removal, and installation retry.
- Reboot scheduling and independent 4K/HDR playback compatibility.

No simulator was launched or tested. Device-specific diagnostic records are kept outside this public repository.
