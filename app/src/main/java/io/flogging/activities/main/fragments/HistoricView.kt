package io.flogging.activities.main.fragments

import android.app.DatePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import io.flogging.R
import io.flogging.activities.logdetail.DetailedLogView
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import java.util.*

class HistoricView : Fragment() {
    var vm: LogViewModel? = null
    var sub: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("HistoricView", "OnCreateView called")
        val root = inflater!!.inflate(R.layout.historic_view, container, false) as RelativeLayout
        val prefs = Prefs(activity)
        val uid = FirebaseAuth.getInstance().uid.toString()

        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)
        sub = vm!!.logs.subscribe { rows ->
            Log.d("Logs subscriber", "${prefs.activeProject.projectName} $uid")
            root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.GONE
            Thread(Runnable {
                val ll = root.findViewById<LinearLayout>(R.id.output)
                val logs = vm!!.getLogsWithDiff(rows, prefs.activeProject)
                Log.d("Logs subscriber", prefs.chronoicalOrder.toString())
                Log.d("Logs subscriber", logs.map { it.second }.toString())
                val sortedLogs = if (prefs.chronoicalOrder) logs else logs.reversed()
                Log.d("Logs subscriber", sortedLogs.map { it.second }.toString())
                val startFilter = prefs.startDate
                val endFilter = prefs.endDate

                // Run the logs through a filter of startDate and endDate
                val filteredLogs = sortedLogs.filter { it.second.timestamp in startFilter..endFilter }

                activity.runOnUiThread {
                    val ts = DateTime.now()
                    printRecord(ll, filteredLogs)
                    setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), logs)
                    val ts2 = DateTime.now()
                    Log.d("Historic", "Rendering took " + (ts2.millis - ts.millis) + " ms")
                }
            }).start()
        }

        val filterRoot = root.findViewById<LinearLayout>(R.id.historic_view_filter_layout)
        filterRoot.measure(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = filterRoot.measuredHeight

        root.findViewById<ImageView>(R.id.historic_view_filter_show)
                .setOnClickListener {

                    if (filterRoot.height > 0) {
                        val anim = object : Animation() {
                            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                                filterRoot.layoutParams.height = targetHeight - (targetHeight * interpolatedTime).toInt()
                                filterRoot.requestLayout()
                            }

                        }
                        anim.duration = 400
                        view!!.startAnimation(anim)
                    } else {
                        val anim = object : Animation() {
                            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                                filterRoot.layoutParams.height = (targetHeight * interpolatedTime).toInt()
                                filterRoot.requestLayout()
                            }
                        }
                        anim.duration = 400
                        view!!.startAnimation(anim)
                    }
                }

        setupFilterFunctions(prefs, root)
        return root
    }

    override fun onPause() {
        if (sub != null) {
            sub!!.dispose()
        }
        super.onPause()
    }

    private fun getDiff(minutes: Int): String {
        return Flogs.hhMMWithDiff(Flogs.minutesToHHMM(minutes))
    }

    private fun printRecord(linearLayout: LinearLayout,
                            listOfRowWithDecimal: List<Pair<Int, FloggingRow>>) {
        if (linearLayout.childCount > 0)
            linearLayout.removeAllViews()
        listOfRowWithDecimal.groupBy {
            it.second.timestamp.toString("MM-dd")
        }.map {
            val headerLayout = layoutInflater.inflate(R.layout.historic_view_date, null)
                    as LinearLayout
            populateHeader(headerLayout, it.value[0])
            val stuff = it.value.map {
                Log.d("PrintRecord", it.second.timestamp.toString())
                val newLayout = layoutInflater.inflate(R.layout.historic_view_entry, null)
                newLayout.setOnClickListener {
                    val intent = Intent(activity, DetailedLogView::class.java)
                    Log.d("PrintRecordOnclick", (it.findViewById<TextView>(R.id.selector) as TextView).text.toString())
                    intent.putExtra("index",
                            (it.findViewById<TextView>(R.id.selector) as TextView).text)
                    startActivity(intent)
                }
                record(newLayout as LinearLayout, it)
            }
            listOf<LinearLayout>() + headerLayout + stuff
        }.forEach {
            it.forEach {
                linearLayout.addView(it)
            }
        }
    }

    private fun setTotalDiff(linearLayout: View,
                             listOfRowWithDecimal: List<Pair<Int, FloggingRow>>) {
        val text = linearLayout.findViewById<TextView>(R.id.total_hh_mm_flex_diff)
        if (listOfRowWithDecimal.isNotEmpty()) {
            val hhmm = getDiff(listOfRowWithDecimal.last().first)
            if ("-" in hhmm) {
                text.setTextColor(ContextCompat.getColor(activity, R.color.red))
            }
            text.text = hhmm
        } else {
            text.text = ""

        }
    }

    private fun populateHeader(linearLayout: LinearLayout,
                               rowWithDecimal: Pair<Int, FloggingRow>) {
        val t = linearLayout.findViewById<TextView>(R.id.header_date)
        t.text = rowWithDecimal.second.timestamp.toString(Flogs.HEADER_PATTERN)
    }

    private fun record(linearLayout: LinearLayout,
                       rowWithDecimal: Pair<Int, FloggingRow>): LinearLayout {
        val floggingRow = rowWithDecimal.second

        val hiddenText = linearLayout.findViewById<TextView>(R.id.selector)
        val asdf = floggingRow.timestamp.toString(Flogs.YYYY_MM_DD_PATTERN) + " " +
                floggingRow.startDate.toString(Flogs.HH_MM_PATTERN) + " " +
                floggingRow.endDate.toString(Flogs.HH_MM_PATTERN)
        hiddenText.text = asdf

        Log.d("printRecord", floggingRow.toString())

        val startTime = linearLayout.findViewById<TextView>(R.id.start_time)
        startTime.text = floggingRow.startDate.toString(Flogs.HH_MM_PATTERN)

        val endTime = linearLayout.findViewById<TextView>(R.id.end_time)
        endTime.text = floggingRow.endDate.toString(Flogs.HH_MM_PATTERN)

        val breakTime = linearLayout.findViewById<TextView>(R.id.break_time)
        breakTime.text = floggingRow.breakMinutes.toString()

        val decimalText = linearLayout.findViewById<TextView>(R.id.decimal_time)
        decimalText.text = floggingRow.decimal

        if (!Flogs.isWorkingDay(floggingRow.timestamp)) {
            val dbgText = linearLayout.findViewById<TextView>(R.id.debug_non_workday)
            dbgText.visibility = TextView.VISIBLE
        }
        val d = linearLayout.findViewById<ImageView>(R.id.status)
        if (floggingRow.status == FloggingRow.Status.FLEX_TIME_OFF) {
            d.setBackgroundColor(ContextCompat.getColor(activity, R.color.yellow))
        } else if (floggingRow.status == FloggingRow.Status.PAID_LEAVE) {
            d.setBackgroundColor(ContextCompat.getColor(activity, R.color.red))
        }

        val diffText = linearLayout.findViewById<TextView>(R.id.diff_time)
        val res = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(rowWithDecimal.first))
        if ("-" in res) {
            diffText.setTextColor(ContextCompat.getColor(activity, R.color.red))
        }
        diffText.text = res

        return linearLayout
    }

    private fun setupFilterFunctions(prefs: Prefs, root: RelativeLayout) {
        fun setStartDateText() {
            val startDateText = root.findViewById<EditText>(R.id.historic_view_filter_start_date_text)
            startDateText
                    .setText(prefs.startDate.toString(Flogs.YYYY_MM_DD_PATTERN), TextView.BufferType.EDITABLE)
        }

        fun setEndDateText() {
            val endDateText = root.findViewById<EditText>(R.id.historic_view_filter_end_date_text)
            endDateText
                    .setText(prefs.endDate.toString(Flogs.YYYY_MM_DD_PATTERN), TextView.BufferType.EDITABLE)
        }
        root.findViewById<CheckBox>(R.id.historic_view_filter_chronological_order)
                .setOnCheckedChangeListener { a: CompoundButton?, b: Boolean ->
                    Log.d("Prefs", "Setting chronological order to " + b.toString() + " " + DateTime.now())
                    prefs.chronoicalOrder = b
                    vm!!.omitCurrentLogs()
                }

        setStartDateText()
        setEndDateText()
        val startDate = root.findViewById<ImageView>(R.id.historic_view_filter_start_date)

        startDate
                .setOnClickListener {
                    val c = Calendar.getInstance()
                    DatePickerDialog(activity, { _: DatePicker,
                                                 year: Int,
                                                 month: Int,
                                                 day: Int ->

                        val zeroedMonth = if ((month + 1) < 10) "0" + (month + 1) else month.toString()
                        val zeroedDay = if (day < 10) "0" + day else day.toString()
                        root.findViewById<EditText>(R.id.historic_view_filter_start_date_text)
                                .setText(year.toString() + "-" + zeroedMonth + "-" + zeroedDay,
                                        TextView.BufferType.EDITABLE)
                        prefs.startDate = DateTime(year, zeroedMonth.toInt(), day, 0, 0, 0, 0)
                        setStartDateText()
                        vm!!.omitCurrentLogs()

                    }, c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)).show()
                }
        root.findViewById<ImageView>(R.id.historic_view_filter_end_date)
                .setOnClickListener {
                    val c = Calendar.getInstance()
                    DatePickerDialog(activity, { _: DatePicker,
                                                 year: Int,
                                                 month: Int,
                                                 day: Int ->

                        val zeroedMonth = if ((month + 1) < 10) "0" + (month + 1) else month.toString()
                        val zeroedDay = if (day < 10) "0" + day else day.toString()
                        root.findViewById<EditText>(R.id.historic_view_filter_end_date_text)
                                .setText(year.toString() + "-" + zeroedMonth + "-" + zeroedDay,
                                        TextView.BufferType.EDITABLE)
                        prefs.endDate = DateTime(year, zeroedMonth.toInt(), day, 0, 0, 0, 0)
                        setEndDateText()
                        vm!!.omitCurrentLogs()

                    }, c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)).show()
                }
    }
}
