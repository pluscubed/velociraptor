package com.pluscubed.velociraptor.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.location.LocationServices
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.snackbar.Snackbar
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.billing.BillingConstants
import com.pluscubed.velociraptor.billing.BillingManager
import com.pluscubed.velociraptor.detection.AppDetectionService
import com.pluscubed.velociraptor.limit.LimitService
import com.pluscubed.velociraptor.settings.appselection.AppSelectionActivity
import com.pluscubed.velociraptor.utils.NotificationUtils
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SettingsActivity : AppCompatActivity(), CoroutineScope {

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

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

    //General
    @BindView(R.id.switch_limits)
    lateinit var showSpeedLimitsSwitch: SwitchCompat

    @BindView(R.id.switch_speedometer)
    lateinit var showSpeedometerSwitch: SwitchCompat

    @BindView(R.id.switch_beep)
    lateinit var beepSwitch: SwitchCompat
    @BindView(R.id.button_test_beep)
    lateinit var testBeepButton: Button

    @BindView(R.id.linear_tolerance)
    lateinit var toleranceView: LinearLayout
    @BindView(R.id.text_overview_tolerance)
    lateinit var toleranceOverview: TextView

    @BindView(R.id.linear_size)
    lateinit var sizeView: LinearLayout
    @BindView(R.id.text_overview_size)
    lateinit var sizeOverview: TextView

    @BindView(R.id.linear_opacity)
    lateinit var opacityView: LinearLayout
    @BindView(R.id.text_overview_opacity)
    lateinit var opacityOverview: TextView

    @BindView(R.id.spinner_unit)
    lateinit var unitSpinner: Spinner
    @BindView(R.id.spinner_style)
    lateinit var styleSpinner: Spinner

    //Providers
    @BindView(R.id.here_title)
    lateinit var hereTitle: TextView
    @BindView(R.id.here_provider_desc)
    lateinit var herePriceDesc: TextView
    @BindView(R.id.here_subscribe)
    lateinit var hereSubscribeButton: Button
    @BindView(R.id.here_editdata)
    lateinit var hereEditDataButton: Button

    @BindView(R.id.tomtom_title)
    lateinit var tomtomTitle: TextView
    @BindView(R.id.tomtom_provider_desc)
    lateinit var tomtomPriceDesc: TextView
    @BindView(R.id.tomtom_subscribe)
    lateinit var tomtomSubscribeButton: Button
    @BindView(R.id.tomtom_editdata)
    lateinit var tomtomEditDataButton: Button

    @BindView(R.id.osm_title)
    lateinit var osmTitle: TextView
    @BindView(R.id.osm_editdata)
    lateinit var osmEditDataButton: Button
    @BindView(R.id.osm_donate)
    lateinit var osmDonateButton: Button
    @BindView(R.id.osm_coverage)
    lateinit var osmCoverageButton: Button

    //Advanced
    @BindView(R.id.switch_debugging)
    lateinit var debuggingSwitch: SwitchCompat

    @BindView(R.id.linear_app_selection)
    lateinit var appSelectionContainer: ViewGroup
    @BindView(R.id.button_app_selection)
    lateinit var appSelectionButton: Button

    @BindView(R.id.linear_gmaps_navigation)
    lateinit var gmapsOnlyNavigationContainer: ViewGroup
    @BindView(R.id.switch_gmaps_navigation)
    lateinit var gmapsOnlyNavigationSwitch: SwitchCompat

    @BindView(R.id.switch_notif_controls)
    lateinit var notifControlsContainer: View


    private var notificationManager: NotificationManager? = null
    private var billingManager: BillingManager? = null

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val isBillingManagerReady: Boolean
        get() = billingManager != null && billingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK

    private val isNotificationAccessGranted: Boolean
        get() = NotificationManagerCompat.getEnabledListenerPackages(this@SettingsActivity)
            .contains(BuildConfig.APPLICATION_ID)


    companion object {
        const val PENDING_SERVICE = 4
        const val PENDING_SERVICE_CLOSE = 3
        const val PENDING_SETTINGS = 2
        const val NOTIFICATION_CONTROLS = 42

        const val PRIVACY_URL = "https://www.pluscubed.com/velociraptor/privacy_policy.html"

        const val OSM_EDITDATA_URL = "http://openstreetmap.org"
        const val OSM_COVERAGE_URL = "http://product.itoworld.com/map/124"
        const val HERE_EDITDATA_URL = "https://mapcreator.here.com/mapcreator"
        const val TOMTOM_EDITDATA_URL = "https://www.tomtom.com/mapshare/tools"

        private const val REQUEST_LOCATION = 105
    }


    override fun onDestroy() {
        job.cancel()
        if (billingManager != null) {
            billingManager!!.destroy()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (billingManager != null && billingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK) {
            billingManager!!.queryPurchases()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)

        job = Job()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermCard.visibility = View.GONE
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifControlsContainer.setOnClickListener { v ->
            val intent = Intent(this@SettingsActivity, LimitService::class.java)
            intent.putExtra(LimitService.EXTRA_NOTIF_START, true)
            val pending = PendingIntent.getService(
                this@SettingsActivity,
                PENDING_SERVICE, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            val intentClose = Intent(this@SettingsActivity, LimitService::class.java)
            intentClose.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true)
            val pendingClose = PendingIntent.getService(
                this@SettingsActivity,
                PENDING_SERVICE_CLOSE, intentClose, PendingIntent.FLAG_CANCEL_CURRENT
            )

            val settings = Intent(this@SettingsActivity, SettingsActivity::class.java)
            val settingsIntent = PendingIntent.getActivity(
                this@SettingsActivity,
                PENDING_SETTINGS, settings, PendingIntent.FLAG_CANCEL_CURRENT
            )

            NotificationUtils.initChannels(this)
            val builder =
                NotificationCompat.Builder(this@SettingsActivity, NotificationUtils.CHANNEL_TOGGLES)
                    .setSmallIcon(R.drawable.ic_speedometer_notif)
                    .setContentTitle(getString(R.string.controls_notif_title))
                    .setContentText(getString(R.string.controls_notif_desc))
                    .addAction(0, getString(R.string.show), pending)
                    .addAction(0, getString(R.string.hide), pendingClose)
                    .setDeleteIntent(pendingClose)
                    .setContentIntent(settingsIntent)
            val notification = builder.build()
            notificationManager!!.notify(NOTIFICATION_CONTROLS, notification)
        }


        appSelectionButton.setOnClickListener { v ->
            startActivity(
                Intent(this@SettingsActivity, AppSelectionActivity::class.java)
            )
        }

        enableServiceButton.setOnClickListener { v ->
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
            ActivityCompat.requestPermissions(
                this@SettingsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        }

        val unitAdapter = ArrayAdapter(this, R.layout.spinner_item_text, arrayOf("mph", "km/h"))
        unitAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter
        unitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (PrefUtils.getUseMetric(this@SettingsActivity) != (position == 1)) {
                    PrefUtils.setUseMetric(this@SettingsActivity, position == 1)
                    unitSpinner.dropDownVerticalOffset = Utils.convertDpToPx(
                        this@SettingsActivity,
                        (unitSpinner.selectedItemPosition * -48).toFloat()
                    )

                    Utils.updateFloatingServicePrefs(this@SettingsActivity)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        unitSpinner.setSelection(if (PrefUtils.getUseMetric(this)) 1 else 0)
        unitSpinner.dropDownVerticalOffset =
                Utils.convertDpToPx(this, (unitSpinner.selectedItemPosition * -48).toFloat())

        val styleAdapter = ArrayAdapter(
            this, R.layout.spinner_item_text,
            arrayOf(getString(R.string.united_states), getString(R.string.international))
        )
        styleAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        styleSpinner.adapter = styleAdapter
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != PrefUtils.getSignStyle(this@SettingsActivity)) {
                    PrefUtils.setSignStyle(this@SettingsActivity, position)
                    styleSpinner.dropDownVerticalOffset =
                            Utils.convertDpToPx(
                                this@SettingsActivity,
                                (styleSpinner.selectedItemPosition * -48).toFloat()
                            )

                    Utils.updateFloatingServicePrefs(this@SettingsActivity)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        styleSpinner.setSelection(PrefUtils.getSignStyle(this))
        styleSpinner.dropDownVerticalOffset =
                Utils.convertDpToPx(this, (styleSpinner.selectedItemPosition * -48).toFloat())

        toleranceView.setOnClickListener { v ->
            ToleranceDialogFragment().show(
                supportFragmentManager,
                "dialog_tolerance"
            )
        }

        sizeView.setOnClickListener { v ->
            SizeDialogFragment().show(
                supportFragmentManager,
                "dialog_size"
            )
        }

        opacityView.setOnClickListener { v ->
            OpacityDialogFragment().show(
                supportFragmentManager,
                "dialog_opacity"
            )
        }

        showSpeedometerSwitch.isChecked = PrefUtils.getShowSpeedometer(this)
        (showSpeedometerSwitch.parent as View).setOnClickListener { v ->
            showSpeedometerSwitch.isChecked = !showSpeedometerSwitch.isChecked

            PrefUtils.setShowSpeedometer(this@SettingsActivity, showSpeedometerSwitch.isChecked)

            Utils.updateFloatingServicePrefs(this@SettingsActivity)
        }

        showSpeedLimitsSwitch.isChecked = PrefUtils.getShowLimits(this)
        (showSpeedLimitsSwitch.parent as View).setOnClickListener { v ->
            showSpeedLimitsSwitch.isChecked = !showSpeedLimitsSwitch.isChecked

            PrefUtils.setShowLimits(this@SettingsActivity, showSpeedLimitsSwitch.isChecked)

            Utils.updateFloatingServicePrefs(this@SettingsActivity)
        }


        debuggingSwitch.isChecked = PrefUtils.isDebuggingEnabled(this)
        (debuggingSwitch.parent as View).setOnClickListener { v ->
            debuggingSwitch.isChecked = !debuggingSwitch.isChecked

            PrefUtils.setDebugging(this@SettingsActivity, debuggingSwitch.isChecked)

            Utils.updateFloatingServicePrefs(this@SettingsActivity)
        }

        beepSwitch.isChecked = PrefUtils.isBeepAlertEnabled(this)
        beepSwitch.setOnClickListener { v ->
            PrefUtils.setBeepAlertEnabled(
                this@SettingsActivity,
                beepSwitch.isChecked
            )
        }
        testBeepButton.setOnClickListener { v -> Utils.playBeeps() }

        gmapsOnlyNavigationSwitch.isChecked = isNotificationAccessGranted &&
                PrefUtils.isGmapsOnlyInNavigation(this)
        gmapsOnlyNavigationContainer.setOnClickListener { v ->
            if (!gmapsOnlyNavigationSwitch.isEnabled) {
                return@setOnClickListener
            }

            val accessGranted = isNotificationAccessGranted
            if (accessGranted) {
                gmapsOnlyNavigationSwitch.toggle()
                PrefUtils.setGmapsOnlyInNavigation(
                    this@SettingsActivity,
                    gmapsOnlyNavigationSwitch.isChecked
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                MaterialDialog.Builder(this@SettingsActivity)
                    .content(R.string.gmaps_only_nav_notif_access)
                    .positiveText(R.string.grant)
                    .onPositive { _, _ ->
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
                                enableFloatingButton,
                                R.string.open_settings_failed_notif_access,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    .show()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.gmaps_only_nav_jellybean,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        hereEditDataButton.setOnClickListener { view -> openLink(HERE_EDITDATA_URL) }

        hereSubscribeButton.setOnClickListener { view ->
            if (!isBillingManagerReady) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.in_app_unavailable,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            billingManager!!.initiatePurchaseFlow(
                BillingConstants.SKU_HERE,
                BillingClient.SkuType.SUBS
            )
        }

        tomtomSubscribeButton.setOnClickListener { view ->
            if (!isBillingManagerReady) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.in_app_unavailable,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            billingManager!!.initiatePurchaseFlow(
                BillingConstants.SKU_TOMTOM,
                BillingClient.SkuType.SUBS
            )
        }

        tomtomEditDataButton.setOnClickListener { view -> openLink(TOMTOM_EDITDATA_URL) }

        osmCoverageButton.setOnClickListener { v ->
            if (Utils.isLocationPermissionGranted(this@SettingsActivity)) {
                val fusedLocationProvider =
                    LocationServices.getFusedLocationProviderClient(this@SettingsActivity)
                fusedLocationProvider.lastLocation
                    .addOnCompleteListener(this@SettingsActivity) { task ->
                        var uriString = OSM_COVERAGE_URL
                        if (task.isSuccessful && task.result != null) {
                            val lastLocation = task.result
                            uriString += "?lon=" + lastLocation!!.longitude + "&lat=" + lastLocation.latitude + "&zoom=12"
                        }
                        openLink(uriString)
                    }
            } else {
                openLink(OSM_COVERAGE_URL)
            }
        }

        osmEditDataButton.setOnClickListener { view ->
            MaterialDialog.Builder(this@SettingsActivity)
                .content(R.string.osm_edit)
                .positiveText(R.string.share_link)
                .onPositive { _, _ ->
                    val shareIntent = Intent()
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, OSM_EDITDATA_URL)
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_link)))
                }
                .show()
        }

        osmDonateButton.setOnClickListener { view -> showSupportDialog() }

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this) && !PrefUtils.isFirstRun(this)) {
            showChangelog()
        }

        if (PrefUtils.getVersionCode(this) <= 39 || !PrefUtils.isTermsAccepted(this)) {
            showTermsDialog()
        }

        val checkIcon = AppCompatResources.getDrawable(this, R.drawable.ic_done_green_20dp)
        val crossIcon = AppCompatResources.getDrawable(this, R.drawable.ic_cross_red_20dp)
        osmTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null)
        hereTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)
        tomtomTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)

        billingManager = BillingManager(this, object : BillingManager.BillingUpdatesListener {
            override fun onBillingClientSetupFinished() {
                billingManager!!.querySkuDetailsAsync(
                    BillingClient.SkuType.SUBS,
                    Arrays.asList(BillingConstants.SKU_HERE, BillingConstants.SKU_TOMTOM)
                ) { responseCode, skuDetailsList ->
                    if (responseCode != BillingClient.BillingResponse.OK) {
                        return@querySkuDetailsAsync
                    }
                    for (details in skuDetailsList) {
                        if (details.sku == BillingConstants.SKU_HERE) {
                            herePriceDesc.text = getString(
                                R.string.here_desc,
                                getString(R.string.per_month, details.price)
                            )
                        }

                        if (details.sku == BillingConstants.SKU_TOMTOM) {
                            tomtomPriceDesc.text = getString(
                                R.string.tomtom_desc,
                                getString(R.string.per_month, details.price)
                            )
                        }
                    }
                }
            }

            override fun onConsumeFinished(token: String, result: Int) {

            }

            override fun onPurchasesUpdated(purchases: List<Purchase>) {
                val purchased = HashSet<String>()

                for (purchase in purchases) {
                    purchased.add(purchase.sku)
                }

                setSubscriptionState(
                    hereSubscribeButton,
                    hereTitle,
                    purchased.contains(BillingConstants.SKU_HERE)
                )
                setSubscriptionState(
                    tomtomSubscribeButton,
                    tomtomTitle,
                    purchased.contains(BillingConstants.SKU_TOMTOM)
                )
            }
        })

        PrefUtils.setFirstRun(this, false)
        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE)
    }

    private fun setSubscriptionState(button: Button?, title: TextView?, subscribed: Boolean) {
        if (subscribed) {
            button!!.isEnabled = false
            button.setText(R.string.subscribed)
            val checkIcon = AppCompatResources.getDrawable(this, R.drawable.ic_done_green_20dp)
            title!!.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null)
        } else {
            button!!.isEnabled = true
            button.setText(R.string.subscribe)
            val crossIcon = AppCompatResources.getDrawable(this, R.drawable.ic_cross_red_20dp)
            title!!.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)
        }
    }


    private fun openLink(uriString: String) {
        val intent = Intent()
        intent.data = Uri.parse(uriString)
        intent.action = Intent.ACTION_VIEW
        try {
            this@SettingsActivity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                enableFloatingButton,
                getString(R.string.open_link_failed, uriString),
                Snackbar.LENGTH_LONG
            ).show()
        }

    }

    private suspend fun querySkuDetails(
        manager: BillingManager,
        itemType: String,
        vararg skuList: String
    ): List<SkuDetails> = suspendCoroutine { cont ->
        manager.querySkuDetailsAsync(
            itemType,
            Arrays.asList(*skuList)
        ) { responseCode, skuDetailsList ->
            if (responseCode != BillingClient.BillingResponse.OK) {
                cont.resumeWithException(Throwable("Billing error: $responseCode"))
            } else {
                cont.resume(skuDetailsList)
            }
        }
    }

    fun showSupportDialog() {
        if (!isBillingManagerReady) {
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.in_app_unavailable,
                Snackbar.LENGTH_SHORT
            ).show()
        }

        launch {
            try {
                val monthlyDonations = async(Dispatchers.IO) {
                    querySkuDetails(
                        billingManager!!,
                        BillingClient.SkuType.SUBS,
                        BillingConstants.SKU_D1_MONTHLY,
                        BillingConstants.SKU_D3_MONTHLY
                    )
                }
                val oneTimeDonations = async(Dispatchers.IO) {
                    querySkuDetails(
                        billingManager!!,
                        BillingClient.SkuType.INAPP,
                        BillingConstants.SKU_D1,
                        BillingConstants.SKU_D3,
                        BillingConstants.SKU_D5,
                        BillingConstants.SKU_D10,
                        BillingConstants.SKU_D20
                    )
                }

                val skuDetailsList = monthlyDonations.await() + oneTimeDonations.await()

                val content = getString(R.string.support_dev_dialog)

                val builder = MaterialDialog.Builder(this@SettingsActivity)
                    .icon(
                        AppCompatResources.getDrawable(
                            this@SettingsActivity,
                            R.drawable.ic_favorite_black_24dp
                        )!!
                    )
                    .title(R.string.support_development)
                    .content(Html.fromHtml(content))

                val purchaseDisplay = ArrayList<String>()
                for (details in skuDetailsList) {
                    var amount = details.price
                    if (details.type == BillingClient.SkuType.SUBS)
                        amount = getString(R.string.per_month, amount)
                    else {
                        amount = getString(R.string.one_time, amount)
                    }
                    purchaseDisplay.add(amount)
                }

                builder.items(purchaseDisplay)
                    .itemsCallback { dialog, itemView, which, text ->
                        val skuDetails = skuDetailsList[which]
                        billingManager!!.initiatePurchaseFlow(skuDetails.sku, skuDetails.type)
                    }

                builder.show()
            } catch (e: Exception) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.in_app_unavailable,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        startLimitService(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings_about -> {
                showAboutDialog()
                return true
            }
            R.id.menu_settings_changelog -> {
                showChangelog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
            .positiveText(R.string.dismiss)
            .content(Html.fromHtml(getString(R.string.about_body)))
            .neutralText(R.string.licenses)
            .onNeutral { _, _ ->
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        OssLicensesMenuActivity::class.java
                    )
                )
            }
            .negativeText(R.string.terms)
            .onNegative { _, _ -> showTermsDialog() }
            .iconRes(R.mipmap.ic_launcher)
            .show()
    }

    private fun showTermsDialog() {
        var builder: MaterialDialog.Builder = MaterialDialog.Builder(this)
            .content(Html.fromHtml(getString(R.string.terms_body)))
            .neutralText(R.string.privacy_policy)
            .onNeutral { _, _ -> openLink(PRIVACY_URL) }
            .iconRes(R.mipmap.ic_launcher)

        if (!PrefUtils.isTermsAccepted(this)) {
            builder = builder
                .autoDismiss(false)
                .canceledOnTouchOutside(false)
                .positiveText(R.string.accept)
                .onPositive { dialog, which ->
                    PrefUtils.setTermsAccepted(this@SettingsActivity, true)
                    dialog.dismiss()
                }
        }

        builder.show()
    }

    private fun showChangelog() {
        ChangelogDialogFragment.newInstance().show(supportFragmentManager, "CHANGELOG_DIALOG")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            invalidateStates()
        }
    }

    private fun invalidateStates() {
        val permissionGranted = Utils.isLocationPermissionGranted(this)
        enabledLocationImage.setImageResource(if (permissionGranted) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableLocationButton.isEnabled = !permissionGranted

        @SuppressLint("NewApi", "LocalSuppress") val overlayEnabled =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        enabledFloatingImage.setImageResource(if (overlayEnabled) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableFloatingButton.isEnabled = !overlayEnabled

        val serviceEnabled =
            Utils.isAccessibilityServiceEnabled(this, AppDetectionService::class.java)
        enabledServiceImage.setImageResource(if (serviceEnabled) R.drawable.ic_done_green_40dp else R.drawable.ic_cross_red_40dp)
        enableServiceButton.isEnabled = !serviceEnabled

        val constant = getString(
            if (PrefUtils.getUseMetric(this)) R.string.kmph else R.string.mph,
            PrefUtils.getSpeedingConstant(this).toString()
        )
        val percent = getString(R.string.percent, PrefUtils.getSpeedingPercent(this).toString())
        val mode = if (PrefUtils.getToleranceMode(this)) "+" else getString(R.string.or)
        val overview = getString(R.string.tolerance_desc, percent, mode, constant)
        toleranceOverview.text = overview

        val limitSizePercent = getString(
            R.string.percent,
            (PrefUtils.getSpeedLimitSize(this) * 100).toInt().toString()
        )
        val speedLimitSize = getString(R.string.size_limit_overview, limitSizePercent)
        val speedometerSizePercent = getString(
            R.string.percent,
            (PrefUtils.getSpeedometerSize(this) * 100).toInt().toString()
        )
        val speedometerSize = getString(R.string.size_speedometer_overview, speedometerSizePercent)
        sizeOverview.text = speedLimitSize + "\n" + speedometerSize

        opacityOverview.text = getString(R.string.percent, PrefUtils.getOpacity(this).toString())

        if (permissionGranted && overlayEnabled) {
            startLimitService(true)
        }
    }

    private fun startLimitService(start: Boolean) {
        val intent = Intent(this, LimitService::class.java)
        if (!start) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true)
        }
        startService(intent)
    }

    private fun openSettings(settingsAction: String, packageName: String) {
        val intent = Intent(settingsAction)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }


}
