package com.independent.f1tvupdater;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class UpdatePolicyTest {
    @Test public void sameSignerUpdatesWithoutRemoval() {
        assertEquals(UpdatePolicy.Action.UPDATE,
                UpdatePolicy.assess(30500001, 30494003, true, false, false));
    }
    @Test public void changedSignerRequiresVerifiedReleaseDigest() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, 30494003, false, false, false));
        assertEquals(UpdatePolicy.Action.REPLACE,
                UpdatePolicy.assess(30500001, 30494003, false, true, false));
    }
    @Test public void missingAppOnlyAllowsPinnedRecovery() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, -1, false, true, false));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, -1, false, false, true));
        assertEquals(UpdatePolicy.Action.RECOVER,
                UpdatePolicy.assess(30500001, -1, false, true, true));
    }
    @Test public void matchingSignerCannotAuthorizeDowngradeOrReinstall() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30494003, 30494003, true, true, false));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30494002, 30494003, true, true, false));
    }
    @Test public void invalidVersionCannotAuthorizeRecovery() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(0, -1, false, true, true));
    }
    @Test public void explicitSetupAllowsOnlyADigestVerifiedFirstInstall() {
        assertEquals(UpdatePolicy.Action.INSTALL,
                UpdatePolicy.assess(30500001, -1, false, true, false, true));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, -1, false, false, false, true));
    }
    @Test public void backgroundChecksCannotAuthorizeAFirstInstall() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, -1, true, true, false, false));
    }
    @Test public void setupConsentDoesNotBypassExistingInstallationPolicy() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500001, 30500001, true, true, false, true));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500000, 30500001, true, true, false, true));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(30500002, 30500001, false, false, false, true));
        assertEquals(UpdatePolicy.Action.REPLACE,
                UpdatePolicy.assess(30500002, 30500001, false, true, false, true));
        assertEquals(UpdatePolicy.Action.UPDATE,
                UpdatePolicy.assess(30500002, 30500001, true, true, false, true));
    }
    @Test public void pinnedReplacementRecoveryRemainsDistinctFromSetup() {
        assertEquals(UpdatePolicy.Action.RECOVER,
                UpdatePolicy.assess(30500001, -1, false, true, true, true));
    }
    @Test public void setupConsentCannotAuthorizeAnInvalidVersion() {
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(0, -1, false, true, false, true));
        assertThrows(SecurityException.class,
                () -> UpdatePolicy.assess(-1, -1, false, true, false, true));
    }
}
