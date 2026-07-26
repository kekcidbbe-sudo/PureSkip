package com.nzsk.pureskip.ui.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.R
import com.nzsk.pureskip.rules.AppInfo
import com.nzsk.pureskip.rules.RuleProvider
import com.nzsk.pureskip.safety.AppSafetyPolicy
import com.nzsk.pureskip.settings.EnhancementScope
import com.nzsk.pureskip.settings.LearnedPopupRule

class AppsFragment : Fragment() {

    private enum class ListMode { ADAPTED, RECENT, ALL }

    private lateinit var rvApps: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var switchEnhancementMaster: MaterialSwitch
    private lateinit var toggleAppList: MaterialButtonToggleGroup
    private var listMode = ListMode.ADAPTED

    private val settings
        get() = PureSkipApplication.getInstance().settingsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvApps = view.findViewById(R.id.rv_apps)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        switchEnhancementMaster = view.findViewById(R.id.switch_experimental_global)
        toggleAppList = view.findViewById(R.id.toggle_app_list)

        if (settings.consumeLegacyTeachingMigrationNotice()) {
            AlertDialog.Builder(requireContext())
                .setTitle("请重新执行手动教学")
                .setMessage(
                    "1.8.0 的教学坐标受系统栏偏移影响，旧规则已为安全自动停用。" +
                        "请在目标应用中重新教学，确认新准星中心对准关闭或跳过按钮。"
                )
                .setPositiveButton("知道了", null)
                .show()
        }

        switchEnhancementMaster.isChecked = settings.isEnhancementMasterEnabled()
        switchEnhancementMaster.setOnCheckedChangeListener { _, checked ->
            if (!checked) {
                settings.setEnhancementMasterEnabled(false)
            } else {
                showEnhancementWarning {
                    settings.setEnhancementMasterEnabled(true)
                }
            }
        }

        toggleAppList.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_list_adapted -> selectMode(ListMode.ADAPTED)
                R.id.btn_list_recent -> selectMode(ListMode.RECENT)
                R.id.btn_list_all -> requestAllAppsList()
            }
        }
        toggleAppList.check(R.id.btn_list_adapted)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning from teaching or other activities
        // to reflect newly learned rules or changed settings
        refreshList()
    }

    private fun selectMode(mode: ListMode) {
        listMode = mode
        refreshList()
    }

    private fun requestAllAppsList() {
        if (settings.isAllAppsConsentGiven()) {
            selectMode(ListMode.ALL)
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.apps_all_permission_title)
            .setMessage(R.string.apps_all_permission_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                settings.setAllAppsConsentGiven(true)
                selectMode(ListMode.ALL)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                toggleAppList.check(R.id.btn_list_recent)
            }
            .show()
    }

    private fun refreshList() {
        val apps = when (listMode) {
            ListMode.ADAPTED -> loadAdaptedApps()
            ListMode.RECENT -> loadRecentApps()
            ListMode.ALL -> loadInstalledUserApps()
        }
        layoutEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        rvApps.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE
        if (apps.isNotEmpty()) {
            rvApps.layoutManager = LinearLayoutManager(requireContext())
            rvApps.adapter = AppControlAdapter(apps)
        }
    }

    private fun loadAdaptedApps(): List<AppInfo> {
        return RuleProvider.getRegisteredPackages()
            .filter { it != "*" }
            .map { createAppInfo(it) }
            .sortedBy { it.appName }
    }

    private fun loadRecentApps(): List<AppInfo> {
        return settings.getObservedApps()
            .map { createAppInfo(it.packageName, it.lastSeenAt) }
            .filter { it.packageName != requireContext().packageName }
    }

    @Suppress("DEPRECATION")
    private fun loadInstalledUserApps(): List<AppInfo> {
        val pm = requireContext().packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != requireContext().packageName }
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { createAppInfo(it.packageName) }
            .sortedBy { it.appName }
            .toList()
    }

    @Suppress("DEPRECATION")
    private fun createAppInfo(packageName: String, lastSeenAt: Long = 0L): AppInfo {
        val pm = requireContext().packageManager
        val appName = runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrElse { packageName.substringAfterLast('.') }
        return AppInfo(
            packageName = packageName,
            appName = appName,
            isEnabled = settings.isAppEnabled(packageName),
            isBlocked = settings.isAppBlocked(packageName),
            isExperimentalEnabled = settings.isAppEnhancedEnabled(packageName),
            enhancementScope = settings.getEnhancementScope(packageName),
            learnedRuleCount = settings.getLearnedRules(packageName).size,
            isSafetyRestricted = AppSafetyPolicy.isRestricted(packageName),
            ruleCount = RuleProvider.getRulesForPackage(packageName).count { it.packageName != "*" },
            lastSeenTimestamp = lastSeenAt
        )
    }

    private fun showEnhancementWarning(onConfirmed: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.apps_experimental_label)
            .setMessage(R.string.apps_experimental_warning)
            .setPositiveButton(R.string.confirm) { _, _ -> onConfirmed() }
            .setNegativeButton(R.string.cancel) { _, _ ->
                switchEnhancementMaster.setOnCheckedChangeListener(null)
                switchEnhancementMaster.isChecked = settings.isEnhancementMasterEnabled()
                reconnectMasterListener()
            }
            .show()
    }

    private fun reconnectMasterListener() {
        switchEnhancementMaster.setOnCheckedChangeListener { _, checked ->
            if (!checked) settings.setEnhancementMasterEnabled(false)
            else showEnhancementWarning { settings.setEnhancementMasterEnabled(true) }
        }
    }

    private fun startTeaching(app: AppInfo) {
        if (app.isSafetyRestricted) {
            Toast.makeText(requireContext(), R.string.apps_restricted, Toast.LENGTH_LONG).show()
            return
        }
        if (!settings.isAccessibilityServiceEnabled()) {
            Toast.makeText(requireContext(), "请先开启无障碍服务", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent == null) {
            Toast.makeText(requireContext(), "无法启动该应用，请先手动打开它", Toast.LENGTH_LONG).show()
            return
        }
        settings.setMasterEnabled(true)
        settings.setAppEnabled(app.packageName, true)
        settings.setEnhancementMasterEnabled(true)
        settings.setAppEnhancedEnabled(app.packageName, true)
        settings.setPendingTeachingPackage(app.packageName)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
        Toast.makeText(requireContext(), "弹窗出现后拖动准星到关闭按钮", Toast.LENGTH_LONG).show()
    }

    private fun showRulesDialog(app: AppInfo) {
        val rules = settings.getLearnedRules(app.packageName)
        if (rules.isEmpty()) {
            Toast.makeText(requireContext(), "还没有学习规则", Toast.LENGTH_SHORT).show()
            return
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        rules.forEach { rule -> container.addView(createRuleRow(rule)) }
        AlertDialog.Builder(requireContext())
            .setTitle("${app.appName} · 学习规则")
            .setView(container)
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener { refreshList() }
            .show()
    }

    private fun createRuleRow(rule: LearnedPopupRule): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val description = TextView(requireContext()).apply {
            text = when {
                !rule.hasCompatibleCoordinates() -> "旧坐标（请删除并重教）"
                rule.autoEligible -> "自动规则"
                else -> "需确认规则"
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val enabled = MaterialSwitch(requireContext()).apply {
            isEnabled = rule.hasCompatibleCoordinates()
            isChecked = rule.enabled && rule.hasCompatibleCoordinates()
            setOnCheckedChangeListener { _, checked ->
                settings.setLearnedRuleEnabled(rule.ruleId, checked)
            }
        }
        val delete = MaterialButton(requireContext()).apply { text = "删除" }
        delete.setOnClickListener {
            settings.deleteLearnedRule(rule.ruleId)
            (row.parent as? ViewGroup)?.removeView(row)
        }
        row.addView(description)
        row.addView(enabled)
        row.addView(delete)
        return row
    }

    inner class AppControlAdapter(
        private val apps: List<AppInfo>
    ) : RecyclerView.Adapter<AppControlAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_app_icon)
            val appName: TextView = view.findViewById(R.id.tv_app_name)
            val packageName: TextView = view.findViewById(R.id.tv_package_name)
            val ruleCount: TextView = view.findViewById(R.id.tv_skip_count)
            val enabled: MaterialSwitch = view.findViewById(R.id.switch_enabled)
            val enhancementLayout: LinearLayout = view.findViewById(R.id.layout_experimental)
            val enhanced: MaterialSwitch = view.findViewById(R.id.switch_experimental)
            val enhancedActions: LinearLayout = view.findViewById(R.id.layout_enhanced_actions)
            val learnedCount: TextView = view.findViewById(R.id.tv_learned_count)
            val scope: MaterialButton = view.findViewById(R.id.btn_scope)
            val teach: MaterialButton = view.findViewById(R.id.btn_teach)
            val manage: MaterialButton = view.findViewById(R.id.btn_manage_rules)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_control, parent, false)
            )
        }

        @Suppress("DEPRECATION")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.appName.text = app.appName
            holder.packageName.text = app.packageName
            holder.ruleCount.text = when {
                app.isSafetyRestricted -> getString(R.string.apps_restricted)
                app.ruleCount > 0 -> "${app.ruleCount} 条内置规则"
                else -> "无内置规则，可使用增强识别"
            }
            runCatching {
                holder.icon.setImageDrawable(
                    requireContext().packageManager.getApplicationIcon(app.packageName)
                )
            }.onFailure { holder.icon.setImageResource(R.mipmap.ic_launcher) }

            holder.enabled.setOnCheckedChangeListener(null)
            holder.enabled.isChecked = app.isEnabled
            holder.enabled.setOnCheckedChangeListener { _, checked ->
                settings.setAppEnabled(app.packageName, checked)
            }

            holder.enhancementLayout.visibility = View.VISIBLE
            holder.enhanced.isEnabled = !app.isSafetyRestricted
            holder.enhanced.setOnCheckedChangeListener(null)
            holder.enhanced.isChecked = app.isExperimentalEnabled
            holder.enhanced.setOnCheckedChangeListener { switch, checked ->
                if (checked && app.isSafetyRestricted) {
                    switch.isChecked = false
                    Toast.makeText(requireContext(), R.string.apps_restricted, Toast.LENGTH_LONG).show()
                    return@setOnCheckedChangeListener
                }
                if (checked) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(app.appName)
                        .setMessage(R.string.apps_experimental_warning)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            settings.setEnhancementMasterEnabled(true)
                            settings.setAppEnhancedEnabled(app.packageName, true)
                            switchEnhancementMaster.setOnCheckedChangeListener(null)
                            switchEnhancementMaster.isChecked = true
                            reconnectMasterListener()
                            holder.enhancedActions.visibility = View.VISIBLE
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            holder.enhanced.isChecked = false
                            settings.setAppEnhancedEnabled(app.packageName, false)
                        }
                        .show()
                } else {
                    settings.setAppEnhancedEnabled(app.packageName, false)
                    holder.enhancedActions.visibility = View.GONE
                }
            }

            holder.enhancedActions.visibility =
                if (app.isExperimentalEnabled && !app.isSafetyRestricted) View.VISIBLE else View.GONE
            holder.learnedCount.text = getString(R.string.apps_learned_count, app.learnedRuleCount)
            holder.scope.text = if (app.enhancementScope == EnhancementScope.FULL_TIME) {
                getString(R.string.apps_scope_full)
            } else {
                getString(R.string.apps_scope_startup)
            }
            holder.scope.setOnClickListener {
                val current = settings.getEnhancementScope(app.packageName)
                val next = if (current == EnhancementScope.FULL_TIME) {
                    EnhancementScope.STARTUP_ONLY
                } else {
                    EnhancementScope.FULL_TIME
                }
                settings.setEnhancementScope(app.packageName, next)
                holder.scope.text = if (next == EnhancementScope.FULL_TIME) {
                    getString(R.string.apps_scope_full)
                } else {
                    getString(R.string.apps_scope_startup)
                }
            }
            holder.teach.setOnClickListener { startTeaching(app) }
            holder.manage.isEnabled = app.learnedRuleCount > 0
            holder.manage.setOnClickListener { showRulesDialog(app) }
        }

        override fun getItemCount(): Int = apps.size
    }
}
