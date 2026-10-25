package com.independent.f1tvupdater;

import org.junit.Test;
import static org.junit.Assert.*;

public class SelfUpdatePolicyTest {
    private static final String PACKAGE = SelfUpdatePolicy.COMMUNITY;
    private static final String NAME = "f1tv-patch-updater-0.3.1.apk";
    private static final String URL = "https://github.com/Lzchyi/f1tv-patch-updater/releases/download/v0.3.1/" + NAME;
    private static final String DIGEST = "sha256:" + "a".repeat(64);

    @Test public void onlyThePublicEditionHasAPublishedUpdateAsset() {
        assertEquals(NAME, SelfUpdatePolicy.assetName(PACKAGE, "v0.3.1"));
        assertEquals(NAME, SelfUpdatePolicy.assetName(PACKAGE, "0.3.1"));
        assertEquals("f1tv-patch-updater-0.3.1.1.apk", SelfUpdatePolicy.assetName(PACKAGE, "v0.3.1.1"));
        assertNull(SelfUpdatePolicy.assetName("com.independent.f1tvupdater", "v0.3.1"));
        assertNull(SelfUpdatePolicy.assetName(PACKAGE, "v0.3.1-beta"));
    }
    @Test public void comparesVersionsNumericallyAndSkipsCurrentOrOlderReleases() {
        assertTrue(SelfUpdatePolicy.newerRelease("0.3.9", "v0.3.10"));
        assertTrue(SelfUpdatePolicy.newerRelease("0.3.10", "v0.4.0"));
        assertTrue(SelfUpdatePolicy.newerRelease("0.3.1", "v0.3.1.1"));
        assertTrue(SelfUpdatePolicy.newerRelease("0.3.1.1", "v0.3.1.2"));
        assertFalse(SelfUpdatePolicy.newerRelease("0.3.1.1", "v0.3.1"));
        assertFalse(SelfUpdatePolicy.newerRelease("0.3.1", "v0.3.1"));
        assertFalse(SelfUpdatePolicy.newerRelease("0.3.1", "v0.3.0"));
        assertFalse(SelfUpdatePolicy.newerRelease("0.3", "v0.3.0"));
    }
    @Test public void malformedAndOverflowingVersionsFailClosed() {
        for (String tag : new String[] {null, "", "latest", "v0.3.2-beta", "v0.3.-1", "v0.3.9999999999999999999999"}) {
            assertFalse(SelfUpdatePolicy.newerRelease("0.3.1", tag));
            assertNull(SelfUpdatePolicy.assetName(PACKAGE, tag));
        }
        assertFalse(SelfUpdatePolicy.newerRelease("unknown", "v0.3.2"));
    }
    @Test public void acceptsOnlyANewerMatchingPackageAndSigningIdentity() {
        SelfUpdatePolicy.verify(PACKAGE, 7, "known", PACKAGE, 8, "known");
    }
    @Test public void refusesAnEditionSwitchAndDifferentTargetApp() {
        assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verify(PACKAGE, 7, "known",
                "com.independent.f1tvupdater", 8, "known"));
        assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verify(PACKAGE, 7, "known",
                "com.formulaone.production", 999, "known"));
    }
    @Test public void refusesUnknownOrChangedSignersEvenForNewerPackages() {
        for (String signer : new String[] {null, "", "other"})
            assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verify(PACKAGE, 7, "known", PACKAGE, 8, signer));
        assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verify(PACKAGE, 7, null, PACKAGE, 8, "known"));
    }
    @Test public void refusesDowngradesAndSameVersionReinstallation() {
        for (long code : new long[] {-1, 0, 6, 7})
            assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verify(PACKAGE, 7, "known", PACKAGE, code, "known"));
    }
    @Test public void acceptsOnlyTheExpectedBoundedDigestPinnedReleaseAsset() {
        SelfUpdatePolicy.verifyAsset(URL, NAME, 400_000, DIGEST);
        for (String url : new String[] {null, URL.replace("https:", "http:"), URL.replace("Lzchyi", "Other"), URL + "?other", URL.replace(NAME, "other.apk")})
            assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verifyAsset(url, NAME, 400_000, DIGEST));
        for (long size : new long[] {-1, 0, SelfUpdatePolicy.MAX_BYTES + 1})
            assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verifyAsset(URL, NAME, size, DIGEST));
        for (String digest : new String[] {null, "", "sha256:bad", "md5:" + "a".repeat(32)})
            assertThrows(SecurityException.class, () -> SelfUpdatePolicy.verifyAsset(URL, NAME, 400_000, digest));
    }
}
