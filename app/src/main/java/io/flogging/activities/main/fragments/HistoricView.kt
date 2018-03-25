package io.flogging.activities.main.fragments

import android.app.DatePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import io.flogging.R
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.adapters.HistoricViewAdapter
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

        val viewMng = LinearLayoutManager(activity)

        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)
        val uid = prefs.uid

        sub = vm!!.logs.subscribe { rows ->
            Log.d("Logs subscriber", "${prefs.activeProject.projectName} $uid")
            displayLoading(root)
            Thread(Runnable {
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
                    val viewAdapter = HistoricViewAdapter(vm!!, prefs.activeProject, activity, filteredLogs)

                    val recycleView = root.findViewById<RecyclerView>(R.id.recyclerView)
                    viewAdapter.notifyDataSetChanged()
                    recycleView.apply {
                        setHasFixedSize(true)
                        layoutManager = viewMng
                        adapter = viewAdapter
                    }
                    setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), filteredLogs)
                    val ts2 = DateTime.now()
                    hideLoading(root)
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

    private fun setupFilterFunctions(prefs: Prefs, root: RelativeLayout) {
        fun setStartDateText() {
            root.findViewById<EditText>(R.id.historic_view_filter_start_date_text)
                    .setText(prefs.startDate.toString(Flogs.YYYY_MM_DD_PATTERN),
                            TextView.BufferType.EDITABLE)
        }

        fun setEndDateText() {
            root.findViewById<EditText>(R.id.historic_view_filter_end_date_text)
                    .setText(prefs.endDate.toString(Flogs.YYYY_MM_DD_PATTERN),
                            TextView.BufferType.EDITABLE)
        }
        root.findViewById<CheckBox>(R.id.historic_view_filter_chronological_order)
                .setOnCheckedChangeListener { a: CompoundButton?, b: Boolean ->
                    Log.d("Prefs", "Setting chronological order to " + b.toString() + " " + DateTime.now())
                    prefs.chronoicalOrder = b
                    vm!!.omitCurrentLogs()
                }

        setStartDateText()
        setEndDateText()
        val startDateView = root.findViewById<ImageView>(R.id.historic_view_filter_start_date)

        startDateView.setOnClickListener {
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

    private fun displayLoading(root: RelativeLayout) {
        root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.VISIBLE
    }

    private fun hideLoading(root: RelativeLayout) {
        root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.GONE
    }

}
