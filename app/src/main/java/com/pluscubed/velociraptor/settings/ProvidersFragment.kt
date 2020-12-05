package com.pluscubed.velociraptor.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.billing.BillingConstants
import com.pluscubed.velociraptor.billing.BillingManager
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ProvidersFragment : Fragment() {

    //Providers
    @BindView(R.id.here_container)
    lateinit var hereContainer: View

    @BindView(R.id.here_title)
    lateinit var hereTitle: TextView

    @BindView(R.id.here_provider_desc)
    lateinit var herePriceDesc: TextView

    @BindView(R.id.here_subscribe)
    lateinit var hereSubscribeButton: Button

    @BindView(R.id.here_editdata)
    lateinit var hereEditDataButton: Button

    @BindView(R.id.tomtom_container)
    lateinit var tomtomContainer: View

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

    private var billingManager: BillingManager? = null

    private val isBillingManagerReady: Boolean
        get() = billingManager != null && billingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingManager != null) {
            billingManager!!.destroy()
        }
    }

    override fun onResume() {
        super.onResume()
        if (billingManager != null && billingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK) {
            billingManager!!.queryPurchases()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_providers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)


        hereEditDataButton.setOnClickListener {
            Utils.openLink(activity, view, HERE_EDITDATA_URL)
        }

        hereSubscribeButton.setOnClickListener {
            if (!isBillingManagerReady) {
                Snackbar.make(
                        view,
                        R.string.in_app_unavailable,
                        Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            billingManager?.initiatePurchaseFlow(
                    BillingConstants.SKU_HERE,
                    BillingClient.SkuType.SUBS
            )
        }

        tomtomSubscribeButton.setOnClickListener {
            if (!isBillingManagerReady) {
                Snackbar.make(
                        view,
                        R.string.in_app_unavailable,
                        Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener;
            }
            billingManager?.initiatePurchaseFlow(
                    BillingConstants.SKU_TOMTOM,
                    BillingClient.SkuType.SUBS
            )
        }

        tomtomEditDataButton.setOnClickListener {
            Utils.openLink(activity, view, TOMTOM_EDITDATA_URL)
        }

        osmCoverageButton.setOnClickListener {
            openOsmCoverage()
        }

        osmEditDataButton.setOnClickListener {
            activity?.let { activity ->
                MaterialDialog(activity)
                        .show {
                            var text = getString(R.string.osm_edit)
                            if (text.contains("%s")) {
                                text = text.format("<b>$OSM_EDITDATA_URL</b>")
                            }
                            message(text = text.parseAsHtml()) {
                                lineSpacing(1.2f)
                            }
                            positiveButton(R.string.share_link) { _ ->
                                val shareIntent = Intent()
                                shareIntent.type = "text/plain"
                                shareIntent.putExtra(Intent.EXTRA_TEXT, OSM_EDITDATA_URL)
                                startActivity(
                                        Intent.createChooser(
                                                shareIntent,
                                                getString(R.string.share_link)
                                        )
                                )
                            }
                        }
            }
        }

        osmDonateButton.setOnClickListener { Utils.openLink(activity, view, OSM_DONATE_URL) }

        val checkIcon =
                activity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_done_green_20dp) }
        val crossIcon =
                activity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_cross_red_20dp) }
        osmTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null)
        hereTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)
        tomtomTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)

        billingManager = BillingManager(activity, object : BillingManager.BillingUpdatesListener {
            override fun onBillingClientSetupFinished() {
                try {
                    billingManager?.querySkuDetailsAsync(
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
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }

            override fun onConsumeFinished(token: String, result: Int) {
                PrefUtils.setSupported(activity, true)
            }

            override fun onPurchasesUpdated(purchases: List<Purchase>) {
                val purchased = HashSet<String>()

                if (purchased.size > 0) {
                    PrefUtils.setSupported(activity, true)
                }

                val onetime = BillingConstants.getSkuList(BillingClient.SkuType.INAPP)
                for (purchase in purchases) {
                    if (purchase.sku in onetime) {
                        try {
                            billingManager?.consumeAsync(purchase.purchaseToken);
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    } else {
                        purchased.add(purchase.sku)
                    }
                }

                val hereSubscribed = purchased.contains(BillingConstants.SKU_HERE)
                if (hereSubscribed) {
                    hereContainer.isVisible = true
                }
                setButtonSubscriptionState(
                        hereSubscribeButton,
                        hereTitle,
                        purchased.contains(BillingConstants.SKU_HERE),
                        BillingConstants.SKU_HERE
                )

                val tomtomSubscribed = purchased.contains(BillingConstants.SKU_TOMTOM)
                if (tomtomSubscribed) {
                    tomtomContainer.isVisible = true
                }
                setButtonSubscriptionState(
                        tomtomSubscribeButton,
                        tomtomTitle,
                        tomtomSubscribed,
                        BillingConstants.SKU_TOMTOM
                )
            }
        })

    }

    private fun setButtonSubscriptionState(button: Button?, title: TextView?, subscribed: Boolean, sku: String) {
        if (subscribed) {
            button?.setText(R.string.unsubscribe)
            button?.setOnClickListener {
                Utils.openLink(context, it,
                        "http://play.google.com/store/account/subscriptions?package=com.pluscubed.velociraptor&sku=${sku}")
            }
            val checkIcon =
                    activity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_done_green_20dp) }
            title?.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null)
        } else {
            button?.setText(R.string.subscribe)
            button?.setOnClickListener {
                if (!isBillingManagerReady) {
                    Snackbar.make(
                            requireView(),
                            R.string.in_app_unavailable,
                            Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                billingManager?.initiatePurchaseFlow(
                        sku,
                        BillingClient.SkuType.SUBS
                )
            }
            val crossIcon =
                    activity?.let { AppCompatResources.getDrawable(it, R.drawable.ic_cross_red_20dp) }
            title?.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null)
        }
    }

    private suspend fun querySkuDetails(
            manager: BillingManager?,
            itemType: String,
            vararg skuList: String
    ): List<SkuDetails> = suspendCoroutine { cont ->
        manager?.querySkuDetailsAsync(
                itemType,
                Arrays.asList(*skuList)
        ) { responseCode, skuDetailsList ->
            if (responseCode != BillingClient.BillingResponse.OK) {
                cont.resumeWithException(Exception("Billing error: $responseCode"))
            } else {
                cont.resume(skuDetailsList)
            }
        }
    }

    fun showSupportDialog() {
        if (!isBillingManagerReady) {
            view?.let {
                Snackbar.make(it, R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show()
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            supervisorScope {
                try {
                    val monthlyDonations = async(Dispatchers.IO) {
                        querySkuDetails(
                                billingManager,
                                BillingClient.SkuType.SUBS,
                                BillingConstants.SKU_D1_MONTHLY,
                                BillingConstants.SKU_D3_MONTHLY
                        )
                    }
                    val oneTimeDonations = async(Dispatchers.IO) {
                        querySkuDetails(
                                billingManager,
                                BillingClient.SkuType.INAPP,
                                BillingConstants.SKU_D1,
                                BillingConstants.SKU_D3,
                                BillingConstants.SKU_D5,
                                BillingConstants.SKU_D10,
                                BillingConstants.SKU_D20
                        )
                    }

                    val skuDetailsList = monthlyDonations.await() + oneTimeDonations.await()

                    @Suppress("DEPRECATION")
                    var text = getString(R.string.support_dev_dialog)

                    if (PrefUtils.hasSupported(activity)) {
                        text += "\n\n\uD83C\uDF89 " + getString(R.string.support_dev_dialog_badge) + " \uD83C\uDF89"
                    }

                    var dialog = activity?.let {
                        MaterialDialog(it)
                                .icon(
                                        drawable = AppCompatResources.getDrawable(
                                                it,
                                                R.drawable.ic_favorite_black_24dp
                                        )
                                )
                                .title(R.string.support_development)
                                .message(text = text) {
                                    lineSpacing(1.2f)
                                }
                    }

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

                    dialog = dialog?.listItems(items = purchaseDisplay) { _, which, _ ->
                        val skuDetails = skuDetailsList[which]
                        billingManager!!.initiatePurchaseFlow(skuDetails.sku, skuDetails.type)
                    }

                    dialog?.show()
                } catch (e: Exception) {
                    view?.let {
                        Snackbar.make(it, R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openOsmCoverage() {
        if (Utils.isLocationPermissionGranted(activity)) {
            activity?.let {
                val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(it)
                fusedLocationProvider.lastLocation.addOnCompleteListener(it) { task ->
                    var uriString = OSM_COVERAGE_URL
                    if (task.isSuccessful && task.result != null) {
                        val lastLocation = task.result
                        uriString +=
                                "?lon=${lastLocation?.longitude}&lat=${lastLocation?.latitude}&zoom=12"
                    }
                    Utils.openLink(activity, view, uriString)
                }
            }
        } else {
            Utils.openLink(activity, view, OSM_COVERAGE_URL)
        }
    }

    companion object {
        const val OSM_EDITDATA_URL = "https://openstreetmap.org"
        const val OSM_COVERAGE_URL = "https://product.itoworld.com/map/124"
        const val OSM_DONATE_URL = "https://donate.openstreetmap.org"
        const val HERE_EDITDATA_URL = "https://mapcreator.here.com/mapcreator"
        const val TOMTOM_EDITDATA_URL = "https://www.tomtom.com/mapshare/tools"
    }
}