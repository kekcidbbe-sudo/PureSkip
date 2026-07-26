package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Sohu Video (搜狐视频).
 * Package: com.sohu.sohuvideo
 */
object SohuVideoRules {
    private const val PKG = "com.sohu.sohuvideo"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
