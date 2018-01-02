package io.flogging.activities.main.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import io.flogging.R
import io.flogging.activities.DetailedLogView
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import org.joda.time.DateTime

class HistoricView : Observee() {

    override fun onSomethingChanged(projectName : String) {
        super.onSomethingChanged(projectName)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("HistoricView","OnCreateView called")
        val root = inflater!!.inflate(R.layout.historic_view, container, false) as LinearLayout
        val prefs = Prefs(activity)
        val projectName = prefs.activeProject.projectName
        val uid = FirebaseAuth.getInstance().uid.toString()

        if (projectName.isNotEmpty()) {
            Flogging.getLogsForProject(prefs.activeProject.projectName, uid, { rows ->
                Log.d("OnCreateView", "${prefs.activeProject.projectName} $uid")
                root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.GONE
                val ll = root.findViewById<LinearLayout>(R.id.output)
                val logs = Flogging.getLogsWithDiff(rows)
                printRecord(ll, logs)
                setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), logs)
            })
        } else {
            setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), listOf())
        }
        val timestamp = DateTime.now()
        val listener = prefs.configureListener {
            data ->
            Log.d("OnPrefsChanged","changed. I was created at ${timestamp.millis}")
            if (this@HistoricView.isVisible) {
                Flogging.getLogsForProject(data, uid, { rows ->
                    root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.GONE
                    val ll = root.findViewById<LinearLayout>(R.id.output)
                    ll.removeAllViews()
                    val logs = Flogging.getLogsWithDiff(rows)
                    printRecord(ll, logs)
                    setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), logs)
                })
            }
        }
        prefs.registerListener(listener)
        return root
    }

    private fun getDiff(minutes: Int): String {
        return Flogs.hhMMWithDiff(Flogs.minutesToHHMM(minutes))
    }

    private fun printRecord(linearLayout: LinearLayout,
                            listOfRowWithDecimal: List<Pair<Int, FloggingRow>>) {
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
}
