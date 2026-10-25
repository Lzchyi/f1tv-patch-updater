# Frequently asked questions

A quick guide to **F1TV Patch Updater**, an independent Android TV companion for [Alexvbp's F1 TV 4K patch](https://github.com/Alexvbp/f1tv-4k-patch).

**Start here:** [Download the updater](https://github.com/Lzchyi/f1tv-patch-updater/releases/latest) · [Installation guide](install.md) · [Report a problem](https://github.com/Lzchyi/f1tv-patch-updater/issues/new)

## Getting started

### What does this app actually do?

It checks for newer stable releases of Alexvbp's patched F1 TV app, downloads compatible installation files onto your Android TV or Google TV, and guides you through installation. From **v0.3.1**, the public edition also detects new releases of the updater itself and offers an in-place update on the TV. You no longer need to move each upstream update from a computer to your TV.

**This app is an updater, not a video player or a 4K patch.** The patch itself is maintained by [Alexvbp](https://github.com/Alexvbp/f1tv-4k-patch).

### Do I need to install F1 TV first?

**No, starting with updater v0.3.0.** On first launch, when F1 TV is missing, choose **Download** in the setup prompt. After the download finishes, choose **Install update** and follow Android's prompts. Choosing **Not now** does nothing; you can start later using **Check for updates**.

### Do I need a computer, ADB, or a separate APKM installer?

You need a way to install the updater APK **once**. The [installation guide](install.md) covers phone transfer, USB and optional ADB over Wi-Fi. After that, routine F1 TV patch checks, downloads and installations run directly on the TV. **No AntiSplit-M or separate APKM installer is required.**

ADB is not required for normal use. From public **v0.3.1**, future updater releases can also be installed directly on the TV. If you enabled debugging just for installation, you can turn it off afterward.

### Which devices are supported?

The app targets **Android TV and Google TV running Android 8.0 (API 26) or newer**. **Tested environment:** Sony BRAVIA VH1, Android 12 (API 31), 32-bit ARM (`armeabi-v7a`), with patched F1 TV 3.0.50. App detection, no unnecessary download, remote controls, installation/retry and cleanup were exercised on that TV; the owner reports working playback. [Detailed results](validation.md). Other manufacturers, Android versions and TV boxes need their own compatibility reports.

It does **not** run on Samsung Tizen or LG webOS. Compatibility with Fire TV or other Android-derived devices is not guaranteed.

### Do I need an F1 TV subscription? Will this give me 4K?

You need **your own valid F1 TV subscription**. Where offered, 4K access requires an appropriate Premium entitlement. Neither this updater nor the upstream patch supplies a subscription or guarantees UHD/HDR playback on a particular device.

See [F1 TV's subscription comparison](https://f1help.formula1.com/article/F1-TV-subscription-comparison).

## Updates and installation

### Will the updater check for new F1 TV patches automatically?

It schedules a **best-effort daily check**, normally on an unmetered connection. Android may delay it while the TV sleeps or restricts background activity. You can always use **Check for updates** to check manually.

An available patch is not silently installed: Android installation confirmation is still required.

### Does the updater update itself?

**Yes, in the public edition starting with v0.3.1.** It checks this repository's latest stable release when opened, during **Check for updates**, and through the daily background job. When a newer version is available, an **Update the updater?** popup offers **Update now** or **Later**.

**Update now** downloads the updater APK, verifies its SHA-256, package, version and signing identity, and opens Android's update confirmation. F1 TV and its sign-in stay in place. **Later** suppresses the automatic popup for that release; **Check for updates** can offer it again. Background checks do not launch installation.

Users on **public v0.3.0 or earlier** need to install v0.3.1 once before this feature is available. The private personal edition keeps its original branding and local distribution; there is no public matching personal APK for it to fetch.

### What if updating the updater is interrupted or cancelled?

A verified updater APK is kept for retry. Use **Check for updates**, choose **Update now** again, and approve Android's prompt. The app revalidates the file and signing identity before retrying. Completed and partial updater downloads are cleaned up separately from F1 TV patch files, including recovery after the updater replaces its own process.

If a self-update cannot be verified or uses a different signing key, the installed updater is kept. The updater never uninstalls itself to bypass a signing conflict.

### What's the difference between the updater APK and the upstream APKM?

The **`.apk` from this repository** installs the updater utility. The **`.apkm` from Alexvbp's repository** contains the patched F1 TV app and its splits. They are different files; do not rename one to the other. The updater processes the upstream APKM for you.

### Why does Android ask me to allow installation or confirm an update?

Android normally requires confirmation for APK installation from this type of app. If asked, enable **Install unknown apps** for F1TV Patch Updater, return to the app and choose **Install update** again. Depending on the TV, Android may also show a final install confirmation.

The updater does not bypass Android's installer security checks.

### What does “APK signing key differs” mean?

Android requires compatible signing identities for an in-place update. If the installed F1 TV app and a new patched release are signed differently, Android cannot update one directly over the other.

For the **F1 TV app only**, the updater may offer an explicit fresh-install/replacement flow after validating the release. **Read the warning carefully:** uninstalling F1 TV clears its local app data, including sign-in and settings. The updater must not remove it without your consent.

If the signing conflict concerns **F1TV Patch Updater itself**, do **not** uninstall it as a shortcut. Confirm that you downloaded the correct public edition and report the issue.

### Will updating erase my F1 TV login?

A normal in-place update with a compatible signing key should preserve app data. A confirmed **uninstall/reinstall** after a signing-key change removes F1 TV's local data, so you'll need to sign in again. Read the confirmation before proceeding.

### What if the download, installation or TV is interrupted?

The updater is designed to retain verified pending downloads for retries and recovery. Incomplete downloads and obsolete temporary files are cleaned up; successfully installed bundles are also removed. Reopen the app and use the available update action. Do not repeatedly uninstall the target application to troubleshoot.

### Why does it say I'm already up to date?

The updater compares installed and upstream build information to avoid downloading the same patch repeatedly. If you believe it is wrong, note the installed version, upstream release, status message and the time of the check when [reporting a bug](https://github.com/Lzchyi/f1tv-patch-updater/issues/new).

## Troubleshooting

### I can't find or open the APK on my TV.

On GitHub Releases, open **Assets** and download the file named like `f1tv-patch-updater-<version>.apk`, not **Source code**. Use a TV file manager to open the transferred APK and grant installation permission to the app that opens it. See the [step-by-step installation guide](install.md).

### “Not allowed to install” or nothing happens after pressing Install update.

Check **Settings → Apps → Special app access → Install unknown apps** (or your TV's equivalent). Grant permission to the **updater** when it installs a patch, or to the **file manager/transfer app** when installing the updater itself. Return and retry; look for Android's confirmation window.

### “No compatible APK ABI” or a split installation fails.

Your TV may require a different CPU architecture, density or split configuration, or the selected upstream bundle may not support it. **Don't bypass the compatibility check or install random splits.** Report your TV model, Android version, updater version, exact status message and upstream release so the problem can be reproduced.

### The app is stuck downloading, or GitHub cannot be reached.

Check the TV's network connection and internet access. GitHub availability, rate limiting, storage space and Android background restrictions can affect checks or downloads. Retry **Check for updates** later. Include the exact status message if reporting a recurring problem.

### I installed the public version over an older personal/prototype build, but it appears as a second app.

That is expected. The public/community edition has a **different Android application ID and signing identity** from the older personal edition. The public build installs separately; it does not replace your personal build or its data.

## Privacy, licensing and support

### Does this app need an account, collect viewing data or provide F1 TV content?

The updater does not provide streaming content or a subscription. Its documented workflow uses GitHub release information and Android's local package installer. Review the source code for the exact current implementation; this FAQ is **not** a blanket privacy guarantee about third-party apps, GitHub or F1 TV.

### Is this an official Formula 1 or Alexvbp application?

**No.** This is an independent hobby project, not affiliated with or endorsed by Formula 1, F1 TV or the upstream patch maintainer. The updater's own code is MIT-licensed; that licence does not cover F1 TV's branding, its software or the upstream patch. Use is subject to applicable terms and rights. See the [README's credits and disclaimers](../README.md#credits--independence).

### How can I help test another TV or report a bug?

Testing on different **physical Android TV/Google TV devices** is especially useful. Please [open a GitHub issue](https://github.com/Lzchyi/f1tv-patch-updater/issues/new) and include:

- **TV/box model** and manufacturer
- **Android TV/Google TV version** (and, if known, CPU architecture)
- **Updater version and edition** (public/community or personal)
- **Installed F1 TV version** and the upstream release being checked
- **What you tried**, **what you expected**, and **what actually happened**
- **Exact error text**, screenshots or relevant redacted logs
- Whether detection, download, Android confirmation and installation succeeded independently

**Do not post** your F1 TV password, access tokens, private network address, account details or unredacted device logs.

If it works, successful device reports are valuable too. They help establish real compatibility rather than assuming that one TV represents all Android TV devices.

---

More technical detail: [How updates work](how-it-works.md) · [Validation and known limits](validation.md) · [Changelog](../CHANGELOG.md) · [Installation guide](install.md)
