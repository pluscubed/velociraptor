package com.pluscubed.velociraptor.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SizeDialogFragment extends DialogFragment {

    @BindView(R.id.text_percent_limit)
    TextView percentTextLimit;
    @BindView(R.id.edittext_percent_limit)
    EditText percentEditTextLimit;
    @BindView(R.id.seekbar_percent_limit)
    SeekBar percentSeekbarLimit;

    @BindView(R.id.text_percent_speedometer)
    TextView percentTextSpeedometer;
    @BindView(R.id.edittext_percent_speedometer)
    EditText percentEditTextSpeedometer;
    @BindView(R.id.seekbar_percent_speedometer)
    SeekBar percentSeekbarSpeedometer;

    private float initialLimitSize;
    private float initialSpeedometerSize;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View dialog = getActivity().getLayoutInflater().inflate(R.layout.dialog_size, null, false);
        ButterKnife.bind(this, dialog);

        initialLimitSize = getSize(true);
        initialSpeedometerSize = getSize(false);

        setup(true, percentTextLimit, percentEditTextLimit, percentSeekbarLimit);
        setup(false, percentTextSpeedometer, percentEditTextSpeedometer, percentSeekbarSpeedometer);

        return new MaterialDialog.Builder(getActivity())
                .customView(dialog, true)
                .title(R.string.size)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        setSize(true, initialLimitSize);
                        setSize(false, initialSpeedometerSize);
                        Utils.updateFloatingServicePrefs(getActivity());
                    }
                }).build();
    }

    private void setup(final boolean limit, TextView percentText, final EditText percentEditText, final SeekBar percentSeekbar) {
        percentText.setText(getString(R.string.percent, ""));
        percentEditText.setText(String.valueOf((int) (getSize(limit) * 100)));
        percentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int constant = Integer.parseInt(s.toString());
                    percentSeekbar.setProgress(constant - 50);
                } catch (NumberFormatException e) {
                    percentSeekbar.setProgress(50);
                }

                try {
                    setSize(limit, Float.parseFloat(percentEditText.getText().toString()) / 100f);
                    Utils.updateFloatingServicePrefs(getActivity());
                } catch (NumberFormatException ignored) {
                }
            }
        });
        percentSeekbar.setProgress((int) (getSize(limit) * 100) - 50);
        percentSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    percentEditText.setText(String.valueOf(progress + 50));

                    try {
                        setSize(limit, Float.parseFloat(percentEditText.getText().toString()) / 100f);
                        Utils.updateFloatingServicePrefs(getActivity());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private float getSize(boolean limit) {
        return limit ? PrefUtils.getSpeedLimitSize(getActivity()) : PrefUtils.getSpeedometerSize(getActivity());
    }

    private void setSize(boolean limit, float size) {
        if (limit) {
            PrefUtils.setSpeedLimitSize(getActivity(), size);
        } else {
            PrefUtils.setSpeedometerSize(getActivity(), size);
        }
    }

}