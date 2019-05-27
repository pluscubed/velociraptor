package com.pluscubed.velociraptor.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.snackbar.Snackbar
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.detection.AppDetectionService
import com.pluscubed.velociraptor.utils.Utils

class PermissionsFragment : Fragment() {

    //Permissions
    @BindView(R.id.card_m_permissions)
    lateinit var mPermCard: View
    @BindView(R.id.button_floating_enabled)
    lateinit var enableFloatingButton: Button
    @BindView(R.id.image_floating_enabled)
    lateinit var enabledFloatingImage: ImageView
    @BindView(R.id.button_location_enabled)
    lateinit var enableLocationButton: Button
    @BindView(R.id.image_location_enabled)
    lateinit var enabledLocationImage: ImageView
    @BindView(R.id.button_enable_service)
    lateinit var enableServiceButton: Button
    @BindView(R.id.image_service_enabled)
    lateinit var enabledServiceImage: ImageView
    @BindView(R.id.button_troubleshoot)
    lateinit var troubleshootButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermCard.visibility = View.GONE
        }

        troubleshootButton.setOnClickListener {
            Utils.openLink(activity, view, SettingsActivity.TROUBLESHOOT_URL)
        }

        enableServiceButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    enableServiceButton,
                    R.string.open_settings_failed_accessibility,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        enableFloatingButton.setOnClickListener { v ->
            try {
                //Open the current default browswer App Info page
                openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID)
            } catch (ignored: ActivityNotFoundException) {
                Snackbar.make(
                    enableFloatingButton,
                    R.string.open_settings_failed_overlay,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        enableLocationButton.setOnClickListener { v ->
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateStates()
    }

    private fun invalidateStates() {
        val permissionGranted = Utils.isLocationPermissionGranted(activity)
        enabledLocationImage.setImageResource(if (permissionGranted) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableLocationButton.isEnabled = !permissionGranted

        val overlayEnabled =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity)
        enabledFloatingImage.setImageResource(if (overlayEnabled) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableFloatingButton.isEnabled = !overlayEnabled

        val serviceEnabled =
            Utils.isAccessibilityServiceEnabled(activity, AppDetectionService::class.java)
        enabledServiceImage.setImageResource(if (serviceEnabled) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableServiceButton.isEnabled = !serviceEnabled
    }

    private fun openSettings(settingsAction: String, packageName: String) {
        val intent = Intent(settingsAction)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_LOCATION = 105
    }
}