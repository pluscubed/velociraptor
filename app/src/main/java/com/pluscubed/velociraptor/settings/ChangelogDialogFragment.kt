package com.pluscubed.velociraptor.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.utils.getColorResCompat
import java.io.BufferedReader
import java.io.InputStreamReader


class ChangelogDialogFragment : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customView = LayoutInflater.from(activity).inflate(R.layout.dialog_webview, null)
        val dialog = MaterialDialog(activity!!)
            .title(R.string.changelog)
            .customView(view = customView)
            .positiveButton(android.R.string.ok)
            .neutralButton(R.string.rate) {
                val intent = Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.pluscubed.velociraptor"))
                startActivity(intent)
            }
            .negativeButton(R.string.support) { (activity as SettingsActivity).showSupportDialog() }

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

            val primaryColor = activity?.getColorResCompat(android.R.attr.textColorPrimary)
                    ?: Color.BLACK

            val str = buf.toString().replace("TEXTCOLOR", String.format("#%06X", 0xFFFFFF and primaryColor))

            // Inject color values for WebView body background and links
            webView.loadData(str, "text/html; charset=UTF-8", null)

            webView.setBackgroundColor(Color.TRANSPARENT)
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