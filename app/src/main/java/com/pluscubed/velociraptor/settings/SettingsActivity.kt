package com.pluscubed.velociraptor.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.limit.LimitService
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils

class SettingsActivity : AppCompatActivity() {

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.bottom_navigation)
    lateinit var bottomNavigationView: BottomNavigationView

    companion object {
        const val PRIVACY_URL = "https://www.pluscubed.com/velociraptor/privacy_policy.html"
        const val TROUBLESHOOT_URL = "https://www.pluscubed.com/velociraptor/troubleshoot.html"
    }

    private val permissionsFragment = PermissionsFragment()
    private val providersFragment = ProvidersFragment()
    private val generalFragment = GeneralFragment()
    private val advancedFragment = AdvancedFragment()
    private var active: Fragment = permissionsFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction()
                .add(R.id.main_view, advancedFragment, "advanced").hide(advancedFragment).commit()
        supportFragmentManager.beginTransaction()
                .add(R.id.main_view, providersFragment, "providers").hide(providersFragment).commit()
        supportFragmentManager.beginTransaction()
                .add(R.id.main_view, generalFragment, "general").hide(generalFragment).commit()
        supportFragmentManager.beginTransaction()
                .add(R.id.main_view, permissionsFragment, "permissions").commit()


        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_advanced -> {
                    supportFragmentManager.beginTransaction().hide(active).show(advancedFragment)
                            .commit()
                    active = advancedFragment
                    true
                }
                R.id.action_general -> {
                    supportFragmentManager.beginTransaction().hide(active).show(generalFragment)
                            .commit()
                    active = generalFragment
                    true
                }
                R.id.action_providers -> {
                    supportFragmentManager.beginTransaction().hide(active).show(providersFragment)
                            .commit()
                    active = providersFragment
                    true
                }
                R.id.action_permissions -> {
                    supportFragmentManager.beginTransaction().hide(active).show(permissionsFragment)
                            .commit()
                    active = permissionsFragment
                    true
                }
                else -> {
                    false
                }
            }
        }

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this) && !PrefUtils.isFirstRun(this)) {
            showChangelog()
        }

        if (PrefUtils.getVersionCode(this) <= 39 || !PrefUtils.isTermsAccepted(this)) {
            showTermsDialog()
        }

        PrefUtils.setFirstRun(this, false)
        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE)
    }

    fun showSupportDialog() {
        providersFragment.showSupportDialog()
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
            R.id.menu_settings_support -> {
                showSupportDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        MaterialDialog(this)
                .title(text = getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
                .positiveButton(R.string.dismiss)
                .message(R.string.about_body) {
                    html()
                    lineSpacing(1.2f)
                }
                .neutralButton(R.string.licenses) {
                    startActivity(
                            Intent(this@SettingsActivity, OssLicensesMenuActivity::class.java)
                    )
                }
                .negativeButton(R.string.terms) { showTermsDialog() }
                .icon(R.mipmap.ic_launcher)
                .show()
    }

    private fun showTermsDialog() {
        var dialog = MaterialDialog(this)
                .message(R.string.terms_body) {
                    html()
                    lineSpacing(1.2f)
                }
                .neutralButton(R.string.privacy_policy) { Utils.openLink(this, toolbar, PRIVACY_URL) }

        if (!PrefUtils.isTermsAccepted(this)) {
            dialog = dialog
                    .noAutoDismiss()
                    .cancelOnTouchOutside(false)
                    .positiveButton(R.string.accept) {
                        PrefUtils.setTermsAccepted(this@SettingsActivity, true)
                        it.dismiss()
                    }
        }

        dialog.show()
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
        val overlayEnabled =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        if (permissionGranted && overlayEnabled) {
            startLimitService(true)
        }
    }

    private fun startLimitService(start: Boolean) {
        val intent = Intent(this, LimitService::class.java)
        if (!start) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

}
