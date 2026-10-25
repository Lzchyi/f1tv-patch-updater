package com.independent.f1tvupdater;

/** Decisions only: removal always needs a separate, explicit user confirmation. */
final class UpdatePolicy {
    enum Action { UPDATE, REPLACE, RECOVER }

    static Action assess(long incoming, long installed, boolean compatibleSigner,
                         boolean releaseDigestVerified, boolean pinnedRecovery) {
        if (incoming <= 0) throw new SecurityException("Invalid incoming version");
        if (installed < 0) {
            if (!releaseDigestVerified || !pinnedRecovery)
                throw new SecurityException("No previously verified replacement to recover");
            return Action.RECOVER;
        }
        if (incoming <= installed) throw new SecurityException("Candidate is not an upgrade");
        if (compatibleSigner) return Action.UPDATE;
        if (!releaseDigestVerified)
            throw new SecurityException("A signing-key change requires a GitHub SHA-256 digest");
        return Action.REPLACE;
    }
}
