package com.nzsk.pureskip.engine

/** Central processing gate evaluated before any recognition work is scheduled. */
object ProcessingEligibilityPolicy {

    fun shouldProcess(
        masterEnabled: Boolean,
        paused: Boolean,
        appEnabled: Boolean,
        appBlocked: Boolean,
        systemRestricted: Boolean
    ): Boolean {
        return masterEnabled && !paused && appEnabled && !appBlocked && !systemRestricted
    }
}
