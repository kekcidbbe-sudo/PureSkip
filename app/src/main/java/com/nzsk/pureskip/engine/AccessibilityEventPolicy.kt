package com.nzsk.pureskip.engine

import android.view.accessibility.AccessibilityEvent

/** Filters high-frequency events that do not justify a new recognition scan. */
object AccessibilityEventPolicy {

    fun shouldScheduleInteraction(eventType: Int): Boolean {
        return eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
    }
}
