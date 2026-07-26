package com.nzsk.pureskip.ui.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nzsk.pureskip.R

/**
 * Privacy center screen showing permissions, data handling, and privacy commitments.
 */
class PrivacyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_privacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Privacy center is purely informational, no dynamic content needed
        // All text is defined in string resources
    }
}
