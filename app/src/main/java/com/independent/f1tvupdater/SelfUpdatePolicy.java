package com.independent.f1tvupdater;

/** The updater may upgrade itself, never change edition, signing identity or remove itself. */
final class SelfUpdatePolicy {
    static final String COMMUNITY = "com.independent.f1tvupdater.community";
    static final long MAX_BYTES = 32L * 1024 * 1024;
    private static final String DOWNLOAD_PREFIX = "https://github.com/Lzchyi/f1tv-patch-updater/releases/download/";

    static String assetName(String packageName, String tag) {
        if (!COMMUNITY.equals(packageName) || components(tag) == null) return null;
        return "f1tv-patch-updater-" + (tag.startsWith("v") ? tag.substring(1) : tag) + ".apk";
    }

    static boolean newerRelease(String installed, String tag) {
        long[] current = components(installed), latest = components(tag);
        if (current == null || latest == null) return false;
        for (int i = 0; i < Math.max(current.length, latest.length); i++) {
            long a = i < current.length ? current[i] : 0;
            long b = i < latest.length ? latest[i] : 0;
            if (a != b) return b > a;
        }
        return false;
    }

    static void verify(String installedPackage, long installedCode, String installedSigner,
                       String incomingPackage, long incomingCode, String incomingSigner) {
        if (installedPackage == null || !installedPackage.equals(incomingPackage))
            throw new SecurityException("Updater APK belongs to a different app or edition");
        if (incomingCode <= installedCode)
            throw new SecurityException("Updater APK is not a newer version");
        if (installedSigner == null || installedSigner.isEmpty() || !installedSigner.equals(incomingSigner))
            throw new SecurityException("Updater signing key differs; the existing updater will be kept");
    }

    static void verifyAsset(String url, String name, long size, String digest) {
        if (url == null || name == null || !url.startsWith(DOWNLOAD_PREFIX) || !url.endsWith("/" + name) ||
                size <= 0 || size > MAX_BYTES || digest == null || !digest.matches("(?i)sha256:[0-9a-f]{64}"))
            throw new SecurityException("Invalid updater APK URL, size or SHA-256");
    }

    private static long[] components(String value) {
        if (value == null || !value.matches("v?[0-9]+(?:\\.[0-9]+){1,3}")) return null;
        String[] parts = (value.startsWith("v") ? value.substring(1) : value).split("\\.");
        long[] result = new long[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) result[i] = Long.parseLong(parts[i]);
        } catch (NumberFormatException e) { return null; }
        return result;
    }
}
