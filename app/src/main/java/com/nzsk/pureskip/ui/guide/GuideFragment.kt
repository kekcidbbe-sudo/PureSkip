package com.nzsk.pureskip.ui.guide

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.R
import com.nzsk.pureskip.ui.MainActivity

/**
 * First-launch guide that explains the app and requests accessibility service permission.
 */
class GuideFragment : Fragment() {

    private lateinit var checkboxAgree: CheckBox
    private lateinit var btnAgree: MaterialButton
    private lateinit var btnSkip: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkboxAgree = view.findViewById(R.id.cb_agree)
        btnAgree = view.findViewById(R.id.btn_agree)
        btnSkip = view.findViewById(R.id.btn_skip)

        checkboxAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
        }

        btnAgree.setOnClickListener {
            // Mark first launch as completed
            PureSkipApplication.getInstance().settingsManager.setFirstLaunchCompleted()

            // Try to open accessibility settings
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    "请找到「纯净跳过」并开启无障碍服务",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "无法打开无障碍设置，请手动前往系统设置开启",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Navigate to home
            (activity as? MainActivity)?.showHome()
        }

        btnSkip.setOnClickListener {
            // Mark first launch as completed but don't open settings
            PureSkipApplication.getInstance().settingsManager.setFirstLaunchCompleted()
            (activity as? MainActivity)?.showHome()
        }
    }
}
