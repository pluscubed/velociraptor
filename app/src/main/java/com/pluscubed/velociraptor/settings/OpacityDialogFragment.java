package com.pluscubed.velociraptor.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;

public class OpacityDialogFragment extends DialogFragment {


    @BindView(R.id.text_percent)
    TextView percentText;
    @BindView(R.id.edittext_percent)
    EditText percentEditText;
    @BindView(R.id.seekbar_percent)
    SeekBar percentSeekbar;

    private int initialTransparency;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View dialog = getActivity().getLayoutInflater().inflate(R.layout.dialog_opacity, null, false);
        ButterKnife.bind(this, dialog);

        initialTransparency = PrefUtils.getOpacity(getActivity());

        percentText.setText(getString(R.string.percent, ""));
        percentEditText.setText(String.valueOf(PrefUtils.getOpacity(getActivity())));
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
                    percentSeekbar.setProgress(constant);
                } catch (NumberFormatException e) {
                    percentSeekbar.setProgress(100);
                }

                try {
                    PrefUtils.setOpacity(getActivity(), Integer.parseInt(percentEditText.getText().toString()));
                    Utils.updateFloatingServicePrefs(getActivity());
                } catch (NumberFormatException ignored) {
                }
            }
        });
        percentSeekbar.setProgress(PrefUtils.getOpacity(getActivity()));
        percentSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    percentEditText.setText(String.valueOf(progress));

                    try {
                        PrefUtils.setOpacity(getActivity(), Integer.parseInt(percentEditText.getText().toString()));
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

        return new MaterialDialog.Builder(getActivity())
                .customView(dialog, true)
                .title(R.string.transparency)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        PrefUtils.setOpacity(getActivity(), initialTransparency);
                        Utils.updateFloatingServicePrefs(getActivity());
                    }
                }).build();
    }

}