package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Qidian Reading (起点读书).
 * Package: com.qidian.QDReader
 */
object QidianRules {
    private const val PKG = "com.qidian.QDReader"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
