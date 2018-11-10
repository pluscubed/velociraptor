package com.pluscubed.velociraptor.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils

class SizeDialogFragment : DialogFragment() {

    @BindView(R.id.text_percent_limit)
    lateinit var percentTextLimit: TextView
    @BindView(R.id.edittext_percent_limit)
    lateinit var percentEditTextLimit: EditText
    @BindView(R.id.seekbar_percent_limit)
    lateinit var percentSeekbarLimit: SeekBar

    @BindView(R.id.text_percent_speedometer)
    lateinit var percentTextSpeedometer: TextView
    @BindView(R.id.edittext_percent_speedometer)
    lateinit var percentEditTextSpeedometer: EditText
    @BindView(R.id.seekbar_percent_speedometer)
    lateinit var percentSeekbarSpeedometer: SeekBar

    private var initialLimitSize: Float = 0.toFloat()
    private var initialSpeedometerSize: Float = 0.toFloat()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val dialog = activity!!.layoutInflater.inflate(R.layout.dialog_size, null, false)
        ButterKnife.bind(this, dialog)

        initialLimitSize = getSize(true)
        initialSpeedometerSize = getSize(false)

        setup(true, percentTextLimit, percentEditTextLimit, percentSeekbarLimit)
        setup(
            false,
            percentTextSpeedometer,
            percentEditTextSpeedometer,
            percentSeekbarSpeedometer
        )

        return MaterialDialog.Builder(activity!!)
            .customView(dialog, true)
            .title(R.string.size)
            .negativeText(android.R.string.cancel)
            .positiveText(android.R.string.ok)
            .onNegative { _, _ ->
                setSize(true, initialLimitSize)
                setSize(false, initialSpeedometerSize)
                Utils.updateFloatingServicePrefs(activity)
            }.build()
    }

    private fun setup(
        limit: Boolean,
        percentText: TextView,
        percentEditText: EditText,
        percentSeekbar: SeekBar
    ) {
        percentText.text = getString(R.string.percent, "")
        percentEditText.setText((getSize(limit) * 100).toInt().toString())
        percentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                try {
                    val constant = Integer.parseInt(s.toString())
                    percentSeekbar.progress = constant - 50
                } catch (e: NumberFormatException) {
                    percentSeekbar.progress = 50
                }

                try {
                    setSize(
                        limit,
                        java.lang.Float.parseFloat(percentEditText.text.toString()) / 100f
                    )
                    Utils.updateFloatingServicePrefs(activity)
                } catch (ignored: NumberFormatException) {
                }

            }
        })
        percentSeekbar.progress = (getSize(limit) * 100).toInt() - 50
        percentSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    percentEditText.setText((progress + 50).toString())

                    try {
                        setSize(
                            limit,
                            java.lang.Float.parseFloat(percentEditText.text.toString()) / 100f
                        )
                        Utils.updateFloatingServicePrefs(activity)
                    } catch (ignored: NumberFormatException) {
                    }

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun getSize(limit: Boolean): Float {
        return if (limit) PrefUtils.getSpeedLimitSize(activity) else PrefUtils.getSpeedometerSize(
            activity
        )
    }

    private fun setSize(limit: Boolean, size: Float) {
        if (limit) {
            PrefUtils.setSpeedLimitSize(activity, size)
        } else {
            PrefUtils.setSpeedometerSize(activity, size)
        }
    }

}