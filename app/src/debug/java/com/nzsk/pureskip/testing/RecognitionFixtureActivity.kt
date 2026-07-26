package com.nzsk.pureskip.testing

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.nzsk.pureskip.R

class RecognitionFixtureActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        when (intent.getStringExtra(EXTRA_SCENARIO)) {
            SCENARIO_DESCRIPTION -> root.addView(
                ImageButton(this).apply { contentDescription = "关闭广告" },
                smallButtonParams(Gravity.TOP or Gravity.END)
            )
            SCENARIO_RESOURCE_ID_ONLY -> root.addView(
                ImageButton(this).apply {
                    id = R.id.iv_ad_close
                    contentDescription = null
                },
                smallButtonParams(Gravity.TOP or Gravity.END)
            )
            SCENARIO_CLICKABLE_PARENT -> {
                val parent = FrameLayout(this).apply { isClickable = true }
                parent.addView(TextView(this).apply { text = "跳过广告" })
                root.addView(parent, smallButtonParams(Gravity.TOP or Gravity.END))
            }
            SCENARIO_CLICKABLE_AD_PARENT -> {
                val advertisement = FrameLayout(this).apply { isClickable = true }
                advertisement.addView(
                    TextView(this).apply {
                        text = "×"
                        textSize = 24f
                        gravity = Gravity.CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    },
                    FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END)
                )
                root.addView(
                    advertisement,
                    FrameLayout.LayoutParams(dp(280), dp(480), Gravity.CENTER)
                )
            }
            SCENARIO_AMBIGUOUS -> {
                root.addView(
                    Button(this).apply { text = "×"; isAllCaps = false },
                    smallButtonParams(Gravity.TOP or Gravity.START)
                )
                root.addView(
                    Button(this).apply { text = "×"; isAllCaps = false },
                    smallButtonParams(Gravity.TOP or Gravity.END)
                )
            }
            SCENARIO_CUSTOM -> root.addView(
                View(this),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            else -> root.addView(
                Button(this).apply { text = "跳过广告"; isAllCaps = false },
                smallButtonParams(Gravity.TOP or Gravity.END)
            )
        }
        setContentView(root)
    }

    private fun smallButtonParams(gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(dp(72), dp(56), gravity).apply {
            setMargins(dp(12), dp(24), dp(12), 0)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val SCENARIO_TEXT = "text"
        const val SCENARIO_DESCRIPTION = "description"
        const val SCENARIO_RESOURCE_ID_ONLY = "resource_id_only"
        const val SCENARIO_CLICKABLE_PARENT = "clickable_parent"
        const val SCENARIO_CLICKABLE_AD_PARENT = "clickable_ad_parent"
        const val SCENARIO_AMBIGUOUS = "ambiguous"
        const val SCENARIO_CUSTOM = "custom"
    }
}
