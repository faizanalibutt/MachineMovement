package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dev.bytes.adsmanager.ADUnitPlacements
import com.dev.bytes.adsmanager.loadNativeAd
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.BuildConfig
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.Database
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.R
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Graph
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.TimeUtils.getDuration
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Util
import kotlinx.android.synthetic.main.fragment_report.view.*
import kotlinx.android.synthetic.main.fragment_report.view.average_value
import kotlinx.android.synthetic.main.fragment_report.view.total_value
import kotlinx.android.synthetic.main.fragment_report_pedo.*
import kotlinx.android.synthetic.main.fragment_report_pedo.view.*
import kotlinx.android.synthetic.main.fragment_report_pedo.view.bargraph1
import kotlinx.android.synthetic.main.fragment_report_pedo.view.chipGroup
import kotlinx.android.synthetic.main.fragment_report_pedo.view.emptyData
import kotlinx.android.synthetic.main.fragment_report_pedo.view.graphView
import kotlinx.android.synthetic.main.fragment_report_pedo.view.text_average
import kotlinx.android.synthetic.main.fragment_report_pedo.view.text_total
import kotlin.math.abs
import kotlin.math.pow

@SuppressLint("SetTextI18n")
class ReportFragment : Fragment() {

    private var today: Int = 0
    private lateinit var mView: View
    private var listCurrentWeekInterval: MutableList<Pair<Long, Int>> = ArrayList()

    private var chart: com.github.mikephil.charting.charts.BarChart? = null
    private lateinit var valueFormatter: ValueFormatter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_report_pedo, container, false)

        chart = view.bargraph1

        // set chart properties
        chart!!.description.isEnabled = false
        chart!!.setDrawBarShadow(true)
        chart!!.setDrawGridBackground(false)

        val xAxis = chart!!.xAxis

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.isGranularityEnabled = true
        xAxis.setDrawAxisLine(false)
        xAxis.textSize = 8f
//                xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f // only intervals of 1 day

        chart!!.axisLeft.setDrawGridLines(false)
        chart!!.axisRight.setDrawGridLines(false)
        chart!!.axisLeft.isEnabled = false
        chart!!.axisRight.isEnabled = false
        chart!!.legend.isEnabled = false
        chart!!.axisLeft.axisMinimum = 0f
        chart!!.axisRight.axisMinimum = 0f
        chart?.setScaleEnabled(false)

        // add a nice and smooth animation
        chart!!.animateY(2000)

        val chipGroup: RadioGroup = view.findViewById(R.id.chipGroup)
        val database = Database.getInstance(view.context)

        chipGroup.setOnCheckedChangeListener { chip_group, i ->
            when (chip_group.findViewById<MaterialRadioButton>(i)) {
                time_graph -> {
                    time_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    distance_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    step_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    getIntervalsDataWeekly(database, view, Graph.TIME)
                }
                distance_graph -> {
                    time_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    distance_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    step_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    getIntervalsDataWeekly(database, view, Graph.DISTANCE)
                }
                step_graph -> {
                    time_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    distance_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    step_graph.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    getIntervalsDataWeekly(database, view, Graph.STEP)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ad_container = view.findViewById<FrameLayout>(R.id.ad_container_pedo)
        view.context.loadNativeAd(
            ad_container,
            R.layout.ad_unified_common,
            ADUnitPlacements.COMMON_NATIVE_AD, true
        )
        mView = view
        view.step_graph.isChecked = true
        chart?.visibility = View.INVISIBLE
        getIntervalsDataWeekly(Database.getInstance(view.context), view, Graph.STEP)
    }

    override fun onResume() {
        super.onResume()
        mView.let {
            mView.step_graph.isChecked = true
            Handler().postDelayed({
                chart?.visibility = View.VISIBLE
                getIntervalsDataWeekly(Database.getInstance(mView.context), mView, Graph.STEP)
            }, 1000)
        }
    }

    private fun getIntervalsDataWeekly(database: Database, view: View, graphType: Graph) {

        listCurrentWeekInterval = database.getLastEntries(7)

        if (listCurrentWeekInterval.size == 0) {
            view.emptyData.visibility = View.VISIBLE
            view.graphView.visibility = View.GONE
            return
        } else {
            view.emptyData.visibility = View.GONE
            view.graphView.visibility = View.VISIBLE
        }

        valueFormatter = TimeAxisValueFormatter(
            listCurrentWeekInterval,
            chart!!, "pedo"
        )
        chart!!.xAxis.valueFormatter = valueFormatter

        when (graphType) {

            Graph.TIME -> {
                chart!!.xAxis.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.axisMaximum =
                    Util.getRandom(10.toDouble().pow(5.toDouble()).toInt()).toFloat()
                chart!!.axisLeft.axisMaximum =
                    Util.getRandom(10.toDouble().pow(5.toDouble()).toInt()).toFloat()
                chart!!.axisLeft.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.labelCount = listCurrentWeekInterval.size
            }

            Graph.DISTANCE -> {
                chart!!.xAxis.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.axisMaximum = 100000f
                chart!!.axisLeft.axisMaximum = 100000f
                chart!!.axisLeft.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.labelCount = listCurrentWeekInterval.size
            }

            Graph.STEP -> {
                chart!!.xAxis.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.axisMaximum = 10000f
                chart!!.axisLeft.axisMaximum = 10000f
                chart!!.axisLeft.labelCount = listCurrentWeekInterval.size
                chart!!.axisRight.labelCount = listCurrentWeekInterval.size
            }

            else -> {
            }
        }

        var total = 0.0
        chart!!.clear()
        today = listCurrentWeekInterval.size - 1
        val values: MutableList<BarEntry> = ArrayList()

        for ((index, step) in listCurrentWeekInterval.withIndex()) {

            when (graphType) {

                Graph.TIME -> {

                    var steps: Int

                    if (index == today) {
                        var todayOffset = database.getSteps(step.first)
                        steps = database.currentSteps
                        database.close()

                        if (todayOffset == Integer.MIN_VALUE)
                            todayOffset = -steps

                        steps += todayOffset
                        today = 0
                    } else
                        steps = step.second

                    val stepTime: Long
                    stepTime = if (BuildConfig.DEBUG)
                        steps / 10 * 60000.toLong()
                    else
                        steps / 150 * 60000.toLong()

                    total += stepTime

                    values.add(BarEntry(index.toFloat(), stepTime.toFloat()))
                    chartData(values, view, graphType)

                }

                Graph.DISTANCE -> {

                    var steps: Int

                    if (index == today) {
                        var todayOffset = database.getSteps(step.first)
                        steps = database.currentSteps
                        database.close()

                        if (todayOffset == Integer.MIN_VALUE)
                            todayOffset = -steps

                        steps += todayOffset
                        today = 0
                    } else
                        steps = step.second

                    // update only every 10 steps when displaying distance
                    val prefs = requireActivity().getSharedPreferences(
                        "pedometer",
                        Context.MODE_PRIVATE
                    )
                    val stepsize =
                        prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE)

                    var distance: Float = steps * stepsize
                    distance /= if (prefs.getString("stepsize_unit",
                            Fragment_Settings.DEFAULT_STEP_UNIT) == "cm") {
                        160934f
                    } else {
                        5280f
                    }

                    total += distance
                    values.add(BarEntry(index.toFloat(), if (distance < 0) 0.0f else distance))
                    chartData(values, view, graphType)

                }

                Graph.STEP -> {

                    var steps: Int

                    if (index == today) {
                        var todayOffset = database.getSteps(step.first)
                        steps = database.currentSteps
                        database.close()

                        if (todayOffset == Integer.MIN_VALUE)
                            todayOffset = -steps

                        steps += todayOffset
                        today = 0
                    } else
                        steps = step.second

                    total += steps
                    values.add(BarEntry(index.toFloat(), if (steps < 0) 0.0f else steps.toFloat()))
                    chartData(values, view, graphType)
                }
                else -> {
                }
            }

        }

        when (graphType) {
            Graph.TIME -> {
                view.total_value.text = if (total.toLong() == 0L) "00" else getDuration(
                    total.toLong()
                )
                view.average_value.text = if (total.toLong() == 0L) "00" else getDuration(
                    total.toLong() / listCurrentWeekInterval.size
                )
            }
            Graph.DISTANCE -> {
                view.total_value.text = "${AppUtils.roundTwoDecimal(total)} km"
                view.average_value.text =
                    "${AppUtils.roundTwoDecimal(total / listCurrentWeekInterval.size)} km"
            }
            Graph.STEP -> {
                view.total_value.text = "${total.toInt()}"
                view.average_value.text = "${(total / listCurrentWeekInterval.size).toInt()}"
            }
            else -> {
            }
        }

    }


    private fun chartData(
        values: MutableList<BarEntry>,
        view: View,
        graphType: Graph
    ) {

        when (graphType) {
            Graph.TIME -> {
                if (chart!!.data != null && chart!!.data.dataSetCount > 0) {
                    val set1 = chart!!.data.getDataSetByIndex(0) as BarDataSet
                    set1.values = values
                    chart!!.data.notifyDataChanged()
                    chart!!.notifyDataSetChanged()
                } else {
                    val set1 = BarDataSet(values, "")
                    set1.color = ContextCompat.getColor(
                        view.context,
                        R.color.colorPrimaryDark
                    )

                    set1.setDrawValues(true)
                    set1.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return getDuration(value.toLong())
                        }
                    }
                    val dataSets: MutableList<IBarDataSet> = ArrayList()
                    dataSets.add(set1)
                    val data = BarData(dataSets)
                    chart!!.data = data
                    chart!!.data.isHighlightEnabled = false
                    if (listCurrentWeekInterval.size > 6) {
                        data.barWidth = 0.1f
                        chart!!.setScaleMinima(
                            5f, 0f
                        )
                        chart!!.moveViewToAnimated(
                            listCurrentWeekInterval.size - 1.toFloat(),
                            0f, YAxis.AxisDependency.RIGHT, 10000
                        )
                    } else {
                        data.barWidth = 0.2f
                        chart!!.setScaleMinima(1f, 0f)
                    }
                    chart!!.setFitBars(true)
                }
                chart!!.invalidate()
            }
            Graph.DISTANCE -> {
                if (chart!!.data != null &&
                    chart!!.data.dataSetCount > 0
                ) {
                    val set1 = chart!!.data.getDataSetByIndex(0) as BarDataSet
                    set1.values = values
                    chart!!.data.notifyDataChanged()
                    chart!!.notifyDataSetChanged()
                } else {
                    val set1 = BarDataSet(values, "")
                    set1.color = ContextCompat.getColor(
                        view.context,
                        R.color.colorPrimaryDark
                    )

                    set1.setDrawValues(true)
                    set1.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "${AppUtils.roundTwoDecimal(value.toDouble())} km"
                        }
                    }
                    val dataSets: MutableList<IBarDataSet> = ArrayList()
                    dataSets.add(set1)
                    val data = BarData(dataSets)
                    chart!!.data = data
                    chart!!.data.isHighlightEnabled = false
                    if (listCurrentWeekInterval.size > 6) {
                        data.barWidth = 0.1f
                        chart!!.setScaleMinima(
                            5f, 0f
                        )
                        chart!!.moveViewToAnimated(
                            listCurrentWeekInterval.size - 1.toFloat(),
                            0f, YAxis.AxisDependency.RIGHT, 10000
                        )
                    } else {
                        data.barWidth = 0.2f
                        chart!!.setScaleMinima(1f, 0f)
                    }
                    chart!!.setFitBars(true)
                }
                chart!!.invalidate()
            }
            Graph.STEP -> {
                if (chart!!.data != null &&
                    chart!!.data.dataSetCount > 0
                ) {
                    val set1 = chart!!.data.getDataSetByIndex(0) as BarDataSet
                    set1.values = values
                    chart!!.data.notifyDataChanged()
                    chart!!.notifyDataSetChanged()
                } else {
                    val set1 = BarDataSet(values, "")
                    set1.color = ContextCompat.getColor(
                        view.context,
                        R.color.colorPrimaryDark
                    )

                    set1.setDrawValues(true)
                    set1.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "${value.toInt()}"
                        }
                    }
                    val dataSets: MutableList<IBarDataSet> = ArrayList()
                    dataSets.add(set1)
                    val data = BarData(dataSets)
                    chart!!.data = data
                    chart!!.data.isHighlightEnabled = false
                    if (listCurrentWeekInterval.size > 6) {
                        data.barWidth = 0.1f
                        chart!!.setScaleMinima(
                            5f, 0f
                        )
                        chart!!.moveViewToAnimated(
                            listCurrentWeekInterval.size - 1.toFloat(),
                            0f, YAxis.AxisDependency.RIGHT, 10000
                        )
                    } else {
                        data.barWidth = 0.2f
                        chart!!.setScaleMinima(1f, 0f)
                    }
                    chart!!.setFitBars(true)
                }
                chart!!.invalidate()
            }
            else -> {
            }
        }
    }

}