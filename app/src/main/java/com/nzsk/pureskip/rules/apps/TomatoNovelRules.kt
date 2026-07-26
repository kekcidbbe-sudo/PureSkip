package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Tomato Novel (番茄小说).
 * Package: com.dragon.read
 */
object TomatoNovelRules {
    private const val PKG = "com.dragon.read"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
