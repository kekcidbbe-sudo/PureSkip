package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Qimao Novel (七猫免费小说).
 * Package: com.kmxs.reader
 */
object QimaoNovelRules {
    private const val PKG = "com.kmxs.reader"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
