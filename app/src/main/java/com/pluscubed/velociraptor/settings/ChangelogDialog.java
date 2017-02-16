package com.pluscubed.velociraptor.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.velociraptor.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ChangelogDialog extends DialogFragment {

    public static ChangelogDialog newInstance() {
        ChangelogDialog dialog = new ChangelogDialog();
        return dialog;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View customView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_webview, null);
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.changelog)
                .customView(customView, false)
                .positiveText(android.R.string.ok)
                .neutralText(R.string.rate)
                .onNeutral((dialog1, which) -> {
                    Intent intent = new Intent()
                            .setAction(Intent.ACTION_VIEW)
                            .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.pluscubed.velociraptor"));
                    startActivity(intent);
                })
                .negativeText(R.string.support)
                .onNegative((dialog1, which) -> {
                    ((SettingsActivity) getActivity()).showSupportDialog();
                })
                .build();

        final WebView webView = (WebView) customView.findViewById(R.id.webview);
        try {
            // Load from changelog.html in the assets folder
            StringBuilder buf = new StringBuilder();
            InputStream html = getResources().openRawResource(R.raw.changelog);
            BufferedReader in = new BufferedReader(new InputStreamReader(html));
            String str;
            while ((str = in.readLine()) != null)
                buf.append(str);
            in.close();

            // Inject color values for WebView body background and links
            webView.loadData(buf.toString(), "text/html; charset=UTF-8", null);
        } catch (Throwable e) {
            webView.loadData("<h1>Unable to load</h1><p>" + e.getLocalizedMessage() + "</p>", "text/html", "UTF-8");
        }

        return dialog;
    }
}