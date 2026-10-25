package com.independent.f1tvupdater;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Metadata is a bandwidth pre-check; downloaded APK version codes remain authoritative. */
final class ReleaseCheckPolicy {
    private static final Pattern INSTALLED = Pattern.compile("^([0-9]+(?:\\.[0-9]+)+)(?:-[A-Za-z0-9._-]+)?$");
    private static final Pattern TAG = Pattern.compile("^v?([0-9]+(?:\\.[0-9]+)+)$");
    private static final String TITLE_PREFIX = "F1TV UHD Patched - ";

    static boolean canSkipDownload(String installedName, String tag, String title,
                                   boolean cachedForThisInstallation, boolean replacedAsset) {
        // Rebuilt assets may carry a higher Android code under the same readable version.
        if (replacedAsset) return false;
        if (cachedForThisInstallation) return true;
        long[] installed = parse(installedName, INSTALLED);
        long[] latest = parse(tag, TAG);
        if (installed == null || latest == null) return false;
        int comparison = compare(installed, latest);
        if (comparison > 0) return true;
        if (comparison < 0 || title == null || !title.startsWith(TITLE_PREFIX)) return false;
        String releaseName = title.substring(TITLE_PREFIX.length());
        long[] described = parse(releaseName, INSTALLED);
        if (described == null || compare(described, latest) != 0) return false;
        // Compare the full build, not just 3.0.x: a changed SDK/build needs verification.
        return installedName.equals(releaseName + "-UHD") ||
                installedName.equals(releaseName + "-UHD-dbg");
    }

    static boolean keepCandidate(long candidateVersion, long installedVersion,
                                 boolean fileExists, boolean hasPinnedDigest) {
        return fileExists && hasPinnedDigest && candidateVersion > 0 &&
                (installedVersion < 0 || candidateVersion > installedVersion);
    }

    static boolean interruptedState(String state) {
        return state.startsWith("Checking ") || state.startsWith("Downloading ") ||
                state.startsWith("Verifying ");
    }

    private static long[] parse(String value, Pattern pattern) {
        if (value == null) return null;
        Matcher match = pattern.matcher(value);
        if (!match.matches()) return null;
        String[] parts = match.group(1).split("\\.");
        if (parts.length > 8) return null;
        long[] result = new long[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) result[i] = Long.parseLong(parts[i]);
        } catch (NumberFormatException e) { return null; }
        return result;
    }

    private static int compare(long[] a, long[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int value = Long.compare(i < a.length ? a[i] : 0, i < b.length ? b[i] : 0);
            if (value != 0) return value;
        }
        return 0;
    }
}
