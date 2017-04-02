package com.pluscubed.velociraptor.settings;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import timber.log.Timber;

public class HereConfigurationActivity extends AppCompatActivity {

    public static final ButterKnife.Setter<View, Integer> VISIBILITY = (view, value, index) -> view.setVisibility(value);

    public static final int RETRY_DELAY_MILLIS = 2000;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindViews({R.id.db_image, R.id.sign_up, R.id.view_account})
    List<View> initialSelectionViews;
    @BindView(R.id.sign_up)
    Button signupButton;
    @BindView(R.id.view_account)
    Button viewAccountButton;

    @BindView(R.id.here_app_id)
    EditText hereIdEditText;
    @BindView(R.id.here_app_code)
    EditText hereCodeEditText;
    @BindView(R.id.webview)
    WebView webview;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private Snackbar snackbar;
    private int prevResId;

    public static void executeJavascript(WebView view, String javascript) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(javascript, null);
        } else {
            view.loadUrl("javascript:" + javascript);
        }
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_configuration);
        ButterKnife.bind(this);

        int dp24 = Utils.convertDpToPx(this, 24);

        VectorDrawableCompat signUp = VectorDrawableCompat.create(getResources(), R.drawable.ic_person_add_black_24dp, getTheme());
        signUp.setBounds(0, 0, dp24, dp24);
        signupButton.setCompoundDrawables(signUp, null, null, null);

        VectorDrawableCompat view = VectorDrawableCompat.create(getResources(), R.drawable.ic_eye_black_24dp, getTheme());
        view.setBounds(0, 0, dp24, dp24);
        viewAccountButton.setCompoundDrawables(view, null, null, null);

        webview.getSettings().setJavaScriptEnabled(true);

        HereWebViewClient client = new HereWebViewClient();
        webview.setWebViewClient(client);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setSupportZoom(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDisplayZoomControls(false);

        webview.addJavascriptInterface(this, "android");

        progressBar.setVisibility(View.GONE);
        progressBar.setMax(100);

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Timber.e(consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        showWebView(false);

        signupButton.setOnClickListener(v -> {
            showWebView(true);
            String uriString = "https://developer.here.com/plans?create=Public_Free_Plan_Monthly&keepState=true&step=account";
            webview.loadUrl(uriString);
            showGuideText(R.string.here_config_step1);
        });

        viewAccountButton.setOnClickListener(v -> {
            showWebView(true);
            String url = "https://developer.here.com/projects/";
            webview.loadUrl(url);
            showGuideText(0);
        });

        setSupportActionBar(toolbar);
        setTitle(R.string.here_config_title);

        String[] array = PrefUtils.getHereCodes(this).split(",");
        if (array.length == 2) {
            hereIdEditText.setText(array[0]);
            hereCodeEditText.setText(array[1]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_here_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_here_config_done:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showWebView(boolean show) {
        ButterKnife.apply(initialSelectionViews, VISIBILITY, show ? View.GONE : View.VISIBLE);
        webview.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        if (!show) {
            progressBar.setVisibility(View.GONE);
            showGuideText(0);
        }
    }

    private void showGuideText(int resId) {
        if (prevResId != resId) {
            if (snackbar != null) {
                snackbar.dismiss();
            }
            if (resId != 0) {
                snackbar = Snackbar.make(webview, resId, Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
            }
        }
        prevResId = resId;
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else if (webview.getVisibility() == View.VISIBLE) {
            showWebView(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PrefUtils.setHereCodes(this, hereIdEditText.getText() + "," + hereCodeEditText.getText());
    }

    private void checkElementExist() {
        executeJavascript(webview, "android.onCheckExists(" +
                "document.querySelector('[data-ng-bind=\"::app.app_id\"]')!==null" +
                ")");
    }

    private void attemptGetHereCodes() {
        executeJavascript(webview, "android.onGetCodes(" +
                "document.querySelector('[data-ng-bind=\"::app.app_id\"]').firstChild.nodeValue" + "," +
                "document.querySelector('[data-ng-bind=\"::app.app_code\"]').firstChild.nodeValue" +
                ")");
    }

    @JavascriptInterface
    public void onCheckExists(boolean exists) {
        if (!exists) {
            webview.postDelayed(HereConfigurationActivity.this::checkElementExist, RETRY_DELAY_MILLIS);

            webview.post(() -> showGuideText(R.string.here_config_step2));
        } else {
            webview.post(() -> {
                HereConfigurationActivity.this.attemptGetHereCodes();
                showGuideText(0);
            });
        }
    }

    @JavascriptInterface
    public void onGetCodes(String hereId, String hereCode) {
        if (!hereId.isEmpty() && !hereCode.isEmpty()) {
            webview.post(() -> {
                hereIdEditText.setText(hereId);
                hereCodeEditText.setText(hereCode);
                showGuideText(0);
            });
        } else {
            webview.postDelayed(HereConfigurationActivity.this::checkElementExist, RETRY_DELAY_MILLIS);
        }
    }

    private class HereWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(final WebView view, int errorCode, String description, final String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Timber.e(description);
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Timber.e(error.getDescription().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            Timber.e(url);

            //Set viewport
            executeJavascript(webview, "document.getElementsByName('viewport')[0].setAttribute('content','')");

            //Project page
            if (url.startsWith("https://developer.here.com/projects/")) {
                checkElementExist();
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            Timber.e("start" + url);
        }
    }
}
