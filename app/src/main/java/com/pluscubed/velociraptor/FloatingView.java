package com.pluscubed.velociraptor;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.gigamole.library.ArcProgressStackView;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.ArrayList;

public class FloatingView implements SpeedLimitView {

    private SpeedLimitService service;

    private WindowManager mWindowManager;

    private int style;

    private View mFloatingView;
    private View mSpeedometer;
    private TextView mLimitText;
    private TextView mSpeedometerText;
    private TextView mSpeedometerUnitsText;
    private TextView mDebuggingText;
    private ArcProgressStackView mArcView;

    public FloatingView(SpeedLimitService service) {
        this.service = service;

        mWindowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        inflateMonitor();
        inflateDebugging();

        updatePrefs();
    }

    @SuppressLint("InflateParams")
    private void inflateDebugging() {
        mDebuggingText = (TextView) LayoutInflater.from(new ContextThemeWrapper(service, R.style.Theme_Velociraptor))
                .inflate(R.layout.floating_stats, null, false);
        WindowManager.LayoutParams debuggingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        debuggingParams.gravity = Gravity.BOTTOM;
        try {
            mWindowManager.addView(mDebuggingText, debuggingParams);
        } catch (Exception e) {
            service.showToast("Velociraptor error: " + e.getMessage());
        }
    }

    private void inflateMonitor() {
        int layout;
        switch (style = PrefUtils.getSignStyle(service)) {
            case PrefUtils.STYLE_US:
                layout = R.layout.floating_us;
                break;
            case PrefUtils.STYLE_INTERNATIONAL:
            default:
                layout = R.layout.floating_international;
                break;
        }

        mFloatingView = LayoutInflater.from(new ContextThemeWrapper(service, R.style.Theme_Velociraptor))
                .inflate(layout, null, false);

        mLimitText = (TextView) mFloatingView.findViewById(R.id.text);
        mArcView = (ArcProgressStackView) mFloatingView.findViewById(R.id.arcview);
        mSpeedometerText = (TextView) mFloatingView.findViewById(R.id.speed);
        mSpeedometerUnitsText = (TextView) mFloatingView.findViewById(R.id.speedUnits);
        mSpeedometer = mFloatingView.findViewById(R.id.speedometer);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.alpha = PrefUtils.getOpacity(service) / 100.0F;
        if (mWindowManager != null)
            try {
                mWindowManager.addView(mFloatingView, params);
            } catch (Exception e) {
                service.showToast("Velociraptor error: " + e);
            }
        mFloatingView.setOnTouchListener(new FloatingOnTouchListener());

        initMonitorPosition();

        final ArrayList<ArcProgressStackView.Model> models = new ArrayList<>();
        models.add(new ArcProgressStackView.Model("", 0, ContextCompat.getColor(service, R.color.colorPrimary800),
                new int[]{ContextCompat.getColor(service, R.color.colorPrimaryA200),
                        ContextCompat.getColor(service, R.color.colorPrimaryA200),
                        ContextCompat.getColor(service, R.color.red500),
                        ContextCompat.getColor(service, R.color.red500)}));
        mArcView.setTextColor(ContextCompat.getColor(service, android.R.color.transparent));
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

                String[] split = PrefUtils.getFloatingLocation(service).split(",");
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

        PrefUtils.setFloatingLocation(service, (float) params.y / screenSize.y, endX == 0);

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();
                try {
                    mWindowManager.updateViewLayout(mFloatingView, params);
                } catch (IllegalArgumentException ignore) {
                }
            }
        });

        valueAnimator.start();
    }

    @Override
    public void setSpeed(int speed, int percentOfWarning) {
        if (PrefUtils.getShowSpeedometer(service) && mSpeedometerText != null) {
            mSpeedometerText.setText(Integer.toString(speed));
            mArcView.getModels().get(0).setProgress(percentOfWarning);
            mArcView.animateProgress();
        }
    }

    @Override
    public void setSpeeding(boolean speeding) {
        int colorRes = speeding ? R.color.red500 : R.color.primary_text_default_material_light;
        int color = ContextCompat.getColor(service, colorRes);
        mSpeedometerText.setTextColor(color);
        mSpeedometerUnitsText.setTextColor(color);
    }

    @Override
    public void setDebuggingText(String text) {
        if (mDebuggingText != null) {
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
        int prefStyle = PrefUtils.getSignStyle(service);
        if (prefStyle != style) {
            removeWindowView(mFloatingView);
            inflateMonitor();
        }
        style = prefStyle;

        boolean debuggingEnabled = PrefUtils.isDebuggingEnabled(service);
        if (mDebuggingText != null) {
            mDebuggingText.setVisibility(debuggingEnabled ? View.VISIBLE : View.GONE);
        }

        boolean speedometerShown = PrefUtils.getShowSpeedometer(service);
        if (mSpeedometer != null) {
            mSpeedometer.setVisibility(speedometerShown ? View.VISIBLE : View.GONE);
        }

        if (mSpeedometerUnitsText != null) {
            mSpeedometerUnitsText.setText(Utils.getUnitText(service));
        }

        if (mFloatingView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
            layoutParams.alpha = PrefUtils.getOpacity(service) / 100F;
            try {
                mWindowManager.updateViewLayout(mFloatingView, layoutParams);
            } catch (IllegalArgumentException ignore) {
            }
        }
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
            fadeOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    params.alpha = (float) valueAnimator.getAnimatedValue();
                    try {
                        mWindowManager.updateViewLayout(mFloatingView, params);
                    } catch (IllegalArgumentException ignore) {
                    }
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
