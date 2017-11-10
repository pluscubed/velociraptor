package com.pluscubed.velociraptor.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ToleranceDialogFragment extends DialogFragment {

    @BindView(R.id.text_constant_unit)
    TextView constantUnitText;
    @BindView(R.id.edittext_constant)
    EditText constantEditText;
    @BindView(R.id.seekbar_constant)
    SeekBar constantSeekbar;

    @BindView(R.id.text_percent)
    TextView percentText;
    @BindView(R.id.edittext_percent)
    EditText percentEditText;
    @BindView(R.id.seekbar_percent)
    SeekBar percentSeekbar;

    @BindView(R.id.button_and)
    ToggleButton andButton;
    @BindView(R.id.button_or)
    ToggleButton orButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View dialog = getActivity().getLayoutInflater().inflate(R.layout.dialog_tolerance, null, false);
        ButterKnife.bind(this, dialog);

        constantUnitText.setText(Utils.getUnitText(getActivity()));
        constantEditText.setText(String.valueOf(PrefUtils.getSpeedingConstant(getActivity())));
        constantEditText.addTextChangedListener(new TextWatcher() {
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
                    constantSeekbar.setProgress(constant + 25);
                } catch (NumberFormatException e) {
                    constantSeekbar.setProgress(25);
                }
            }
        });
        constantSeekbar.setProgress(PrefUtils.getSpeedingConstant(getActivity()) + 25);
        constantSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    constantEditText.setText(String.valueOf(progress - 25));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        percentText.setText(getString(R.string.percent, ""));
        percentEditText.setText(String.valueOf(PrefUtils.getSpeedingPercent(getActivity())));
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
                    percentSeekbar.setProgress(constant + 25);
                } catch (NumberFormatException e) {
                    percentSeekbar.setProgress(25);
                }
            }
        });
        percentSeekbar.setProgress(PrefUtils.getSpeedingPercent(getActivity()) + 25);
        percentSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    percentEditText.setText(String.valueOf(progress - 25));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        andButton.setChecked(PrefUtils.getToleranceMode(getActivity()));
        orButton.setChecked(!PrefUtils.getToleranceMode(getActivity()));
        andButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                andButton.setChecked(true);
                orButton.setChecked(false);
            }
        });
        orButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orButton.setChecked(true);
                andButton.setChecked(false);
            }
        });

        return new MaterialDialog.Builder(getActivity())
                .customView(dialog, true)
                .title(R.string.speeding_amount)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            PrefUtils.setSpeedingConstant(getActivity(), Integer.parseInt(constantEditText.getText().toString()));
                            PrefUtils.setSpeedingPercent(getActivity(), Integer.parseInt(percentEditText.getText().toString()));
                        } catch (NumberFormatException ignored) {
                        }
                        PrefUtils.setToleranceMode(getActivity(), andButton.isChecked());
                    }
                }).build();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        Utils.updateFloatingServicePrefs(getActivity());
    }
}