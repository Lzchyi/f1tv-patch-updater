# Validation

## 0.2.2

Local checks build the personal debug APK and the permanently signed community release APK, run all five community host policy tests, and lint both editions. APK signatures and application IDs are checked separately. Physical TV checks cover launch, version/status display, one-screen bounds, focus navigation, footer visibility. Android’s install-source settings were inspected without changing permission.

CI builds the community debug edition, runs its host policy tests, and lints it. CI debug artifacts are not public release APKs; the published APK is signed with the permanent community release identity.

## What the policy tests cover

- Same-key upgrades without removal.
- SHA-256 requirements for changed-key releases.
- Refusal of arbitrary initial installations.
- Recovery only with a previously verified, pinned replacement.
- Rejection of downgrades, same-version reinstalls, and invalid versions.

## Separate device checks

A successful build, download or UI check does not prove all installation/playback behavior on every TV. Keep these separate:

- Same-key installation and preservation of login.
- Cancellation of the fresh-install warning and Android removal confirmation.
- User-authorized replacement, fresh login, interruption/restart after removal, and installation retry.
- Reboot scheduling and independent 4K/HDR playback compatibility.

No simulator was launched or tested. Device-specific diagnostic records are kept outside this public repository.
