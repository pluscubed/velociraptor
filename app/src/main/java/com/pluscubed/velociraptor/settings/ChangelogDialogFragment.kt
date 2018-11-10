package com.pluscubed.velociraptor.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.pluscubed.velociraptor.R
import java.io.BufferedReader
import java.io.InputStreamReader

class ChangelogDialogFragment : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customView = LayoutInflater.from(activity).inflate(R.layout.dialog_webview, null)
        val dialog = MaterialDialog.Builder(activity!!)
            .title(R.string.changelog)
            .customView(customView, false)
            .positiveText(android.R.string.ok)
            .neutralText(R.string.rate)
            .onNeutral { dialog1, which ->
                val intent = Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.pluscubed.velociraptor"))
                startActivity(intent)
            }
            .negativeText(R.string.support)
            .onNegative { dialog1, which -> (activity as SettingsActivity).showSupportDialog() }
            .build()

        val webView = customView.findViewById<View>(R.id.webview) as WebView
        try {
            // Load from changelog.html in the assets folder
            val buf = StringBuilder()
            val html = resources.openRawResource(R.raw.changelog)
            val reader = BufferedReader(InputStreamReader(html))
            reader.forEachLine { str ->
                buf.append(str)
            }
            reader.close()

            // Inject color values for WebView body background and links
            webView.loadData(buf.toString(), "text/html; charset=UTF-8", null)
        } catch (e: Throwable) {
            webView.loadData(
                "<h1>Unable to load</h1><p>${e.localizedMessage}</p>",
                "text/html",
                "UTF-8"
            )
        }

        return dialog
    }

    companion object {

        fun newInstance(): ChangelogDialogFragment {
            val dialog = ChangelogDialogFragment()
            return dialog
        }
    }
}