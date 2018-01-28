package com.pluscubed.velociraptor.limit;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.view.ContextThemeWrapper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.gigamole.library.ArcProgressStackView;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FloatingView implements LimitView {

    @BindView(R.id.limit)
    View mLimitView;
    @Nullable
    @BindView(R.id.limit_label_text)
    TextView mLimitLabelText;
    @BindView(R.id.limit_text)
    TextView mLimitText;
    @BindView(R.id.speedometer)
    View mSpeedometerView;
    @BindView(R.id.arcview)
    ArcProgressStackView mArcView;
    @BindView(R.id.speed)
    TextView mSpeedometerText;
    @BindView(R.id.speedUnits)
    TextView mSpeedometerUnitsText;

    private LimitService mService;
    private WindowManager mWindowManager;
    private int mStyle;
    private View mFloatingView;
    private TextView mDebuggingText;

    public FloatingView(LimitService service) {
        this.mService = service;

        mWindowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        inflateMonitor();
        inflateDebugging();

        updatePrefs();
    }

    @SuppressLint("InflateParams")
    private void inflateDebugging() {
        mDebuggingText = (TextView) LayoutInflater.from(new ContextThemeWrapper(mService, R.style.Theme_Velociraptor))
                .inflate(R.layout.floating_stats, null, false);

        WindowManager.LayoutParams debuggingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        debuggingParams.gravity = Gravity.BOTTOM;
        try {
            mWindowManager.addView(mDebuggingText, debuggingParams);
        } catch (Exception e) {
        }
    }

    private int getWindowType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void inflateMonitor() {
        int layout;
        switch (mStyle = PrefUtils.getSignStyle(mService)) {
            case PrefUtils.STYLE_US:
                layout = R.layout.floating_us;
                break;
            case PrefUtils.STYLE_INTERNATIONAL:
            default:
                layout = R.layout.floating_international;
                break;
        }

        mFloatingView = LayoutInflater.from(new ContextThemeWrapper(mService, R.style.Theme_Velociraptor))
                .inflate(layout, null, false);

        ButterKnife.bind(this, mFloatingView);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.alpha = PrefUtils.getOpacity(mService) / 100.0F;
        if (mWindowManager != null)
            try {
                mWindowManager.addView(mFloatingView, params);
            } catch (Exception e) {
            }
        mFloatingView.setOnTouchListener(new FloatingOnTouchListener());

        initMonitorPosition();

        final ArrayList<ArcProgressStackView.Model> models = new ArrayList<>();
        models.add(new ArcProgressStackView.Model("", 0,
                ContextCompat.getColor(mService, R.color.colorPrimary800),
                ContextCompat.getColor(mService, R.color.colorAccent)));
        mArcView.setTextColor(ContextCompat.getColor(mService, android.R.color.transparent));
        mArcView.setInterpolator(new FastOutSlowInInterpolator());
        mArcView.setModels(models);
    }


    private void initMonitorPosition() {
        if (mFloatingView == null) {
            return;
        }
        mFloatingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();

                String[] split = PrefUtils.getFloatingLocation(mService).split(",");
                boolean left = Boolean.parseBoolean(split[0]);
                float yRatio = Float.parseFloat(split[1]);

                Point screenSize = new Point();
                mWindowManager.getDefaultDisplay().getSize(screenSize);
                params.x = left ? 0 : screenSize.x - mFloatingView.getWidth();
                params.y = (int) (yRatio * screenSize.y + 0.5f);

                try {
                    mWindowManager.updateViewLayout(mFloatingView, params);
                } catch (IllegalArgumentException ignore) {
                }

                mFloatingView.setVisibility(View.VISIBLE);

                mFloatingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void changeConfig() {
        initMonitorPosition();
    }

    @Override
    public void stop() {
        removeWindowView(mFloatingView);
        removeWindowView(mDebuggingText);
    }

    private void removeWindowView(View view) {
        if (view != null && mWindowManager != null)
            try {
                mWindowManager.removeView(view);
            } catch (IllegalArgumentException ignore) {
            }
    }

    private void animateViewToSideSlot() {
        Point screenSize = new Point();
        mWindowManager.getDefaultDisplay().getSize(screenSize);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
        int endX;
        if (params.x + mFloatingView.getWidth() / 2 >= screenSize.x / 2) {
            endX = screenSize.x - mFloatingView.getWidth();
        } else {
            endX = 0;
        }

        PrefUtils.setFloatingLocation(mService, (float) params.y / screenSize.y, endX == 0);

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            WindowManager.LayoutParams params1 = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
            params1.x = (int) animation.getAnimatedValue();
            try {
                mWindowManager.updateViewLayout(mFloatingView, params1);
            } catch (IllegalArgumentException ignore) {
            }
        });

        valueAnimator.start();
    }

    @Override
    public void setSpeed(int speed, int percentOfWarning) {
        if (PrefUtils.getShowSpeedometer(mService) && mSpeedometerText != null) {
            mSpeedometerText.setText(Integer.toString(speed));
            mArcView.getModels().get(0).setProgress(percentOfWarning);
            mArcView.animateProgress();
        }
    }

    @Override
    public void setSpeeding(boolean speeding) {
        int colorRes = speeding ? R.color.red500 : R.color.primary_text_default_material_light;
        int color = ContextCompat.getColor(mService, colorRes);
        mSpeedometerText.setTextColor(color);
        mSpeedometerUnitsText.setTextColor(color);
    }

    @Override
    public void setDebuggingText(String text) {
        if (mDebuggingText != null && mDebuggingText.getVisibility() == View.VISIBLE) {
            mDebuggingText.setText(text);
        }
    }

    @Override
    public void setLimitText(String text) {
        if (mLimitText != null) {
            mLimitText.setText(text);
        }
    }

    @Override
    public void updatePrefs() {
        int prefStyle = PrefUtils.getSignStyle(mService);
        if (prefStyle != mStyle) {
            removeWindowView(mFloatingView);
            inflateMonitor();
        }
        mStyle = prefStyle;

        boolean debuggingEnabled = PrefUtils.isDebuggingEnabled(mService);
        if (mDebuggingText != null) {
            mDebuggingText.setVisibility(debuggingEnabled ? View.VISIBLE : View.GONE);
        }

        boolean speedometerShown = PrefUtils.getShowSpeedometer(mService);
        if (mSpeedometerView != null) {
            mSpeedometerView.setVisibility(speedometerShown ? View.VISIBLE : View.GONE);
        }

        boolean limitShown = PrefUtils.getShowLimits(mService);
        if (mLimitView != null) {
            mLimitView.setVisibility(limitShown ? View.VISIBLE : View.GONE);
        }

        if (mSpeedometerUnitsText != null) {
            mSpeedometerUnitsText.setText(Utils.getUnitText(mService));
        }

        if (mFloatingView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
            layoutParams.alpha = PrefUtils.getOpacity(mService) / 100F;
            try {
                mWindowManager.updateViewLayout(mFloatingView, layoutParams);
            } catch (IllegalArgumentException ignore) {
            }
        }

        if (mLimitView != null && mLimitText != null) {
            float speedLimitSize = PrefUtils.getSpeedLimitSize(mService);
            float textSize = 0;
            float height = 0;
            float width = 0;

            switch (mStyle) {
                case PrefUtils.STYLE_US:
                    float cardSidePadding = (float) (2 + (1 - Math.cos(Math.toRadians(45))) * 2);
                    float cardTopBottomPadding = (float) (2 * 1.5 + (1 - Math.cos(Math.toRadians(45))) * 2);

                    width = speedLimitSize * 56;
                    height = speedLimitSize * 72;

                    width += cardSidePadding * 2;
                    height += cardTopBottomPadding * 2;

                    textSize = 28;

                    break;
                case PrefUtils.STYLE_INTERNATIONAL:
                    height = speedLimitSize * 64;
                    width = speedLimitSize * 64;

                    textSize = 0;
                    break;
            }

            ViewGroup.LayoutParams layoutParams = mLimitView.getLayoutParams();
            layoutParams.width = Utils.convertDpToPx(mService, width);
            layoutParams.height = Utils.convertDpToPx(mService, height);
            mLimitView.setLayoutParams(layoutParams);

            if (textSize != 0) {
                textSize *= speedLimitSize;
                mLimitText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            }

            float labelTextSize = 12 * speedLimitSize;
            if (mLimitLabelText != null) {
                mLimitLabelText.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelTextSize);
            }
        }

        if (mSpeedometerView != null && mSpeedometerText != null && mSpeedometerUnitsText != null) {
            float speedometerSize = PrefUtils.getSpeedometerSize(mService);

            float width = 64 * speedometerSize;
            float height = 64 * speedometerSize;

            float textSize = 24 * speedometerSize;
            mSpeedometerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

            float labelTextSize = 12 * speedometerSize;
            mSpeedometerUnitsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelTextSize);

            ViewGroup.LayoutParams layoutParams = mSpeedometerView.getLayoutParams();
            layoutParams.width = Utils.convertDpToPx(mService, width);
            layoutParams.height = Utils.convertDpToPx(mService, height);
            mSpeedometerView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void hideLimit(boolean hideLimit) {
        mLimitView.setVisibility(!PrefUtils.getShowLimits(mService) ? View.GONE : (hideLimit ? View.INVISIBLE : View.VISIBLE));
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {

        private float mInitialTouchX;
        private float mInitialTouchY;
        private int mInitialX;
        private int mInitialY;
        private long mStartClickTime;
        private boolean mIsClick;

        private AnimatorSet fadeAnimator;
        private float initialAlpha;
        private ValueAnimator fadeOut;
        private ValueAnimator fadeIn;

        public FloatingOnTouchListener() {
            final WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
            fadeOut = ValueAnimator.ofFloat(params.alpha, 0.1F);
            fadeOut.setInterpolator(new FastOutSlowInInterpolator());
            fadeOut.setDuration(100);
            fadeOut.addUpdateListener(valueAnimator -> {
                params.alpha = (float) valueAnimator.getAnimatedValue();
                try {
                    mWindowManager.updateViewLayout(mFloatingView, params);
                } catch (IllegalArgumentException ignore) {
                }
            });
            fadeIn = fadeOut.clone();
            fadeIn.setFloatValues(0.1F, params.alpha);
            fadeIn.setStartDelay(5000);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mInitialTouchX = event.getRawX();
                    mInitialTouchY = event.getRawY();

                    mInitialX = params.x;
                    mInitialY = params.y;

                    mStartClickTime = System.currentTimeMillis();

                    mIsClick = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dX = event.getRawX() - mInitialTouchX;
                    float dY = event.getRawY() - mInitialTouchY;
                    if ((mIsClick && (Math.abs(dX) > 10 || Math.abs(dY) > 10))
                            || System.currentTimeMillis() - mStartClickTime > ViewConfiguration.getLongPressTimeout()) {
                        mIsClick = false;
                    }

                    if (!mIsClick) {
                        params.x = (int) (dX + mInitialX);
                        params.y = (int) (dY + mInitialY);

                        try {
                            mWindowManager.updateViewLayout(mFloatingView, params);
                        } catch (IllegalArgumentException ignore) {
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mIsClick && System.currentTimeMillis() - mStartClickTime <= ViewConfiguration.getLongPressTimeout()) {
                        if (fadeAnimator != null && fadeAnimator.isStarted()) {
                            fadeAnimator.cancel();
                            params.alpha = initialAlpha;
                            try {
                                mWindowManager.updateViewLayout(mFloatingView, params);
                            } catch (IllegalArgumentException ignore) {
                            }
                        } else {
                            initialAlpha = params.alpha;

                            fadeAnimator = new AnimatorSet();
                            fadeAnimator.play(fadeOut).before(fadeIn);
                            fadeAnimator.start();
                        }
                    } else {
                        animateViewToSideSlot();
                    }
                    return true;
            }
            return false;
        }
    }

}
