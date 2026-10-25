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
}
