package com.nzsk.pureskip.ui.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.R
import com.nzsk.pureskip.ui.MainActivity

/**
 * Home screen showing service status, skip count, and quick controls.
 */
class HomeFragment : Fragment() {

    private lateinit var tvServiceStatus: TextView
    private lateinit var tvStatusValue: TextView
    private lateinit var viewStatusIndicator: View
    private lateinit var btnOpenAccessibility: MaterialButton
    private lateinit var tvSkipCount: TextView
    private lateinit var switchPause: MaterialSwitch
    private lateinit var tvPauseTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvServiceStatus = view.findViewById(R.id.tv_service_status)
        tvStatusValue = view.findViewById(R.id.tv_status_value)
        viewStatusIndicator = view.findViewById(R.id.view_status_indicator)
        btnOpenAccessibility = view.findViewById(R.id.btn_open_accessibility)
        tvSkipCount = view.findViewById(R.id.tv_skip_count)
        switchPause = view.findViewById(R.id.switch_pause)
        tvPauseTitle = view.findViewById(R.id.tv_pause_title)

        // Open accessibility settings
        btnOpenAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开设置", Toast.LENGTH_SHORT).show()
            }
        }

        // Pause/resume switch
        switchPause.setOnCheckedChangeListener { _, isChecked ->
            val settings = PureSkipApplication.getInstance().settingsManager
            settings.setPaused(isChecked)
            updatePauseState(isChecked)
        }

        // Quick navigation buttons
        view.findViewById<MaterialButton>(R.id.btn_nav_apps).setOnClickListener {
            (activity as? MainActivity)?.navigateToApps()
        }
        view.findViewById<MaterialButton>(R.id.btn_nav_privacy).setOnClickListener {
            (activity as? MainActivity)?.navigateToPrivacy()
        }

        refreshStatus()
    }

    fun refreshStatus() {
        if (!isAdded) return

        val settings = PureSkipApplication.getInstance().settingsManager

        // Update service status
        val isRunning = settings.isServiceRunning()
        if (isRunning) {
            tvServiceStatus.text = getString(R.string.main_service_on)
            tvStatusValue.text = getString(R.string.main_service_on)
            tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_on))
            viewStatusIndicator.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.status_on)
            )
            btnOpenAccessibility.visibility = View.GONE
        } else {
            tvServiceStatus.text = getString(R.string.main_service_off)
            tvStatusValue.text = getString(R.string.main_service_off)
            tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_off))
            viewStatusIndicator.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.status_off)
            )
            btnOpenAccessibility.visibility = View.VISIBLE
        }

        // Update skip count
        val count = settings.getSkipCount()
        tvSkipCount.text = count.toString()

        // Update pause state
        switchPause.isChecked = settings.isPaused()
        updatePauseState(settings.isPaused())
    }

    private fun updatePauseState(isPaused: Boolean) {
        if (isPaused) {
            tvPauseTitle.text = getString(R.string.main_resume_all)
            tvPauseTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_paused))
        } else {
            tvPauseTitle.text = getString(R.string.main_pause_all)
            tvPauseTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
}
