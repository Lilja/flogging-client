package io.flogging

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import org.joda.time.DateTime

class HistoricView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.historic_view)

        val ll = findViewById<LinearLayout>(R.id.output)
        val list = Flogging.getLogsWithDiff(listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("14:00", Flogs.HH_MM_PATTERN),
                        0,
                       "06:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("14:30", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "02:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-05", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("17:00", Flogs.HH_MM_PATTERN),
                        30,
                       "08:30",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-06", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("17:00", Flogs.HH_MM_PATTERN),
                        0,
                       "09:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-07", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        0,
                       "07:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-08", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("09:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("10:00", Flogs.HH_MM_PATTERN),
                        0,
                       "01:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-08", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("10:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("17:00", Flogs.HH_MM_PATTERN),
                        0,
                       "07:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-08", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("17:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("18:00", Flogs.HH_MM_PATTERN),
                        0,
                       "01:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                )

        ))
        printRecord(ll, list)
        setTotalDiff(findViewById(android.R.id.content), list)
    }

     fun getDiff(listOfRowWithDecimal: List<Pair<Int, FloggingRow>>): String {
        val minutes = listOfRowWithDecimal
                .filter { it.second.timestamp.millis < DateTime.now().millis }
                .sumBy { it.first }
        return Flogs.hhMMWithDiff(Flogs.minutesToHHMM(minutes))
    }

    fun printRecord(linearLayout: LinearLayout, listOfRowWithDecimal: List<Pair<Int, FloggingRow>>) {
        listOfRowWithDecimal.groupBy {
            it.second.timestamp.toString("dd")
        }.map {
            val headerLayout = layoutInflater.inflate(R.layout.historic_view_date, null) as LinearLayout
            populateHeader(headerLayout, it.value[0])
            val stuff = it.value.map {
                val newLayout = layoutInflater.inflate(R.layout.historic_view_entry, null)
                record(newLayout as LinearLayout, it)
            }
            listOf<LinearLayout>() + headerLayout + stuff
        }.forEach {
            it.forEach{
                linearLayout.addView(it)
            }
        }
    }

    fun setTotalDiff(linearLayout: View, listOfRowWithDecimal: List<Pair<Int, FloggingRow>>) {
        val text = linearLayout.findViewById<TextView>(R.id.big_meme)
        val hhmm = getDiff(listOfRowWithDecimal)
        if ("-" in hhmm) {
            text.setTextColor(ContextCompat.getColor(this@HistoricView, R.color.red))
        }
        text.text = hhmm
    }

    fun populateHeader(linearLayout: LinearLayout, rowWithDecimal: Pair<Int, FloggingRow>) {
        val t = linearLayout.findViewById<TextView>(R.id.header_date)
        t.text = rowWithDecimal.second.timestamp.toString(Flogs.HEADER_PATTERN)
    }

    fun record(linearLayout: LinearLayout, rowWithDecimal: Pair<Int, FloggingRow>): LinearLayout {
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
        if(floggingRow.status == FloggingRow.Status.FLEX_TIME_OFF) {
            d.setBackgroundColor(ContextCompat.getColor(this@HistoricView, R.color.yellow))
        } else if(floggingRow.status == FloggingRow.Status.PAID_LEAVE) {
            d.setBackgroundColor(ContextCompat.getColor(this@HistoricView, R.color.red))
        }

        val diffText = linearLayout.findViewById<TextView>(R.id.diff_time)
        val res = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(rowWithDecimal.first))
        if ("-" in res) {
            diffText.setTextColor(ContextCompat.getColor(this@HistoricView, R.color.red))
        }
        diffText.text = res

        return linearLayout
    }
}
