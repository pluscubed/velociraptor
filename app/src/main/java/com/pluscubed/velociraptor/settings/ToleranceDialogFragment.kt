package com.pluscubed.velociraptor.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils

class ToleranceDialogFragment : DialogFragment() {

    @BindView(R.id.text_constant_unit)
    lateinit var constantUnitText: TextView
    @BindView(R.id.edittext_constant)
    lateinit var constantEditText: EditText
    @BindView(R.id.seekbar_constant)
    lateinit var constantSeekbar: SeekBar

    @BindView(R.id.text_percent)
    lateinit var percentText: TextView
    @BindView(R.id.edittext_percent)
    lateinit var percentEditText: EditText
    @BindView(R.id.seekbar_percent)
    lateinit var percentSeekbar: SeekBar

    @BindView(R.id.button_and)
    lateinit var andButton: ToggleButton
    @BindView(R.id.button_or)
    lateinit var orButton: ToggleButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val dialog = activity!!.layoutInflater.inflate(R.layout.dialog_tolerance, null, false)
        ButterKnife.bind(this, dialog)

        constantUnitText.text = Utils.getUnitText(activity!!)
        constantEditText.setText(PrefUtils.getSpeedingConstant(activity).toString())
        constantEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                try {
                    val constant = Integer.parseInt(s.toString())
                    constantSeekbar.progress = constant + 25
                } catch (e: NumberFormatException) {
                    constantSeekbar.progress = 25
                }

            }
        })
        constantSeekbar.progress = PrefUtils.getSpeedingConstant(activity) + 25
        constantSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    constantEditText.setText((progress - 25).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        percentText.text = getString(R.string.percent, "")
        percentEditText.setText(PrefUtils.getSpeedingPercent(activity).toString())
        percentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                try {
                    val constant = Integer.parseInt(s.toString())
                    percentSeekbar.progress = constant + 25
                } catch (e: NumberFormatException) {
                    percentSeekbar.progress = 25
                }

            }
        })
        percentSeekbar.progress = PrefUtils.getSpeedingPercent(activity) + 25
        percentSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    percentEditText.setText((progress - 25).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        andButton.isChecked = PrefUtils.getToleranceMode(activity)
        orButton.isChecked = !PrefUtils.getToleranceMode(activity)
        andButton.setOnClickListener {
            andButton.isChecked = true
            orButton.isChecked = false
        }
        orButton.setOnClickListener {
            orButton.isChecked = true
            andButton.isChecked = false
        }

        return MaterialDialog(activity!!)
                .customView(view = dialog, scrollable = true)
                .title(R.string.speeding_amount)
                .negativeButton(android.R.string.cancel)
                .positiveButton(android.R.string.ok) {
                    try {
                        PrefUtils.setSpeedingConstant(
                                activity,
                                Integer.parseInt(constantEditText.text.toString())
                        )
                        PrefUtils.setSpeedingPercent(
                                activity,
                                Integer.parseInt(percentEditText.text.toString())
                        )
                    } catch (ignored: NumberFormatException) {
                    }

                    PrefUtils.setToleranceMode(activity, andButton.isChecked)
                }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)

        Utils.updateFloatingServicePrefs(activity)
    }
}