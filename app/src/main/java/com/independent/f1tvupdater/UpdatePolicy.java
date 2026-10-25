package com.independent.f1tvupdater;

/** Decisions only: removal always needs a separate, explicit user confirmation. */
final class UpdatePolicy {
    enum Action { INSTALL, UPDATE, REPLACE, RECOVER }

    static Action assess(long incoming, long installed, boolean compatibleSigner,
                         boolean releaseDigestVerified, boolean pinnedRecovery) {
        return assess(incoming, installed, compatibleSigner, releaseDigestVerified, pinnedRecovery, false);
    }

    static Action assess(long incoming, long installed, boolean compatibleSigner,
                         boolean releaseDigestVerified, boolean pinnedRecovery, boolean initialSetup) {
        if (incoming <= 0) throw new SecurityException("Invalid incoming version");
        if (installed < 0) {
            if (!releaseDigestVerified)
                throw new SecurityException("Initial installation requires a verified release SHA-256");
            if (pinnedRecovery) return Action.RECOVER;
            if (initialSetup) return Action.INSTALL;
            throw new SecurityException("Confirm the first F1 TV download in the updater");
        }
        if (incoming <= installed) throw new SecurityException("Candidate is not an upgrade");
        if (compatibleSigner) return Action.UPDATE;
        if (!releaseDigestVerified)
            throw new SecurityException("A signing-key change requires a GitHub SHA-256 digest");
        return Action.REPLACE;
    }
}
