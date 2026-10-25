package com.independent.f1tvupdater;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.StatFs;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Independent Android TV update client for an upstream, externally patched app.
 * Same-key updates preserve data. Different-key releases require the user to
 * confirm removal in Android before installing the verified, cached replacement.
 */
public final class Updater {
    private Updater() {}
    public static final String TARGET = "com.formulaone.production";
    private static final String API = "https://api.github.com/repos/Alexvbp/f1tv-4k-patch/releases/latest";
    private static final String ASSET = "f1tv-uhd-patched.apkm";
    private static final String PREFS = "updater_state_v1";
    private static final String FILE = "candidate.apkm";
    private static final long MAX_BUNDLE_BYTES = 700L * 1024 * 1024;
    private static final long MAX_EXTRACTED_BYTES = 1400L * 1024 * 1024;
    private static final int JOB_ID = 281021;
    private static final Object LOCK = new Object();

    public interface Progress { void report(String message); }

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public static void setState(Context c, String state) {
        prefs(c).edit().putString("state", state).apply();
    }
    public static void recordError(Context c, Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        setState(c, "Update check or installation preparation failed");
        prefs(c).edit().putString("last_error", msg).apply();
    }
    public static long installedVersion(Context c) {
        try { return version(c.getPackageManager().getPackageInfo(TARGET, 0)); }
        catch (Exception ignored) { return -1; }
    }
    public static String installedVersionName(Context c) {
        try {
            PackageInfo info = c.getPackageManager().getPackageInfo(TARGET, 0);
            return info.versionName == null || info.versionName.isEmpty()
                    ? "Unknown (build " + version(info) + ")" : info.versionName;
        } catch (PackageManager.NameNotFoundException e) { return "Not installed"; }
    }
    private static PackageInfo installedPackage(Context c) throws Exception {
        try { return c.getPackageManager().getPackageInfo(TARGET, signatureFlags()); }
        catch (PackageManager.NameNotFoundException e) { return null; }
    }
    static long version(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }
    private static File candidate(Context c) {
        return new File(c.getFilesDir(), FILE);
    }
    public static boolean ready(Context c) {
        return candidate(c).isFile() &&
                !prefs(c).getString("candidate_sha256", "").isEmpty() &&
                prefs(c).getLong("candidate_version", -1) > installedVersion(c);
    }
    private static void clearCandidateMetadata(Context c) {
        prefs(c).edit().remove("candidate_version").remove("candidate_release_id")
                .remove("candidate_asset_key")
                .remove("candidate_sha256").remove("candidate_signer")
                .remove("candidate_digest_verified").remove("candidate_replacement")
                .remove("candidate_initial_install")
                .remove("candidate_original_version").remove("candidate_original_signer")
                .remove("replacement_pending").apply();
    }

    private static boolean waitingForInstaller(String state) {
        return state.equals("Waiting for Android installation confirmation") ||
                state.equals("Awaiting installation confirmation");
    }

    /** Runs off the UI thread. The same lock protects live downloads and installation staging. */
    public static void reconcile(Context context) throws Exception {
        Context c = context.getApplicationContext();
        synchronized (LOCK) {
            boolean pending = false;
            PackageInstaller installer = c.getPackageManager().getPackageInstaller();
            for (PackageInstaller.SessionInfo session : installer.getMySessions()) {
                if (!TARGET.equals(session.getAppPackageName())) continue;
                if (session.isSealed() || session.isActive()) pending = true;
                else {
                    // An uncommitted session left by a dead process cannot finish by itself.
                    try { installer.abandonSession(session.getSessionId()); }
                    catch (IllegalStateException ignored) { /* It finished between the queries. */ }
                }
            }
            long actual = installedVersion(c);
            long cachedVersion = prefs(c).getLong("candidate_version", -1);
            boolean keep = pending || ReleaseCheckPolicy.keepCandidate(cachedVersion, actual,
                    candidate(c).isFile(), !prefs(c).getString("candidate_sha256", "").isEmpty());
            UpdateFiles.cleanup(c.getFilesDir(), c.getCacheDir(), !keep);
            prefs(c).edit().remove("cleanup_pending").apply();
            if (pending) return;
            boolean obsolete = candidate(c).isFile() || cachedVersion > 0;
            String state = prefs(c).getString("state", "");
            long installing = prefs(c).getLong("installing_version", -1);
            if (installing > 0 && actual == installing) {
                installationSucceeded(c);
                return;
            }
            if (!keep) clearCandidateMetadata(c);
            prefs(c).edit().remove("installing_release_id").remove("installing_version")
                    .remove("installing_session_id").apply();
            if (ReleaseCheckPolicy.interruptedState(state) || waitingForInstaller(state) ||
                    (!keep && obsolete)) {
                prefs(c).edit().putString("state", ready(c) ? readyState(c) : "Ready to check")
                        .putString("last_error", "").apply();
            }
        }
    }

    public static void installationSucceeded(Context c) {
        synchronized (LOCK) {
            long expected = prefs(c).getLong("installing_version", -1);
            long actual = installedVersion(c);
            if (expected < 0 && actual > 0 && prefs(c).getLong("installed_version", -1) == actual)
                return; // Startup recovery may have completed this success before the callback arrived.
            if (expected < 0 || actual != expected) {
                setState(c, "Installer reported success, but installed version differs. Verify manually.");
                return;
            }
            prefs(c).edit()
                    .putLong("installed_release_id", prefs(c).getLong("installing_release_id", -1))
                    .putLong("installed_version", actual)
                    .remove("installing_release_id").remove("installing_version")
                    .remove("installing_session_id")
                    .putString("state", "Installed successfully").putString("last_error", "").apply();
            try {
                boolean keep = ReleaseCheckPolicy.keepCandidate(prefs(c).getLong("candidate_version", -1),
                        actual, candidate(c).isFile(), !prefs(c).getString("candidate_sha256", "").isEmpty());
                UpdateFiles.cleanup(c.getFilesDir(), c.getCacheDir(), !keep);
                if (!keep) clearCandidateMetadata(c);
                prefs(c).edit().remove("cleanup_pending").apply();
            } catch (IOException e) {
                // Installation succeeded; retry file cleanup on the next launch/check.
                prefs(c).edit().putBoolean("cleanup_pending", true).apply();
                android.util.Log.w("F1TVUpdater", "Post-install cleanup will retry", e);
            }
        }
    }

    private static boolean matchesCheckedInstallation(Context c, PackageInfo installed) {
        return prefs(c).getLong("checked_version", -1) == version(installed) &&
                prefs(c).getLong("checked_update_time", -1) == installed.lastUpdateTime &&
                prefs(c).getString("checked_version_name", "").equals(
                        installed.versionName == null ? "" : installed.versionName);
    }

    private static void rememberCurrent(Context c, PackageInfo installed, long releaseId,
                                        String assetKey, String tag) throws IOException {
        if (!prefs(c).edit().putLong("checked_release_id", releaseId)
                .putString("checked_asset_key", assetKey)
                .putLong("checked_version", version(installed))
                .putLong("checked_update_time", installed.lastUpdateTime)
                .putString("checked_version_name", installed.versionName == null ? "" : installed.versionName)
                .putString("candidate_tag", tag).putString("last_error", "")
                .putString("state", "Up to date (checked release)").commit())
            throw new IOException("Cannot save update check result");
    }
    public static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        for (JobInfo job : js.getAllPendingJobs()) if (job.getId() == JOB_ID) return;
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(c, UpdateJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .setPeriodic(24L * 60 * 60 * 1000)
                .build();
        js.schedule(job);
    }

    public static void checkAndDownload(Context context, Progress progress) throws Exception {
        checkAndDownload(context, progress, false);
    }

    /** Called by the activity only after the user selects Download in the setup prompt. */
    public static void downloadInitial(Context context, Progress progress) throws Exception {
        checkAndDownload(context, progress, true);
    }

    private static void checkAndDownload(Context context, Progress progress, boolean initialSetup) throws Exception {
        Context c = context.getApplicationContext();
        synchronized (LOCK) {
            reconcile(c);
            String current = prefs(c).getString("state", "");
            if (waitingForInstaller(current)) {
                progress.report("An installation is pending; update check deferred");
                return;
            }
            if (installedVersion(c) < 0) {
                if (ready(c)) {
                    try {
                        Bundle saved = verifiedCandidate(c);
                        try { action(c, saved); }
                        finally { saved.clean(); }
                    } catch (SecurityException e) {
                        // An invalid saved setup must not trap the user in an un-installable state.
                        UpdateFiles.cleanup(c.getFilesDir(), c.getCacheDir(), true);
                        clearCandidateMetadata(c);
                        throw e;
                    }
                    prefs(c).edit().putString("last_error", "").apply();
                    setState(c, readyState(c));
                    progress.report("Your download is saved. Choose Install update to finish.");
                    return;
                }
                if (!initialSetup) {
                    setState(c, "F1 TV is not installed. Open the updater to set it up.");
                    progress.report("Choose Check for updates to download F1 TV.");
                    return;
                }
            }
            progress.report("Checking GitHub stable releases…");
            setState(c, "Checking GitHub");
            String response = readSmall(API, "application/vnd.github+json", 1_000_000);
            JSONObject release = new JSONObject(response);
            if (release.optBoolean("draft", true) || release.optBoolean("prerelease", true))
                throw new SecurityException("The latest GitHub release is not stable");
            long releaseId = release.getLong("id");
            String tag = release.optString("tag_name", "unknown");
            JSONArray assets = release.getJSONArray("assets");
            JSONObject asset = null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject value = assets.getJSONObject(i);
                if (ASSET.equals(value.optString("name"))) {
                    if (asset != null) throw new SecurityException("Ambiguous matching APKM assets");
                    asset = value;
                }
            }
            if (asset == null) throw new Exception("No f1tv-uhd-patched.apkm asset in latest stable release");
            String url = asset.getString("browser_download_url");
            if (!url.startsWith("https://github.com/Alexvbp/f1tv-4k-patch/releases/download/"))
                throw new SecurityException("Unexpected GitHub release asset URL");
            long size = asset.optLong("size", -1);
            if (size > MAX_BUNDLE_BYTES) throw new Exception("Release bundle exceeds size limit");
            String digest = asset.optString("digest", "");
            if ("null".equalsIgnoreCase(digest)) digest = "";
            if (!digest.isEmpty() && !digest.matches("(?i)sha256:[0-9a-f]{64}"))
                throw new SecurityException("Unsupported or malformed release digest");
            String assetKey = releaseId + ":" + asset.getLong("id") + ":" + size + ":" +
                    digest + ":" + asset.optString("updated_at", "");
            PackageInfo before = installedPackage(c);
            if (before == null && (!initialSetup || digest.isEmpty()))
                throw new SecurityException("Initial installation requires confirmation and a release SHA-256");
            boolean sameInstallation = before != null && matchesCheckedInstallation(c, before);
            boolean sameRelease = prefs(c).getLong("checked_release_id", -1) == releaseId;
            String checkedAsset = prefs(c).getString("checked_asset_key", "");
            boolean changedAsset = sameRelease && !checkedAsset.isEmpty() && !checkedAsset.equals(assetKey);
            if (before != null && ReleaseCheckPolicy.canSkipDownload(before.versionName, tag, release.optString("name", ""),
                    sameInstallation && sameRelease && checkedAsset.equals(assetKey), changedAsset)) {
                rememberCurrent(c, before, releaseId, assetKey, tag);
                progress.report("Current patched release is already installed");
                return;
            }
            boolean matchingCandidateAsset = assetKey.equals(prefs(c).getString("candidate_asset_key", "")) ||
                    (!digest.isEmpty() && digest.equalsIgnoreCase("sha256:" +
                            prefs(c).getString("candidate_sha256", "")));
            if (before != null && ready(c) && matchingCandidateAsset &&
                    releaseId == prefs(c).getLong("candidate_release_id", -1) &&
                    version(before) == prefs(c).getLong("candidate_original_version", -1) &&
                    prefs(c).getString("candidate_original_signer", "").equals(signer(before))) {
                prefs(c).edit().putString("candidate_asset_key", assetKey)
                        .putString("last_error", "").putString("state", readyState(c)).apply();
                progress.report("Previously downloaded update ready for installation");
                return;
            }

            File tmp = new File(c.getFilesDir(), "downloading.apkm");
            tmp.delete();
            try {
                progress.report("Downloading " + tag + "…");
                setState(c, "Downloading " + tag);
                String computed = download(url, tmp, size);
                if (!digest.isEmpty() && !computed.equalsIgnoreCase(digest.substring(7)))
                    throw new SecurityException("Release bundle SHA-256 mismatch");
                progress.report("Checking architecture, package version, and signing certificate…");
                setState(c, "Verifying " + tag);
                Bundle bundle = prepare(c, tmp);
                PackageInfo existing;
                UpdatePolicy.Action action;
                try {
                    existing = installedPackage(c);
                    long installed = existing == null ? -1 : version(existing);
                    if (bundle.version <= installed) {
                        rememberCurrent(c, existing, releaseId, assetKey, tag);
                        progress.report("No newer compatible Android app version");
                        return;
                    }
                    action = UpdatePolicy.assess(bundle.version, installed,
                            existing != null && canUpdate(bundle.base, existing), !digest.isEmpty(), false, initialSetup);
                } finally { bundle.clean(); }
                // Never remove the last successfully verified candidate until the replacement is ready.
                File completed = candidate(c);
                if (completed.exists() && !completed.delete()) throw new Exception("Cannot replace cached update");
                if (!tmp.renameTo(completed)) throw new Exception("Cannot store verified update");
                if (!prefs(c).edit()
                        .putLong("candidate_release_id", releaseId)
                        .putString("candidate_asset_key", assetKey)
                        .putLong("candidate_version", bundle.version)
                        .putString("candidate_name", bundle.base.versionName)
                        .putString("candidate_sha256", computed)
                        .putString("candidate_signer", bundle.signer)
                        .putBoolean("candidate_digest_verified", !digest.isEmpty())
                        .putBoolean("candidate_replacement", action == UpdatePolicy.Action.REPLACE)
                        .putBoolean("candidate_initial_install", action == UpdatePolicy.Action.INSTALL)
                        .putLong("candidate_original_version", existing == null ? -1 : version(existing))
                        .putString("candidate_original_signer", existing == null ? "" : signer(existing))
                        .putLong("checked_release_id", releaseId)
                        .putString("checked_asset_key", assetKey)
                        .remove("checked_version").remove("checked_update_time").remove("checked_version_name")
                        .remove("replacement_pending")
                        .putString("candidate_tag", tag)
                        .putString("last_error", "")
                        .putString("state", action == UpdatePolicy.Action.REPLACE
                                ? "Signing key changed. Replacement needs confirmation and clears F1 TV login."
                                : "Ready to install")
                        .commit()) throw new Exception("Cannot save verified update metadata");
                progress.report("Release downloaded. Open the updater and confirm installation.");
            } finally { tmp.delete(); }
        }
    }

    static String readSmall(String target, String accept, int limit) throws Exception {
        HttpURLConnection c = open(target, accept);
        try (InputStream stream = c.getInputStream()) {
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = stream.read(buf)) != -1) {
                if (bytes.size() + n > limit) throw new Exception("Oversized GitHub response");
                bytes.write(buf, 0, n);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } finally { c.disconnect(); }
    }
    private static HttpURLConnection open(String raw, String accept) throws Exception {
        URL url = new URL(raw);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new SecurityException("HTTPS required");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "F1TV-Patch-Updater/0.2 (AndroidTV)");
        if (accept != null) conn.setRequestProperty("Accept", accept);
        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            throw new Exception("GitHub HTTP error " + code);
        }
        URL finalUrl = conn.getURL();
        String host = finalUrl.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(finalUrl.getProtocol()) ||
                !(host.equals("github.com") || host.equals("api.github.com") ||
                  host.endsWith(".githubusercontent.com") ||
                  host.equals("objects.githubusercontent.com"))) {
            conn.disconnect();
            throw new SecurityException("Unexpected asset redirect host: " + host);
        }
        return conn;
    }
    private static String download(String url, File output, long advertisedSize) throws Exception {
        return download(url, output, advertisedSize, MAX_BUNDLE_BYTES);
    }
    static String download(String url, File output, long advertisedSize, long maximumBytes) throws Exception {
        long available = new StatFs(output.getParent()).getAvailableBytes();
        long minimum = (advertisedSize > 0 ? advertisedSize : 200L * 1024 * 1024) + 64L * 1024 * 1024;
        if (available < minimum) throw new Exception("Insufficient free storage for APKM download");
        HttpURLConnection c = open(url, "application/octet-stream");
        try (InputStream in = new BufferedInputStream(c.getInputStream());
             FileOutputStream out = new FileOutputStream(output)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            long received = 0;
            while ((n = in.read(buf)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Download interrupted");
                received += n;
                if (received > maximumBytes) throw new SecurityException("Download exceeds size limit");
                out.write(buf, 0, n);
                digest.update(buf, 0, n);
            }
            out.getFD().sync();
            if (advertisedSize > 0 && received != advertisedSize)
                throw new SecurityException("Truncated or oversized download");
            return hex(digest.digest());
        } finally { c.disconnect(); }
    }
    private static String hex(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return sb.toString();
    }

    private static final class Bundle {
        final File directory;
        final List<File> apks;
        final long version;
        final PackageInfo base;
        final String signer;
        Bundle(File directory, List<File> apks, PackageInfo base, String signer) {
            this.directory = directory; this.apks = apks; this.version = version(base);
            this.base = base; this.signer = signer;
        }
        void clean() { deleteTree(directory); }
    }
    private static void deleteTree(File f) {
        if (f == null) return;
        try { UpdateFiles.removeTree(f); }
        catch (IOException e) { android.util.Log.w("F1TVUpdater", "Temporary cleanup will retry", e); }
    }

    /** Zip extraction stays entirely inside app-private storage. */
    private static Bundle prepare(Context c, File archive) throws Exception {
        if (archive.length() <= 0 || archive.length() > MAX_BUNDLE_BYTES)
            throw new SecurityException("Invalid APKM size");
        File dir = new File(c.getCacheDir(), "apkm_extract_" + System.nanoTime());
        if (!dir.mkdirs()) throw new Exception("Cannot create private APK extraction directory");
        try {
            List<File> apks = new ArrayList<>();
            try (ZipFile zip = new ZipFile(archive)) {
                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                int count = 0; long total = 0;
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (++count > 512) throw new SecurityException("Too many APKM entries");
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    // This upstream release puts APK files at the archive root.
                    if (name.contains("/") || name.contains("\\") || name.contains("..") || name.startsWith("."))
                        throw new SecurityException("Unsafe or unsupported APKM entry: " + name);
                    if (!name.endsWith(".apk")) continue;
                    if (entry.getSize() > MAX_BUNDLE_BYTES) throw new SecurityException("Oversized archive member");
                    File output = new File(dir, name);
                    if (output.exists()) throw new SecurityException("Duplicate APK name: " + name);
                    long bytes = 0;
                    try (InputStream in = zip.getInputStream(entry);
                         OutputStream out = new FileOutputStream(output)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            bytes += n; total += n;
                            if (bytes > MAX_BUNDLE_BYTES || total > MAX_EXTRACTED_BYTES)
                                throw new SecurityException("APKM decompression limit exceeded");
                            out.write(buf, 0, n);
                        }
                    }
                    apks.add(output);
                }
            }
            List<File> chosen = selectSplits(c, apks);
            PackageManager pm = c.getPackageManager();
            PackageInfo base = pm.getPackageArchiveInfo(chosen.get(0).getAbsolutePath(),
                    signatureFlags());
            if (base == null || !TARGET.equals(base.packageName))
                throw new SecurityException("Bundle base is not an F1 TV APK");
            if (base.applicationInfo == null || base.applicationInfo.minSdkVersion > Build.VERSION.SDK_INT)
                throw new SecurityException("The release requires a newer Android version than this TV");
            String baseSigner = signer(base);
            if (baseSigner == null) throw new SecurityException("Cannot authenticate incoming base APK");
            // Individual configuration APK parsing varies by Android OS release. The platform
            // PackageInstaller additionally validates every split's signature as one package.
            for (int i = 1; i < chosen.size(); i++) {
                PackageInfo split = pm.getPackageArchiveInfo(chosen.get(i).getAbsolutePath(),
                        signatureFlags());
                if (split != null) {
                    if (!TARGET.equals(split.packageName) || version(split) != version(base))
                        throw new SecurityException("Inconsistent configuration APK");
                    String signature = signer(split);
                    if (signature != null && !baseSigner.equals(signature))
                        throw new SecurityException("Configuration APK signing mismatch");
                }
            }
            return new Bundle(dir, chosen, base, baseSigner);
        } catch (Exception e) { deleteTree(dir); throw e; }
    }
    static int signatureFlags() {
        return Build.VERSION.SDK_INT >= 28 ?
                PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
    }
    static String signer(PackageInfo pkg) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28 && pkg.signingInfo != null) {
            signatures = pkg.signingInfo.getApkContentsSigners();
        } else {
            signatures = pkg.signatures;
        }
        if (signatures == null || signatures.length != 1) return null;
        return hex(MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray()));
    }

    private static boolean canUpdate(PackageInfo incoming, PackageInfo existing) throws Exception {
        String installedSigner = signer(existing);
        if (installedSigner == null) throw new SecurityException("Cannot read installed F1 TV signer");
        if (installedSigner.equals(signer(incoming))) return true;
        // A valid v3 signing lineage can preserve data across a key rotation.
        if (Build.VERSION.SDK_INT >= 28 && incoming.signingInfo != null &&
                !incoming.signingInfo.hasMultipleSigners()) {
            Signature[] history = incoming.signingInfo.getSigningCertificateHistory();
            if (history != null) for (Signature cert : history) {
                String hash = hex(MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()));
                if (installedSigner.equals(hash)) return true;
            }
        }
        return false;
    }

    static String fileDigest(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        return hex(digest.digest());
    }

    public static String readyState(Context c) {
        if (installedVersion(c) < 0) return "Ready to install F1 TV";
        return prefs(c).getBoolean("candidate_replacement", false)
                ? "Signing key changed. Replacement needs confirmation and clears F1 TV login."
                : "Ready to install";
    }

    private static Bundle verifiedCandidate(Context c) throws Exception {
        if (!ready(c)) throw new SecurityException("No verified newer update ready");
        if (!fileDigest(candidate(c)).equals(prefs(c).getString("candidate_sha256", "")))
            throw new SecurityException("Cached bundle changed. Check for updates again.");
        Bundle bundle = prepare(c, candidate(c));
        if (bundle.version != prefs(c).getLong("candidate_version", -1) ||
                !bundle.signer.equals(prefs(c).getString("candidate_signer", ""))) {
            bundle.clean();
            throw new SecurityException("Cached package identity changed");
        }
        return bundle;
    }

    private static UpdatePolicy.Action action(Context c, Bundle bundle) throws Exception {
        PackageInfo existing = installedPackage(c);
        boolean recovery = prefs(c).getBoolean("replacement_pending", false) &&
                prefs(c).getLong("candidate_original_version", -1) > 0 &&
                bundle.version > prefs(c).getLong("candidate_original_version", -1) &&
                !prefs(c).getString("candidate_original_signer", "").isEmpty();
        UpdatePolicy.Action result = UpdatePolicy.assess(bundle.version,
                existing == null ? -1 : version(existing),
                existing != null && canUpdate(bundle.base, existing),
                prefs(c).getBoolean("candidate_digest_verified", false), recovery,
                prefs(c).getBoolean("candidate_initial_install", false) &&
                        prefs(c).getLong("candidate_original_version", 0) == -1 &&
                        prefs(c).getString("candidate_original_signer", "").isEmpty());
        if (result == UpdatePolicy.Action.REPLACE &&
                (version(existing) != prefs(c).getLong("candidate_original_version", -1) ||
                 !signer(existing).equals(prefs(c).getString("candidate_original_signer", ""))))
            throw new SecurityException("Installed F1 TV changed. Check for updates again.");
        return result;
    }

    public static boolean replacementRequired(Context c) throws Exception {
        synchronized (LOCK) {
            Bundle bundle = verifiedCandidate(c);
            try { return action(c, bundle) == UpdatePolicy.Action.REPLACE; }
            finally { bundle.clean(); }
        }
    }

    /** Called only after the user accepts the data-loss warning in the activity. */
    public static void beginReplacement(Context c) throws Exception {
        synchronized (LOCK) {
            Bundle bundle = verifiedCandidate(c);
            try {
                if (action(c, bundle) != UpdatePolicy.Action.REPLACE)
                    throw new SecurityException("Replacement is no longer required");
                if (!prefs(c).edit().putBoolean("replacement_pending", true)
                        .putString("state", "Confirm F1 TV removal in Android, then install the saved replacement")
                        .commit()) throw new Exception("Cannot save replacement recovery state");
            } finally { bundle.clean(); }
        }
    }

    /** Choose exactly one compatible ABI and density, and a matching/fallback locale. */
    private static List<File> selectSplits(Context c, List<File> all) throws Exception {
        File base = null;
        List<File> binaries = new ArrayList<>(), densities = new ArrayList<>(), languages = new ArrayList<>();
        List<File> extras = new ArrayList<>();
        for (File f : all) {
            String name = f.getName();
            if (name.equals("base.apk")) {
                if (base != null) throw new SecurityException("Multiple base APKs");
                base = f; continue;
            }
            int at = name.indexOf("config.");
            if (at < 0) {
                // Some bundles name the base after its package instead.
                if (base == null && name.equals(TARGET + ".apk")) { base = f; continue; }
                extras.add(f); continue;
            }
            String qualifier = name.substring(at + 7, name.length() - 4).toLowerCase(Locale.ROOT);
            if (qualifier.equals("arm64_v8a") || qualifier.equals("armeabi_v7a") ||
                    qualifier.equals("x86") || qualifier.equals("x86_64")) binaries.add(f);
            else if (Arrays.asList("ldpi","mdpi","tvdpi","hdpi","xhdpi","xxhdpi","xxxhdpi").contains(qualifier))
                densities.add(f);
            else if (qualifier.matches("[a-z]{2,3}(_r[a-z]{2})?")) languages.add(f);
            else extras.add(f);
        }
        if (base == null) throw new SecurityException("No supported base APK filename");
        if (!extras.isEmpty()) throw new SecurityException("Unknown APK splits: " + extras.get(0).getName());
        List<File> result = new ArrayList<>(); result.add(base);
        if (!binaries.isEmpty()) {
            File compatible = null;
            for (String abi : Build.SUPPORTED_ABIS) {
                // Android reports "arm64-v8a" / "armeabi-v7a" but APK splits use
                // "arm64_v8a" / "armeabi_v7a". Keep the OS preference order.
                String splitAbi = abi.toLowerCase(Locale.ROOT).replace('-', '_');
                for (File f : binaries) {
                    if (f.getName().toLowerCase(Locale.ROOT).endsWith("config." + splitAbi + ".apk")) {
                        compatible = f; break;
                    }
                }
                if (compatible != null) break;
            }
            if (compatible == null) {
                List<String> available = new ArrayList<>();
                for (File f : binaries) available.add(f.getName());
                throw new SecurityException("No compatible APK ABI. TV supports: " +
                        Arrays.toString(Build.SUPPORTED_ABIS) + "; release contains: " + available);
            }
            result.add(compatible);
        }
        if (!densities.isEmpty()) {
            Map<String,Integer> values = new HashMap<>();
            values.put("ldpi",120);values.put("mdpi",160);values.put("tvdpi",213);
            values.put("hdpi",240);values.put("xhdpi",320);values.put("xxhdpi",480);values.put("xxxhdpi",640);
            DisplayMetrics metrics = c.getResources().getDisplayMetrics();
            int actual = metrics.densityDpi;
            File match = null; int best = Integer.MAX_VALUE;
            for (File f : densities) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                for (Map.Entry<String,Integer> d : values.entrySet()) {
                    if (n.endsWith("config." + d.getKey() + ".apk")) {
                        int gap = Math.abs(actual - d.getValue());
                        if (gap < best) { best = gap; match = f; }
                    }
                }
            }
            if (match == null) throw new SecurityException("Cannot choose density split");
            result.add(match);
        }
        if (!languages.isEmpty()) {
            String locale = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
            File match = null;
            for (File f : languages) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith("config." + locale + ".apk")) { match = f; break; }
            }
            if (match == null) for (File f : languages)
                if (f.getName().toLowerCase(Locale.ROOT).endsWith("config.en.apk")) { match = f; break; }
            if (match == null) throw new SecurityException("No supported language split");
            result.add(match);
        }
        return result;
    }

    public static void install(Context context) throws Exception {
        Context c = context.getApplicationContext();
        synchronized (LOCK) {
            reconcile(c);
            if (Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls())
                throw new SecurityException("Grant Install unknown apps permission first");
            Bundle bundle = verifiedCandidate(c);
            try {
                if (action(c, bundle) == UpdatePolicy.Action.REPLACE)
                    throw new SecurityException("Confirm replacement and remove old F1 TV first");
                PackageInstaller installer = c.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(TARGET);
                if (Build.VERSION.SDK_INT >= 31)
                    params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
                int id = installer.createSession(params);
                boolean submitted = false;
                try (PackageInstaller.Session session = installer.openSession(id)) {
                    for (File apk : bundle.apks) {
                        try (InputStream source = new FileInputStream(apk);
                             OutputStream sink = session.openWrite(apk.getName(), 0, apk.length())) {
                            byte[] buf = new byte[65536];
                            int n;
                            while ((n = source.read(buf)) != -1) sink.write(buf, 0, n);
                            session.fsync(sink);
                        }
                    }
                    Intent callback = new Intent(c, InstallResultReceiver.class);
                    callback.setAction("com.independent.f1tvupdater.INSTALL_RESULT");
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
                    PendingIntent pi = PendingIntent.getBroadcast(c, 1001, callback, flags);
                    prefs(c).edit()
                            .putInt("installing_session_id", id)
                            .putLong("installing_release_id", prefs(c).getLong("candidate_release_id", -1))
                            .putLong("installing_version", bundle.version)
                            .putString("state", "Waiting for Android installation confirmation")
                            .apply();
                    session.commit(pi.getIntentSender());
                    submitted = true;
                } finally {
                    if (!submitted) {
                        installer.abandonSession(id);
                        prefs(c).edit().remove("installing_release_id").remove("installing_version")
                                .remove("installing_session_id").apply();
                    }
                }
            } finally { bundle.clean(); }
        }
    }
}
