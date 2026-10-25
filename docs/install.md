# Install on your TV

Choose one method below. Install public version 0.3.1 or newer once; future F1 TV patch checks and public updater self-update checks happen on the TV.

**[Download the latest updater](https://github.com/Lzchyi/f1tv-patch-updater/releases/latest)** · [Back to the main page](../README.md)

## Before you start

- Use an **Android TV or Google TV device running Android 8.0 or newer**. This APK does not run on Samsung Tizen or LG webOS.
- **F1 TV does not need to be installed first.** The updater offers to download it when it is missing. See [Need F1 TV as well?](#need-f1-tv-as-well).
- Watching requires your own valid F1 TV subscription; 4K requires the appropriate Premium entitlement where available. This independent hobby project is intended for personal use and provides no subscription or content. [Subscription details](https://f1help.formula1.com/article/F1-TV-subscription-comparison)
- On the release page, expand **Assets** if needed. Download **`f1tv-patch-updater-<version>.apk`**. Ignore **Source code**; you do not need to build anything.

## Choose your method

| What you have | Best place to start |
| :--- | :--- |
| An Android phone and home Wi-Fi | [Phone → TV](#phone--tv) — recommended for most people |
| A USB stick and a computer | [USB stick](#usb-stick) — no debugging settings needed |
| A computer and an AI assistant that can run commands | [Wi-Fi with AI help](#wi-fi-with-ai-help) — no file transfer app needed |

Have an iPhone? [LocalSend](#using-localsend-instead) is an option if it is available and usable on your TV. Otherwise, use USB or a computer over Wi-Fi.

## Phone → TV

1. Install **Send Files to TV** on your TV and Android phone. Use your app store or the [official website](https://www.sendfilestotv.app/how-to-sideload-and-install-apk-to-tv).
2. Connect both devices to the same home Wi-Fi. Open the transfer app on both.
3. On your phone, [download the updater APK](https://github.com/Lzchyi/f1tv-patch-updater/releases/latest).
4. On the TV, choose **Receive**. On the phone, choose **Send**, select the APK and choose your TV.
5. When the transfer finishes, open the APK on the TV. If it does not open from the transfer app, use a TV file manager and look in **Downloads** or the transfer app's save folder.
6. If Android asks, allow **Install unknown apps** for the transfer app or file manager opening the APK. Return to the file and choose **Install**.
7. Open **F1TV Patch Updater** from your TV's apps list.

### Using LocalSend instead

Already have **[LocalSend](https://localsend.org/download)** on both devices? Keep it open on the TV, send the APK from your phone or computer, and accept the transfer on the TV. Open the received file with a TV file manager, then follow steps 6–7 above.

LocalSend supports Android, iPhone, Windows, Mac and Linux. Availability and remote-control usability on TVs vary. If you cannot install or operate it on your TV, choose another method above. Your phone only transfers the APK; the APK is installed on the TV.

## USB stick

1. On your computer, [download the updater APK](https://github.com/Lzchyi/f1tv-patch-updater/releases/latest).
2. Copy the APK to a USB stick, then plug the stick into your TV or Android TV box.
3. Open a **file manager** on the TV. If you do not have one, install one from your TV's app store. Find the USB drive and open the APK.
4. If prompted, allow **Install unknown apps** for that file manager. Go back and choose **Install**.
5. Open **F1TV Patch Updater** from the TV's apps list. You can remove the USB stick afterwards.

Menu names differ between TVs. The permission may be under **Settings → Apps → Special app access → Install unknown apps**, or **Security & restrictions → Unknown sources**. Allow the app opening this file. These transfer methods do not need ADB debugging. If your TV hides installation permissions until Developer options is enabled, follow the build-number steps below, then return here and leave debugging off.

## Wi-Fi with AI help

Use a desktop AI assistant with permission to run terminal commands **on your own computer**. A chat-only website can explain the steps, but cannot connect to your TV by itself.

### 1. Prepare the TV

1. Put the TV and computer on the same home Wi-Fi.
2. On the TV, open **Settings → System → About**, or **Device Preferences → About**.
3. Find **Android TV OS build** or **Build**. Press the remote's OK button on it seven times, until the TV says you are a developer.
4. Go back and open **Developer options**. Choose the connection method your TV provides:

| What your TV shows | What to do |
| :--- | :--- |
| **Wireless debugging** with a pairing-code option | Turn it on. Open **Pair device with pairing code** when your assistant is ready. It needs the pairing address, port and temporary code, then the connection address on the main Wireless debugging screen. The two ports may differ. |
| **Network debugging / ADB debugging** | Turn it on and note the TV's IP address and any displayed port. Some manufacturers put network ADB under **USB debugging** instead. |
| Only **USB debugging**, with no usable network connection | Tell your assistant the TV model. This switch alone does not guarantee Wi-Fi access. If the TV requires an initial cable connection, use the phone or USB-stick method instead. |

Pairing-based Wireless debugging is supported on Android TV 13 and newer, where available. Older TVs may offer a manufacturer-specific network mode, often using port 5555. Use the TV's displayed port when one is shown. Find its IP under **Settings → Network** or **About → Status**. [Android's ADB guide](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi) explains the connection methods.

### 2. Copy this AI prompt

Use the copy button on this block, paste it into your preferred assistant, and replace the bracketed details. Leave anything you do not know as “unsure.”

```text
Please install the latest public F1TV Patch Updater on my Android TV over my home Wi-Fi. Install the published APK; do not build the app.

My computer: [Windows / Mac / Linux]
My TV or TV box model: [model, or unsure]
TV IP address: [address from the TV]
Debugging option shown: [Wireless debugging / Network or ADB debugging / USB debugging only / unsure]

Project: https://github.com/Lzchyi/f1tv-patch-updater
Installation guide: https://github.com/Lzchyi/f1tv-patch-updater/blob/main/docs/install.md
Latest release: https://github.com/Lzchyi/f1tv-patch-updater/releases/latest

Work through these steps for me, using your local terminal access:
1. Read the current guide and release notes. Use existing ADB or get Android SDK Platform-Tools from Google's official site for my computer. I do not need Android Studio.
2. Help me connect using the debugging mode my TV actually provides. For pairing, ask for the temporary pairing details when ready, then use the connection port shown on the main Wireless debugging screen. For legacy network ADB, use the displayed port; try 5555 only if the TV supports that mode and shows no port. Ask me to accept any TV authorization prompt. Do not assume every TV supports network ADB.
3. Confirm the target device and direct every device command to that TV. Download the public updater APK and SHA256SUMS.txt from the same latest stable release in Lzchyi/f1tv-patch-updater. Verify the APK's SHA-256 and its application ID, com.independent.f1tvupdater.community. Stop on a mismatch.
4. Install or update the updater in place, preserving its data. Never uninstall an app or clear its data to work around a signing conflict; explain it and ask first. Leave F1 TV and its login untouched. If F1 TV is missing, explain the updater's Download prompt and let me choose whether to start setup.
5. Open the updater and verify the installed version and launch. Tell me about any TV prompts I need to approve. Explain how to turn debugging off when finished; the updater does not need ADB for normal use.

Use short, plain-language instructions. If you cannot execute commands on my computer, say so and guide me or suggest the phone/USB method. Do not root the TV, disable security protections, expose ADB through my router, or publish my connection details or device logs.
```

### 3. Approve the TV prompt

If the TV shows **Allow USB debugging?**, approve your computer. The wording can say “USB” even for a network connection. Keep the TV awake while the assistant finishes.

After installation, turn off the debugging options you enabled. You can also forget the paired computer or revoke debugging authorizations in Developer options. The updater uses its own internet connection for future checks.

## Need F1 TV as well?

Install the updater using any method above, then open it. If F1 TV is missing, it asks **Download F1 TV?**

1. Choose **Download** to fetch Alexvbp's latest stable patch. **Not now** leaves it alone; choose **Check for updates** when you want to start later.
2. Wait until the download is ready, then choose **Install update**. Allow installation for this updater if Android asks, return to it, and choose **Install update** again.
3. Approve Android's installation prompt, then choose **Open F1 TV** and sign in with your own account.

The updater handles the `.apkm` bundle directly, so **no AntiSplit-M or separate installer is needed**. Saved downloads stay available if you cancel or restart, and are removed after successful installation. Background checks do not start the first download without your choice.

For manual installation instead, use **[Alexvbp's setup guide](https://github.com/Alexvbp/f1tv-4k-patch#installing-on-your-android-tv)**. Its `.apkm` bundle is different from this updater's `.apk`; do not rename one to the other. Older updater releases through 0.2.3 require an existing patched F1 TV installation.

If the official F1 TV app is already installed, replacing it with the patch may require removal and will clear its login and local settings. Review the upstream instructions before proceeding.

## After installation

Open the updater once. It checks the installed F1 TV version and the latest patch. When a newer patch is ready, choose **Install update** and follow Android's prompts. Allow this updater to install apps if Android asks. Future patch checks run on the TV; you do not need to leave debugging enabled or keep a computer connected.

From **public v0.3.1**, a newer updater release also appears in an **Update the updater?** popup. Choose **Update now**, allow installation if asked and approve Android’s update prompt. **Later** leaves it alone; **Check for updates** can offer it again. No computer or transfer app is needed for those later public updater upgrades. Users on public 0.3.0 or earlier need this one-time manual upgrade first. Private personal builds continue to be installed locally.

From **v0.3.1.1**, use **Up** to select **Updater checks on/off** beside **Daily checks on**. Off stops automatic updater checks and popups while keeping manual checks and daily F1 TV patch checks available. The updater version is shown at the bottom right.

Installing through USB or a transfer app leaves your manually copied installer in its original folder. You may delete that copy after installing. The updater cleans up the patch files it downloads itself after successful installation.

## Stuck?

| What you see | Try this |
| :--- | :--- |
| Phone cannot find the TV | Keep both transfer apps open on the same home network. Guest Wi-Fi may block devices from seeing each other. |
| APK will not open | Use the TV file manager. Check you downloaded the updater's `.apk`, rather than **Source code** or the patch's `.apkm`. |
| “Not allowed to install” | Allow installation for the particular app opening the APK, then return to the file. |
| ADB says “unauthorized” | Look at the TV and accept your computer's debugging prompt. |
| ADB cannot connect or pairing fails | Recheck the address and port on the TV. Pairing and connection ports can differ; pairing codes expire. Some TVs have no Wi-Fi ADB mode. |
| An update reports a signing conflict | Stop and check the edition/source. Do not remove an existing app just to bypass the error; removal clears its data. |

Reference: [Send Files to TV instructions](https://www.sendfilestotv.app/how-to-sideload-and-install-apk-to-tv) · [LocalSend downloads](https://localsend.org/download) · [Google's Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
