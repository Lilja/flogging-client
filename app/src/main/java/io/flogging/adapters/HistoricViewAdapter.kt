package io.flogging.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.flogging.R
import io.flogging.activities.logdetail.DetailedLogView
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs

class HistoricViewAdapter(private val vm : LogViewModel,
                          private val currentProject: FloggingProject,
                          private val context: Context,
                          private val logs: List<Pair<Int, FloggingRow>>) :
        RecyclerView.Adapter<HistoricViewAdapter.ViewHolder>() {

    class ViewHolder(val relativeLayout: RelativeLayout) : RecyclerView.ViewHolder(relativeLayout)

    private fun startDetailedActivity(root: View) {
        val intent = Intent(context, DetailedLogView::class.java)
        Log.d("PrintRecordOnclick", (root.findViewById<TextView>(R.id.selector) as TextView).text.toString())
        intent.putExtra("index",
                (root.findViewById<TextView>(R.id.selector) as TextView).text)
        context.startActivity(intent)
    }

    private fun populateEntry(linearLayout: RelativeLayout,
                              rowWithDecimal: Pair<Int, FloggingRow>): RelativeLayout {
        val floggingRow = rowWithDecimal.second

        val hiddenText = linearLayout.findViewById<TextView>(R.id.selector)
        val uniqueKey = Flogging.createIndex(floggingRow)
        hiddenText.text = uniqueKey

        linearLayout.setOnClickListener {
            startDetailedActivity(linearLayout)
        }

        // Date
        val timestamp = floggingRow.timestamp.toString(Flogs.YYYY_MM_DD_PATTERN)
        linearLayout.findViewById<TextView>(R.id.historic_view_date)
                .text = timestamp

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
            d.background = ContextCompat.getDrawable(context, R.mipmap.ic_work_black_24dp)
        } else if (floggingRow.status == FloggingRow.Status.FLEX_TIME_OFF) {
            d.background = ContextCompat.getDrawable(context, R.mipmap.ic_shuffle_black_24dp)
        } else if (floggingRow.status == FloggingRow.Status.PAID_LEAVE) {
            d.background = ContextCompat.getDrawable(context, R.mipmap.ic_card_travel_black_24dp)
        } else if (floggingRow.status == FloggingRow.Status.PUBLIC_HOLIDAY) {
            d.background = ContextCompat.getDrawable(context, R.mipmap.ic_public_black_24dp)
        }

        val diffText = linearLayout.findViewById<TextView>(R.id.diff_time)
        val res = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(rowWithDecimal.first))
        if ("-" in res) {
            diffText.setTextColor(ContextCompat.getColor(context, R.color.red))
        } else
            diffText.setTextColor(ContextCompat.getColor(context, R.color.green))

        diffText.text = res

        return linearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): HistoricViewAdapter.ViewHolder {
        val textView = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.historic_view_entry, parent, false) as RelativeLayout
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = logs[position]
        populateEntry(holder.relativeLayout, entry)
    }

    override fun getItemCount(): Int {
        return logs.size
    }

}
