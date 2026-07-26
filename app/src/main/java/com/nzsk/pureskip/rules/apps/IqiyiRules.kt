package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for iQiyi (爱奇艺).
 * Package: com.qiyi.video
 */
object IqiyiRules {
    private const val PKG = "com.qiyi.video"
    fun getRules(): List<AdRule> = listOf(
        CommonSkipRules.createStartupSkipRule(PKG),
        CommonSkipRules.createCloseAdRule(PKG)
    )
}
