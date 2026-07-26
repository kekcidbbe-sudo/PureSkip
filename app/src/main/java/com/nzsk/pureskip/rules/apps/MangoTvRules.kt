package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Mango TV (芒果TV).
 * Package: com.hunantv.imgo.activity
 */
object MangoTvRules {
    private const val PKG = "com.hunantv.imgo.activity"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
