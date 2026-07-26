package com.nzsk.pureskip.ui.settings

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.R

/**
 * Settings screen for managing app preferences.
 */
class SettingsFragment : Fragment() {

    private lateinit var switchMaster: MaterialSwitch
    private lateinit var switchExperimental: MaterialSwitch
    private lateinit var switchShowCount: MaterialSwitch
    private lateinit var switchDiagnostics: MaterialSwitch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = PureSkipApplication.getInstance().settingsManager

        // Master switch
        switchMaster = view.findViewById(R.id.switch_master)
        switchMaster.isChecked = settings.isMasterEnabled()
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            settings.setMasterEnabled(isChecked)
        }

        // Enhanced recognition master switch
        switchExperimental = view.findViewById(R.id.switch_experimental)
        switchExperimental.isChecked = settings.isExperimentalEnabled()
        switchExperimental.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(requireContext())
                    .setTitle("增强识别")
                    .setMessage(getString(R.string.apps_experimental_warning))
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        settings.setExperimentalEnabled(true)
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        switchExperimental.isChecked = false
                        settings.setExperimentalEnabled(false)
                        dialog.dismiss()
                    }
                    .show()
            } else {
                settings.setExperimentalEnabled(false)
            }
        }

        // Show count
        switchShowCount = view.findViewById(R.id.switch_show_count)
        switchShowCount.isChecked = settings.isShowCountEnabled()
        switchShowCount.setOnCheckedChangeListener { _, isChecked ->
            settings.setShowCountEnabled(isChecked)
        }

        switchDiagnostics = view.findViewById(R.id.switch_diagnostics)
        switchDiagnostics.isChecked = settings.isDiagnosticsEnabled()
        switchDiagnostics.setOnCheckedChangeListener { _, checked ->
            settings.setDiagnosticsEnabled(checked)
        }
        view.findViewById<View>(R.id.btn_view_diagnostics).setOnClickListener {
            val report = settings.exportDiagnostics().ifBlank { "暂无诊断记录" }
            AlertDialog.Builder(requireContext())
                .setTitle("本地诊断（不含页面文字）")
                .setMessage(report)
                .setPositiveButton("复制") { _, _ ->
                    val clipboard = requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("纯净跳过诊断", report))
                    Toast.makeText(requireContext(), "诊断已复制", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        view.findViewById<View>(R.id.btn_clear_diagnostics).setOnClickListener {
            settings.clearDiagnostics()
            Toast.makeText(requireContext(), "诊断记录已清空", Toast.LENGTH_SHORT).show()
        }

        // Clear records
        view.findViewById<View>(R.id.card_clear_records).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("清空记录")
                .setMessage(getString(R.string.settings_clear_confirm))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    settings.clearSkipCount()
                    Toast.makeText(requireContext(), "记录已清空", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Reset settings
        view.findViewById<View>(R.id.card_reset).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("恢复默认")
                .setMessage(getString(R.string.settings_reset_confirm))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    settings.resetToDefaults()
                    // Refresh UI
                    switchMaster.isChecked = true
                    switchExperimental.isChecked = false
                    switchShowCount.isChecked = true
                    switchDiagnostics.isChecked = false
                    Toast.makeText(requireContext(), "已恢复默认设置", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Open accessibility
        view.findViewById<View>(R.id.card_accessibility).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开设置", Toast.LENGTH_SHORT).show()
            }
        }

        // Version
        val tvVersion = view.findViewById<TextView>(R.id.tv_version)
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = "v${packageInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "v1.0.0"
        }
    }
}
