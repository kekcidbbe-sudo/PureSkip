package com.nzsk.pureskip.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.R
import com.nzsk.pureskip.ui.apps.AppsFragment
import com.nzsk.pureskip.ui.guide.GuideFragment
import com.nzsk.pureskip.ui.home.HomeFragment
import com.nzsk.pureskip.ui.privacy.PrivacyFragment
import com.nzsk.pureskip.ui.settings.SettingsFragment

/**
 * Main entry point of the application.
 * Handles navigation between main sections and first-launch guide.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Prevent duplicate launch from system re-triggering
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && Intent.ACTION_MAIN == intent.action) {
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        setupNavigation()

        // Check if first launch
        val settingsManager = PureSkipApplication.getInstance().settingsManager
        if (settingsManager.isFirstLaunch()) {
            showGuide()
        } else {
            showHome()
        }
    }

    private fun setupNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_apps -> {
                    loadFragment(AppsFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                R.id.nav_privacy -> {
                    loadFragment(PrivacyFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun showGuide() {
        bottomNav.visibility = android.view.View.GONE
        loadFragment(GuideFragment())
    }

    fun showHome() {
        bottomNav.visibility = android.view.View.VISIBLE
        bottomNav.selectedItemId = R.id.nav_home
        loadFragment(HomeFragment())
    }

    fun navigateToApps() {
        bottomNav.selectedItemId = R.id.nav_apps
    }

    fun navigateToPrivacy() {
        bottomNav.selectedItemId = R.id.nav_privacy
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Refresh home fragment when returning from system settings
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is HomeFragment) {
            currentFragment.refreshStatus()
        }
    }
}
