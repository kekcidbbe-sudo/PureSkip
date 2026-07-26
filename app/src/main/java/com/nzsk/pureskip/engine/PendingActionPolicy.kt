package com.nzsk.pureskip.engine

/**
 * Rejects delayed actions when their original accessibility window is no longer active.
 */
object PendingActionPolicy {

    fun canExecute(
        expectedPackage: String,
        currentPackage: String,
        activeRootPackage: String?,
        targetPackage: String?,
        targetRefreshed: Boolean,
        targetVisible: Boolean
    ): Boolean {
        return expectedPackage.isNotBlank() &&
            currentPackage == expectedPackage &&
            activeRootPackage == expectedPackage &&
            targetPackage == expectedPackage &&
            targetRefreshed &&
            targetVisible
    }
}
