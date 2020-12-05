package com.pluscubed.velociraptor.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.utils.PrefUtils
import com.pluscubed.velociraptor.utils.Utils

class GeneralFragment : Fragment() {

    //General
    @BindView(R.id.switch_limits)
    lateinit var showSpeedLimitsSwitch: SwitchMaterial

    @BindView(R.id.switch_speedometer)
    lateinit var showSpeedometerSwitch: SwitchMaterial

    @BindView(R.id.switch_beep)
    lateinit var beepSwitch: SwitchMaterial

    @BindView(R.id.button_test_beep)
    lateinit var testBeepButton: Button

    @BindView(R.id.linear_tolerance)
    lateinit var toleranceView: LinearLayout

    @BindView(R.id.text_overview_tolerance)
    lateinit var toleranceOverview: TextView

    @BindView(R.id.linear_size)
    lateinit var sizeView: LinearLayout

    @BindView(R.id.text_overview_size)
    lateinit var sizeOverview: TextView

    @BindView(R.id.linear_opacity)
    lateinit var opacityView: LinearLayout

    @BindView(R.id.text_overview_opacity)
    lateinit var opacityOverview: TextView

    @BindView(R.id.spinner_unit)
    lateinit var unitSpinner: Spinner

    @BindView(R.id.spinner_style)
    lateinit var styleSpinner: Spinner

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_general, container, false)
    }


    override fun onResume() {
        super.onResume()
        invalidateStates()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)


        val unitAdapter = ArrayAdapter<String>(requireContext(), R.layout.spinner_item_text, arrayOf("mph", "km/h"))
        unitAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter
        unitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                val isMetric = position == 1
                if (PrefUtils.getUseMetric(activity) != isMetric) {
                    PrefUtils.setUseMetric(activity, isMetric)
                    unitSpinner.dropDownVerticalOffset = Utils.convertDpToPx(
                            activity,
                            (unitSpinner.selectedItemPosition * -48).toFloat()
                    )

                    Utils.updateFloatingServicePrefs(activity)
                    invalidateStates()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        unitSpinner.setSelection(if (PrefUtils.getUseMetric(activity)) 1 else 0)
        unitSpinner.dropDownVerticalOffset =
                Utils.convertDpToPx(activity, (unitSpinner.selectedItemPosition * -48).toFloat())

        val styleAdapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_text,
                arrayOf(getString(R.string.united_states), getString(R.string.international))
        )
        styleAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        styleSpinner.adapter = styleAdapter
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                if (position != PrefUtils.getSignStyle(activity)) {
                    PrefUtils.setSignStyle(activity, position)
                    styleSpinner.dropDownVerticalOffset =
                            Utils.convertDpToPx(
                                    activity,
                                    (styleSpinner.selectedItemPosition * -48).toFloat()
                            )

                    Utils.updateFloatingServicePrefs(activity)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        styleSpinner.setSelection(PrefUtils.getSignStyle(activity))
        styleSpinner.dropDownVerticalOffset =
                Utils.convertDpToPx(activity, (styleSpinner.selectedItemPosition * -48).toFloat())

        toleranceView.setOnClickListener { v ->
            ToleranceDialogFragment().show(
                    childFragmentManager,
                    "dialog_tolerance"
            )
        }

        sizeView.setOnClickListener { v ->
            SizeDialogFragment().show(
                    childFragmentManager,
                    "dialog_size"
            )
        }

        opacityView.setOnClickListener { v ->
            OpacityDialogFragment().show(
                    childFragmentManager,
                    "dialog_opacity"
            )
        }

        showSpeedometerSwitch.isChecked = PrefUtils.getShowSpeedometer(activity)
        (showSpeedometerSwitch.parent as View).setOnClickListener { v ->
            showSpeedometerSwitch.isChecked = !showSpeedometerSwitch.isChecked

            PrefUtils.setShowSpeedometer(activity, showSpeedometerSwitch.isChecked)

            Utils.updateFloatingServicePrefs(activity)
        }

        showSpeedLimitsSwitch.isChecked = PrefUtils.getShowLimits(activity)
        (showSpeedLimitsSwitch.parent as View).setOnClickListener { v ->
            showSpeedLimitsSwitch.isChecked = !showSpeedLimitsSwitch.isChecked

            PrefUtils.setShowLimits(activity, showSpeedLimitsSwitch.isChecked)

            Utils.updateFloatingServicePrefs(activity)
        }

        beepSwitch.isChecked = PrefUtils.isBeepAlertEnabled(activity)
        beepSwitch.setOnClickListener { v ->
            PrefUtils.setBeepAlertEnabled(
                    activity,
                    beepSwitch.isChecked
            )
        }
        testBeepButton.setOnClickListener { v -> Utils.playBeeps() }
    }

    private fun invalidateStates() {
        val constant = getString(
                if (PrefUtils.getUseMetric(activity)) R.string.kmph else R.string.mph,
                PrefUtils.getSpeedingConstant(activity).toString()
        )
        val percent = getString(R.string.percent, PrefUtils.getSpeedingPercent(activity).toString())
        val mode = if (PrefUtils.getToleranceMode(activity)) "+" else getString(R.string.or)
        val overview = getString(R.string.tolerance_desc, percent, mode, constant)
        toleranceOverview.text = overview

        val limitSizePercent = getString(
                R.string.percent,
                (PrefUtils.getSpeedLimitSize(activity) * 100).toInt().toString()
        )
        val speedLimitSize = getString(R.string.size_limit_overview, limitSizePercent)
        val speedometerSizePercent = getString(
                R.string.percent,
                (PrefUtils.getSpeedometerSize(activity) * 100).toInt().toString()
        )
        val speedometerSize = getString(R.string.size_speedometer_overview, speedometerSizePercent)
        sizeOverview.text = speedLimitSize + "\n" + speedometerSize

        opacityOverview.text =
                getString(R.string.percent, PrefUtils.getOpacity(activity).toString())
    }
}