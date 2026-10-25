package com.independent.f1tvupdater;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Public-edition self-updates. Its files, preferences and installer callbacks are separate from F1 TV. */
public final class SelfUpdater {
    private SelfUpdater() {}
    static final String INSTALL_ACTION = "com.independent.f1tvupdater.SELF_INSTALL_RESULT";
    private static final String API = "https://api.github.com/repos/Lzchyi/f1tv-patch-updater/releases/latest";
    private static final long MAX_BYTES = SelfUpdatePolicy.MAX_BYTES;
    private static final Object LOCK = new Object();

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("self_update_state_v1", Context.MODE_PRIVATE);
    }
    static boolean supported(Context c) { return SelfUpdatePolicy.COMMUNITY.equals(c.getPackageName()); }
    static boolean automaticChecksEnabled(Context c) {
        return prefs(c).getBoolean("automatic_checks", true);
    }
    static void setAutomaticChecksEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean("automatic_checks", enabled).remove("last_attempt").apply();
    }
    private static PackageInfo own(Context c) throws Exception {
        return c.getPackageManager().getPackageInfo(c.getPackageName(), Updater.signatureFlags());
    }
    static String installedName(Context c) {
        try { return own(c).versionName; }
        catch (Exception e) { return "unknown"; }
    }
    static String latestName(Context c) { return prefs(c).getString("release_tag", ""); }
    static String offerKey(Context c) { return prefs(c).getString("asset_key", ""); }
    static boolean available(Context c) {
        return supported(c) && !offerKey(c).isEmpty() &&
                SelfUpdatePolicy.newerRelease(installedName(c), latestName(c));
    }
    static boolean pending(Context c) { return prefs(c).getInt("session_id", -1) >= 0; }
    static void dismiss(Context c) { prefs(c).edit().putString("dismissed_key", offerKey(c)).apply(); }
    static boolean shouldOffer(Context c, boolean requested) {
        return available(c) && !pending(c) && (requested || (automaticChecksEnabled(c) &&
                !offerKey(c).equals(prefs(c).getString("dismissed_key", ""))));
    }
    static String state(Context c) { return prefs(c).getString("state", "Preparing updater update"); }
    static void setState(Context c, String state) { prefs(c).edit().putString("state", state).apply(); }
    static void recordError(Context c, Exception e) {
        prefs(c).edit().putString("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                .putString("state", "Updater update could not finish").apply();
    }
    private static File apk(Context c) { return new File(c.getFilesDir(), "self_update.apk"); }
    private static File partial(Context c) { return new File(c.getFilesDir(), "self_downloading.apk"); }
    private static void remove(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("Cannot remove temporary updater APK");
    }
    private static void clearCandidate(Context c) throws IOException {
        remove(apk(c));
        prefs(c).edit().remove("candidate_key").remove("candidate_version").remove("candidate_sha256").apply();
    }

    /** Also called after MY_PACKAGE_REPLACED, since replacing this process can lose its success callback. */
    public static void reconcile(Context context) throws Exception {
        Context c = context.getApplicationContext();
        synchronized (LOCK) {
            PackageInfo current = own(c);
            long installedCode = Updater.version(current);
            long expected = prefs(c).getLong("installing_version", -1);
            boolean completed = expected > 0 && installedCode >= expected;
            boolean installing = false;
            PackageInstaller installer = c.getPackageManager().getPackageInstaller();
            for (PackageInstaller.SessionInfo session : installer.getMySessions()) {
                if (!c.getPackageName().equals(session.getAppPackageName())) continue;
                if (session.isSealed() || session.isActive()) installing = true;
                else {
                    try { installer.abandonSession(session.getSessionId()); }
                    catch (IllegalStateException ignored) { /* Session just finished. */ }
                }
            }
            remove(partial(c));
            if (installing && !completed) return;
            if (prefs(c).getLong("candidate_version", -1) <= installedCode || !apk(c).isFile())
                clearCandidate(c);
            SharedPreferences.Editor reconciled = prefs(c).edit().remove("session_id").remove("installing_version");
            if (completed) reconciled.putLong("last_installed_version", installedCode);
            boolean knownSuccess = completed || prefs(c).getLong("last_installed_version", -1) == installedCode;
            reconciled.putString("state", knownSuccess ? "Updater installed successfully" : "Ready")
                    .putBoolean("show_success", completed || prefs(c).getBoolean("show_success", false)).apply();
            if (!SelfUpdatePolicy.newerRelease(current.versionName, latestName(c))) {
                prefs(c).edit().remove("asset_key").remove("release_tag").remove("asset_url")
                        .remove("asset_digest").remove("asset_size").remove("dismissed_key").apply();
            }
        }
    }

    /** Checks metadata only. Downloads and installation are exclusively initiated by Update now. */
    static void check(Context context, boolean requested) throws Exception {
        Context c = context.getApplicationContext();
        if (!supported(c)) return; // Personal builds remain local, with no published matching APK.
        synchronized (LOCK) {
            reconcile(c);
            if (!requested && !automaticChecksEnabled(c)) return;
            if (pending(c)) return;
            long now = System.currentTimeMillis(), last = prefs(c).getLong("last_attempt", 0);
            if (!requested && now >= last && now - last < 5 * 60_000L) return;
            prefs(c).edit().putLong("last_attempt", now).apply();
            JSONObject release = new JSONObject(Updater.readSmall(API, "application/vnd.github+json", 1_000_000));
            if (release.optBoolean("draft", true) || release.optBoolean("prerelease", true))
                throw new SecurityException("Updater release is not stable");
            String tag = release.getString("tag_name");
            String name = SelfUpdatePolicy.assetName(c.getPackageName(), tag);
            if (name == null) throw new SecurityException("Unsupported updater release version");
            if (!SelfUpdatePolicy.newerRelease(own(c).versionName, tag)) {
                prefs(c).edit().remove("asset_key").remove("release_tag").putString("error", "").apply();
                return;
            }
            JSONArray assets = release.getJSONArray("assets");
            JSONObject asset = null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject entry = assets.getJSONObject(i);
                if (name.equals(entry.optString("name"))) {
                    if (asset != null) throw new SecurityException("Ambiguous updater APK assets");
                    asset = entry;
                }
            }
            if (asset == null) throw new IOException("The public updater APK is missing from this release");
            String url = asset.getString("browser_download_url"), digest = asset.optString("digest", "");
            long size = asset.getLong("size");
            SelfUpdatePolicy.verifyAsset(url, name, size, digest);
            String key = release.getLong("id") + ":" + asset.getLong("id") + ":" + digest + ":" +
                    size + ":" + asset.optString("updated_at", "");
            if (!prefs(c).edit().putString("release_tag", tag).putString("asset_key", key)
                    .putString("asset_url", url).putString("asset_digest", digest.substring(7))
                    .putLong("asset_size", size).putString("error", "").commit())
                throw new IOException("Cannot save updater release information");
        }
    }

    private static PackageInfo verify(Context c, File file, String digest) throws Exception {
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_BYTES ||
                !Updater.fileDigest(file).equalsIgnoreCase(digest))
            throw new SecurityException("Updater APK SHA-256 mismatch");
        PackageInfo incoming = c.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), Updater.signatureFlags());
        if (incoming == null || incoming.applicationInfo == null ||
                incoming.applicationInfo.minSdkVersion > Build.VERSION.SDK_INT)
            throw new SecurityException("Updater APK is incompatible with this TV");
        PackageInfo current = own(c);
        SelfUpdatePolicy.verify(current.packageName, Updater.version(current), Updater.signer(current),
                incoming.packageName, Updater.version(incoming), Updater.signer(incoming));
        String tag = latestName(c);
        if (!(tag.startsWith("v") ? tag.substring(1) : tag).equals(incoming.versionName))
            throw new SecurityException("Updater APK version does not match its release");
        return incoming;
    }

    static void install(Context context, Updater.Progress progress) throws Exception {
        Context c = context.getApplicationContext();
        synchronized (LOCK) {
            reconcile(c);
            if (pending(c)) throw new IllegalStateException("An updater installation is awaiting Android confirmation");
            if (!available(c)) throw new IllegalStateException("No newer updater release is available");
            if (!c.getPackageManager().canRequestPackageInstalls())
                throw new SecurityException("Grant installation permission for this updater first");
            String key = offerKey(c), digest = prefs(c).getString("asset_digest", "");
            PackageInfo incoming;
            if (apk(c).isFile() && key.equals(prefs(c).getString("candidate_key", ""))) {
                try { incoming = verify(c, apk(c), digest); }
                catch (SecurityException e) { clearCandidate(c); throw e; }
            } else {
                setState(c, "Downloading updater " + latestName(c));
                progress.report(state(c));
                File tmp = partial(c);
                try {
                    String hash = Updater.download(prefs(c).getString("asset_url", ""), tmp,
                            prefs(c).getLong("asset_size", -1), MAX_BYTES);
                    if (!hash.equalsIgnoreCase(digest)) throw new SecurityException("Updater APK SHA-256 mismatch");
                    setState(c, "Verifying updater update");
                    progress.report(state(c));
                    incoming = verify(c, tmp, digest);
                    remove(apk(c));
                    if (!tmp.renameTo(apk(c))) throw new IOException("Cannot save verified updater APK");
                    if (!prefs(c).edit().putString("candidate_key", key).putString("candidate_sha256", digest)
                            .putLong("candidate_version", Updater.version(incoming)).commit())
                        throw new IOException("Cannot save verified updater identity");
                } finally { remove(tmp); }
            }
            // Recheck the current package/signature immediately before staging, including cached retries.
            verify(c, apk(c), digest);
            PackageInstaller installer = c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(c.getPackageName());
            if (Build.VERSION.SDK_INT >= 31)
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
            int id = installer.createSession(params);
            boolean submitted = false;
            try (PackageInstaller.Session session = installer.openSession(id)) {
                try (InputStream source = new FileInputStream(apk(c));
                     OutputStream sink = session.openWrite("base.apk", 0, apk(c).length())) {
                    byte[] buffer = new byte[65536];
                    int n;
                    while ((n = source.read(buffer)) != -1) sink.write(buffer, 0, n);
                    session.fsync(sink);
                }
                Intent callback = new Intent(c, InstallResultReceiver.class).setAction(INSTALL_ACTION);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
                PendingIntent pi = PendingIntent.getBroadcast(c, 1002, callback, flags);
                if (!prefs(c).edit().putInt("session_id", id)
                        .putLong("installing_version", Updater.version(incoming))
                        .putString("state", "Waiting for Android updater confirmation").putString("error", "").commit())
                    throw new IOException("Cannot save updater installation session");
                session.commit(pi.getIntentSender());
                submitted = true;
            } finally {
                if (!submitted) {
                    installer.abandonSession(id);
                    prefs(c).edit().remove("session_id").remove("installing_version").apply();
                }
            }
        }
    }

    static void installationResult(Context c, int status, String message) {
        if (status == PackageInstaller.STATUS_SUCCESS) {
            try { reconcile(c); }
            catch (Exception e) { recordError(c, e); }
        } else {
            prefs(c).edit().remove("session_id").remove("installing_version")
                    .putString("state", "Updater installation did not finish; download is saved")
                    .putString("error", "Code " + status + ": " + (message == null ? "Installation cancelled or rejected" : message)).apply();
        }
    }

    static void confirmationFailed(Context c, Exception error) {
        synchronized (LOCK) {
            int id = prefs(c).getInt("session_id", -1);
            if (id >= 0) {
                try { c.getPackageManager().getPackageInstaller().abandonSession(id); }
                catch (IllegalStateException ignored) { /* Already finished. */ }
            }
            prefs(c).edit().remove("session_id").remove("installing_version").apply();
            recordError(c, error);
        }
    }
}
