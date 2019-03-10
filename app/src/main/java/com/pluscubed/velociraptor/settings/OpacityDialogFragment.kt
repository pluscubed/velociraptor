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
import com.afollestad.materialdialogs.customview.customView
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils

class OpacityDialogFragment : DialogFragment() {


    @BindView(R.id.text_percent)
    lateinit var percentText: TextView
    @BindView(R.id.edittext_percent)
    lateinit var percentEditText: EditText
    @BindView(R.id.seekbar_percent)
    lateinit var percentSeekbar: SeekBar

    private var initialTransparency: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val dialog = activity!!.layoutInflater.inflate(R.layout.dialog_opacity, null, false)
        ButterKnife.bind(this, dialog)

        initialTransparency = PrefUtils.getOpacity(activity)

        percentText.text = getString(R.string.percent, "")
        percentEditText.setText(PrefUtils.getOpacity(activity).toString())
        percentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                try {
                    val constant = Integer.parseInt(s.toString())
                    percentSeekbar.progress = constant
                } catch (e: NumberFormatException) {
                    percentSeekbar.progress = 100
                }

                try {
                    PrefUtils.setOpacity(
                        activity,
                        Integer.parseInt(percentEditText.text.toString())
                    )
                    Utils.updateFloatingServicePrefs(activity)
                } catch (ignored: NumberFormatException) {
                }

            }
        })
        percentSeekbar.progress = PrefUtils.getOpacity(activity)
        percentSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    percentEditText.setText(progress.toString())

                    try {
                        PrefUtils.setOpacity(
                            activity,
                            Integer.parseInt(percentEditText.text.toString())
                        )
                        Utils.updateFloatingServicePrefs(activity)
                    } catch (ignored: NumberFormatException) {
                    }

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        return MaterialDialog(activity!!)
            .customView(view = dialog, scrollable = true)
            .title(R.string.transparency)
            .negativeButton(android.R.string.cancel) {
                PrefUtils.setOpacity(activity, initialTransparency)
                Utils.updateFloatingServicePrefs(activity)
            }
            .positiveButton(android.R.string.ok)
    }

}