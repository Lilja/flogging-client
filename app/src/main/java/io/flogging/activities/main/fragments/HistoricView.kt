package io.flogging.activities.main.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
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
import io.reactivex.subjects.BehaviorSubject
import org.joda.time.DateTime

class HistoricView : Fragment() {
    var vm: LogViewModel? = null
    var sub: Disposable? = null

    override fun onStart() {
        val root = view!! as RelativeLayout
        val prefs = Prefs(activity)

        Log.d("HistoricView", "onStart")
        sub = vm!!.logs.subscribe { rows ->
            Log.d("HistoricView", "Listener received events")
            subscriptionListener(root, rows, prefs, activity)
        }
        super.onStart()
    }

    private fun subscriptionListener(root: RelativeLayout,
                                     rows: List<FloggingRow>,
                                     prefs: Prefs,
                                     activity: Activity) {
        displayLoading(root)
        Thread(Runnable {
            val logs = vm!!.getLogsWithDiff(rows, prefs.activeProject)

            // Set the type of log, ideally we should do this with options(scala). But kotlin does
            // not  have any native options. So nullable it is.
            // If it's null, that means "all" is selected. Else, go ahead and filter on that
            // condition
            val typeOfLogs: FloggingRow.Status? = if (prefs.typeOfLog != "") {
                FloggingRow.Status.valueOf(prefs.typeOfLog)
            } else {
                null
            }

            val noteTextFilter = prefs.noteText

            val sortedAndFilteredLogs = if (prefs.chronologicalOrder) {
                logs.sortedBy { it.second.timestamp }
            } else {
                logs.sortedByDescending { it.second.timestamp }
            }.filter {
                typeOfLogs == null || it.second.status == typeOfLogs
            }.filter {
                noteTextFilter == "" || it.second.note.contains(noteTextFilter)
            }

            val startFilter = prefs.startDate
            val endFilter = prefs.endDate

            // Run the logs through a filter of startDate and endDate
            val filteredLogs = sortedAndFilteredLogs.filter {
                it.second.timestamp in startFilter..endFilter
            }

            activity.runOnUiThread {
                val observable: BehaviorSubject<List<Pair<Int, FloggingRow>>> = BehaviorSubject.create()
                val recycleView = root.findViewById<RecyclerView>(R.id.recyclerView)
                val viewAdapter = HistoricViewAdapter(vm!!, prefs.activeProject, activity, observable)

                val viewMng = LinearLayoutManager(activity)
                viewAdapter.notifyDataSetChanged()

                recycleView.apply {
                    setHasFixedSize(true)
                    layoutManager = viewMng
                    adapter = viewAdapter
                }
                observable.onNext(filteredLogs)
                setTotalDiff(root.findViewById(R.id.total_hh_mm_flex_diff), filteredLogs)
                hideLoading(root)
            }
        }).start()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("HistoricView", "OnCreateView called")
        val root = inflater!!.inflate(R.layout.historic_view, container, false) as RelativeLayout
        val prefs = Prefs(activity)

        val filterRoot = root.findViewById<LinearLayout>(R.id.historic_view_filter_layout)
        filterRoot.measure(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = filterRoot.measuredHeight

        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)

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

    override fun onStop() {
        if (sub != null) {
            Log.d("HistoricView", "Disposing observable")
            sub!!.dispose()
        }
        super.onStop()
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

    private fun translateToInternal(localized: String): String {
        // TODO: fix this shitty translation, only works for english
        return localized.toUpperCase().replace(" ", "_")
    }

    private fun translateFromInternal(internal: String): String {
        // TODO: fix this shitty translation, only works for english
        return internal.toUpperCase().replace(" ", "_")
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
                    prefs.chronologicalOrder = b
                    vm!!.omitCurrentLogs()
                }
        root.findViewById<EditText>(R.id.historic_view_filter_note_text)
                .setText(prefs.noteText, TextView.BufferType.EDITABLE)

        val elem = root.findViewById<Spinner>(R.id.historic_view_filter_type_of_log)
        val maybeLog = prefs.typeOfLog
        Log.d("HistoricView", "Getting from prefs and setting: '$maybeLog'")
        val menuType = if (maybeLog == "") {
            0
        } else {
            (FloggingRow.Status.fromValue(translateToInternal(maybeLog)).ordinal + 1)
        }
        elem.setSelection(menuType, false)

        elem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.d("OnItemSelected", "Clicked!")
                val item = p0!!.getItemAtPosition(p2).toString()
                Log.d("HistoricView", "Setting $item")
                prefs.typeOfLog = if (item == "All") {
                    ""
                } else {
                    translateToInternal(item)
                }
                vm?.omitCurrentLogs()
            }
        }

        setStartDateText()
        setEndDateText()
        val startDateView = root.findViewById<ImageView>(R.id.historic_view_filter_start_date)

        startDateView.setOnClickListener {
            val startD = prefs.startDate

            val dpYear = startD.year
            val dpMonth = startD.monthOfYear - 1 // subtract with 1 because of DatePicker API
            val dpDay = startD.dayOfMonth

            DatePickerDialog(activity, { _: DatePicker,
                                         year: Int,
                                         month: Int,
                                         day: Int ->

                val zeroedMonth = if ((month + 1) < 10) "0" + (month + 1) else (month + 1).toString()
                val zeroedDay = if (day < 10) "0" + day else day.toString()
                root.findViewById<EditText>(R.id.historic_view_filter_start_date_text)
                        .setText(year.toString() + "-" + zeroedMonth + "-" + zeroedDay,
                                TextView.BufferType.EDITABLE)
                prefs.startDate = DateTime(year, zeroedMonth.toInt(), day, 0, 0, 0, 0)
                setStartDateText()
                vm!!.omitCurrentLogs()

            }, dpYear, dpMonth, dpDay).show()
        }
        root.findViewById<ImageView>(R.id.historic_view_filter_end_date)
                .setOnClickListener {
                    val endD = prefs.endDate

                    val dpYear = endD.year
                    val dpMonth = endD.monthOfYear - 1 // subtract with 1 because of DatePicker API
                    val dpDay = endD.dayOfMonth

                    DatePickerDialog(activity, { _: DatePicker,
                                                 year: Int,
                                                 month: Int,
                                                 day: Int ->

                        val zeroedMonth = if ((month + 1) < 10) "0" + (month + 1) else (month + 1).toString()
                        val zeroedDay = if (day < 10) "0" + day else day.toString()
                        root.findViewById<EditText>(R.id.historic_view_filter_end_date_text)
                                .setText(year.toString() + "-" + zeroedMonth + "-" + zeroedDay,
                                        TextView.BufferType.EDITABLE)
                        prefs.endDate = DateTime(year, zeroedMonth.toInt(), day, 0, 0, 0, 0)
                        setEndDateText()
                        vm!!.omitCurrentLogs()

                    }, dpYear, dpMonth, dpDay).show()
                }

        root.findViewById<EditText>(R.id.historic_view_filter_note_text)
                .addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        prefs.noteText = p0.toString()
                        vm!!.omitCurrentLogs()
                    }

                    override fun afterTextChanged(p0: Editable?) {}
                })
    }

    private fun displayLoading(root: RelativeLayout) {
        root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.VISIBLE
    }

    private fun hideLoading(root: RelativeLayout) {
        root.findViewById<ProgressBar>(R.id.historic_view_load).visibility = ProgressBar.GONE
    }

}
