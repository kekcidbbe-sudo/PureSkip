package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for iReader (掌阅).
 * Package: com.ireader.scodl
 */
object IreaderRules {
    private const val PKG = "com.ireader.scodl"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
