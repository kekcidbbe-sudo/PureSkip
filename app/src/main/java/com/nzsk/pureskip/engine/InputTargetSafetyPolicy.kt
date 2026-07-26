package com.nzsk.pureskip.engine

/**
 * Input controls must never be treated as advertisement close/skip candidates.
 */
object InputTargetSafetyPolicy {

    fun isUnsafeInputTarget(editable: Boolean, className: String?): Boolean {
        return editable || className.orEmpty().contains("EditText", ignoreCase = true)
    }
}
