package com.pluscubed.velociraptor.settings

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.api.cache.CacheLimitProvider
import com.pluscubed.velociraptor.limit.LimitService
import com.pluscubed.velociraptor.settings.appselection.AppSelectionActivity
import com.pluscubed.velociraptor.utils.NotificationUtils
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils
import kotlinx.coroutines.*

class AdvancedFragment : Fragment() {

    //Advanced
    @BindView(R.id.switch_debugging)
    lateinit var debuggingSwitch: SwitchMaterial

    @BindView(R.id.linear_clear_cache)
    lateinit var clearCacheContainer: View

    @BindView(R.id.button_app_selection)
    lateinit var appSelectionButton: Button

    @BindView(R.id.linear_gmaps_navigation)
    lateinit var gmapsOnlyNavigationContainer: ViewGroup

    @BindView(R.id.switch_gmaps_navigation)
    lateinit var gmapsOnlyNavigationSwitch: SwitchMaterial

    @BindView(R.id.switch_notif_controls)
    lateinit var notifControlsContainer: View

    private lateinit var cacheLimitProvider: CacheLimitProvider

    private var notificationManager: NotificationManager? = null

    private val isNotificationAccessGranted: Boolean
        get() = context?.let {
            NotificationManagerCompat.getEnabledListenerPackages(it)
                    .contains(BuildConfig.APPLICATION_ID)
        } ?: false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cacheLimitProvider = CacheLimitProvider(context)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_advanced, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)

        notificationManager =
                activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifControlsContainer.setOnClickListener {
            val intent = Intent(context, LimitService::class.java)
            intent.putExtra(LimitService.EXTRA_NOTIF_START, true)
            val pending = PendingIntent.getService(
                    context,
                    PENDING_SERVICE, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            val intentClose = Intent(context, LimitService::class.java)
            intentClose.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true)
            val pendingClose = PendingIntent.getService(
                    context,
                    PENDING_SERVICE_CLOSE, intentClose, PendingIntent.FLAG_CANCEL_CURRENT
            )

            val settings = Intent(context, SettingsActivity::class.java)
            val settingsIntent = PendingIntent.getActivity(
                    context,
                    PENDING_SETTINGS, settings, PendingIntent.FLAG_CANCEL_CURRENT
            )

            NotificationUtils.initChannels(context)
            val builder =
                    context?.let { it1 ->
                        NotificationCompat.Builder(it1, NotificationUtils.CHANNEL_TOGGLES)
                                .setSmallIcon(R.drawable.ic_speedometer_notif)
                                .setContentTitle(getString(R.string.controls_notif_title))
                                .setContentText(getString(R.string.controls_notif_desc))
                                .addAction(0, getString(R.string.show), pending)
                                .addAction(0, getString(R.string.hide), pendingClose)
                                .setDeleteIntent(pendingClose)
                                .setContentIntent(settingsIntent)
                    }
            val notification = builder?.build()
            notificationManager?.notify(NOTIFICATION_CONTROLS, notification)
        }



        clearCacheContainer.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { cacheLimitProvider.clear() }
                    Snackbar.make(
                            clearCacheContainer,
                            getString(R.string.cache_cleared),
                            Snackbar.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Snackbar.make(clearCacheContainer, "Error: $e", Snackbar.LENGTH_SHORT).show()
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        appSelectionButton.setOnClickListener {
            startActivity(Intent(context, AppSelectionActivity::class.java))
        }




        debuggingSwitch.isChecked = PrefUtils.isDebuggingEnabled(context)
        (debuggingSwitch.parent as View).setOnClickListener { v ->
            debuggingSwitch.isChecked = !debuggingSwitch.isChecked

            PrefUtils.setDebugging(context, debuggingSwitch.isChecked)

            Utils.updateFloatingServicePrefs(context)
        }

        gmapsOnlyNavigationSwitch.isChecked = isNotificationAccessGranted &&
                PrefUtils.isGmapsOnlyInNavigation(context)
        gmapsOnlyNavigationContainer.setOnClickListener { v ->
            if (!gmapsOnlyNavigationSwitch.isEnabled) {
                return@setOnClickListener
            }

            val accessGranted = isNotificationAccessGranted
            if (accessGranted) {
                gmapsOnlyNavigationSwitch.toggle()
                PrefUtils.setGmapsOnlyInNavigation(
                        context,
                        gmapsOnlyNavigationSwitch.isChecked
                )
            } else {
                context?.let {
                    MaterialDialog(it)
                            .show {
                                message(R.string.gmaps_only_nav_notif_access)
                                positiveButton(R.string.grant) {
                                    try {
                                        val settingsAction =
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                                                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                                else
                                                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                                        val intent = Intent(settingsAction)
                                        startActivity(intent)
                                    } catch (ignored: ActivityNotFoundException) {
                                        Snackbar.make(
                                                view,
                                                R.string.open_settings_failed_notif_access,
                                                Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                }
            }
        }
    }

    companion object {
        const val PENDING_SERVICE = 4
        const val PENDING_SERVICE_CLOSE = 3
        const val PENDING_SETTINGS = 2
        const val NOTIFICATION_CONTROLS = 42
    }
}