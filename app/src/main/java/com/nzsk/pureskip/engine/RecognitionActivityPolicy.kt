package com.nzsk.pureskip.engine

/** Limits continuously changing pages to the user-selected enhanced mode. */
object RecognitionActivityPolicy {

    fun shouldHandleContentChange(
        withinStartupWindow: Boolean,
        enhancedRecognitionActive: Boolean
    ): Boolean {
        return withinStartupWindow || enhancedRecognitionActive
    }
}
