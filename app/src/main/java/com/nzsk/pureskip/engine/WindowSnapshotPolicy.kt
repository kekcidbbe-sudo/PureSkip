package com.nzsk.pureskip.engine

/** Avoids the extra full-tree fingerprint pass when no consumer needs it. */
object WindowSnapshotPolicy {

    fun requiresSnapshot(
        actionNeedsVerification: Boolean,
        hasLearnedRules: Boolean,
        diagnosticsEnabled: Boolean
    ): Boolean {
        return actionNeedsVerification || hasLearnedRules || diagnosticsEnabled
    }
}
