package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for QQ Reading (QQ阅读).
 * Package: com.qq.reader
 */
object QqReadingRules {
    private const val PKG = "com.qq.reader"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
