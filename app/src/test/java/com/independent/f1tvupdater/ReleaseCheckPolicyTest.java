package com.independent.f1tvupdater;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseCheckPolicyTest {
    private static final String INSTALLED = "3.0.50-SP168.9.0-release-R55-tv-UHD";
    private static final String TITLE = "F1TV UHD Patched - 3.0.50-SP168.9.0-release-R55-tv";

    @Test public void freshClientWithCurrentFullBuildSkipsThePayload() {
        assertTrue(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.50", TITLE, false, false));
    }
    @Test public void newerUpstreamVersionNeedsDownload() {
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.51",
                "F1TV UHD Patched - 3.0.51-SP170-release-tv", false, false));
    }
    @Test public void componentComparisonIsNumeric() {
        assertTrue(ReleaseCheckPolicy.canSkipDownload("3.0.50-SP168-tv-UHD", "v3.0.9", "", false, false));
        assertFalse(ReleaseCheckPolicy.canSkipDownload("3.0.9-SP168-tv-UHD", "v3.0.50", TITLE, false, false));
    }
    @Test public void sameVersionDifferentBuildIsVerified() {
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.50",
                "F1TV UHD Patched - 3.0.50-SP169-release-tv", false, false));
    }
    @Test public void conflictingOrUnknownMetadataFallsBackToVerification() {
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.50", "Unknown title", false, false));
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "unknown", TITLE, false, false));
        assertFalse(ReleaseCheckPolicy.canSkipDownload("unknown", "v3.0.50", TITLE, false, false));
        assertFalse(ReleaseCheckPolicy.canSkipDownload(null, "v3.0.50", TITLE, false, false));
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.49", TITLE, false, true));
    }
    @Test public void verifiedUnchangedAssetIsRememberedEvenForUnknownNames() {
        assertTrue(ReleaseCheckPolicy.canSkipDownload("unknown", "unknown", "", true, false));
    }
    @Test public void replacedAssetInvalidatesEvenAMatchingCachedResult() {
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED, "v3.0.50", TITLE, true, true));
    }
    @Test public void oversizedVersionComponentFailsConservatively() {
        assertFalse(ReleaseCheckPolicy.canSkipDownload(INSTALLED,
                "v3.0.9999999999999999999999999", TITLE, false, false));
    }
    @Test public void pendingAndRecoveryCandidatesArePreserved() {
        assertTrue(ReleaseCheckPolicy.keepCandidate(30510001, 30500001, true, true));
        assertTrue(ReleaseCheckPolicy.keepCandidate(30510001, -1, true, true));
    }
    @Test public void installedOrInvalidCandidatesAreRemoved() {
        assertFalse(ReleaseCheckPolicy.keepCandidate(30500001, 30500001, true, true));
        assertFalse(ReleaseCheckPolicy.keepCandidate(30494003, 30500001, true, true));
        assertFalse(ReleaseCheckPolicy.keepCandidate(30510001, 30500001, false, true));
        assertFalse(ReleaseCheckPolicy.keepCandidate(30510001, 30500001, true, false));
        assertFalse(ReleaseCheckPolicy.keepCandidate(-1, -1, true, true));
    }
    @Test public void staleTransferStatesAreResetButInstallerConfirmationIsNot() {
        assertTrue(ReleaseCheckPolicy.interruptedState("Downloading v3.0.50"));
        assertTrue(ReleaseCheckPolicy.interruptedState("Verifying v3.0.50"));
        assertTrue(ReleaseCheckPolicy.interruptedState("Checking GitHub"));
        assertFalse(ReleaseCheckPolicy.interruptedState("Waiting for Android installation confirmation"));
        assertFalse(ReleaseCheckPolicy.interruptedState("Ready to install"));
    }
}
