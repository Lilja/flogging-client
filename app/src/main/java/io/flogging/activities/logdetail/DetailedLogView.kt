package io.flogging.activities.logdetail

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import io.flogging.R
import io.flogging.activities.activityrequests.ActivityRequestCodes
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import org.joda.time.DateTime

class DetailedLogView : AppCompatActivity() {

    private fun compareUniqueKey(log: FloggingRow, uniqueKey: String): Boolean {
        return Flogging.createIndex(log) == uniqueKey
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detailed_log_view)
        val prefs = Prefs(this)
        val oldUniqueKey = intent.getStringExtra("index")
        val vm = ViewModelProviders.of(this).get(LogViewModel::class.java)
        val user = prefs.uid

        vm.logs.subscribe {
            Log.d("DetailedLogView", it.toString())
            val log = it.first { compareUniqueKey(it, oldUniqueKey) }
            initView(log, vm, prefs.activeProject, user, oldUniqueKey)
        }
        vm.loadSpecificLogForProject(prefs.activeProject.projectName, user, oldUniqueKey)
    }

    private fun initOnDeleteButton(vm: LogViewModel,
                                   currentProject: FloggingProject,
                                   uid: String) {
        findViewById<Button>(R.id.detailed_log_view_button_delete).setOnClickListener {
            val ts = (findViewById<TextView>(R.id.detailed_log_view_timestamp) as TextView).text.toString()
            val start = (findViewById<TextView>(R.id.detailed_log_view_start) as EditText).text.toString()
            val end = (findViewById<TextView>(R.id.detailed_log_view_end) as EditText).text.toString()

            val timestamp = DateTime.parse(ts, Flogs.YYYY_MM_DD_PATTERN)
            val startDate = DateTime.parse(start, Flogs.HH_MM_PATTERN)
            val endDate = DateTime.parse(end, Flogs.HH_MM_PATTERN)
            val log = FloggingRow(timestamp, startDate, endDate, 0, "", FloggingRow.Status.WORKED, "")

            val alert = AlertDialog.Builder(this).create()
            alert.setTitle("Delete a log")
            alert.setMessage("Are you REALLY sure you want to delete the entry for $ts?")

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", { _, _ -> })
            alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", { _, _ ->
                vm.deleteLog(currentProject, uid, log, { _, _ ->
                    setResult(ActivityRequestCodes.DETAILED_LOG_DELETED)
                    finish()
                })
            })
            alert.show()

        }
    }

    private fun initView(log: FloggingRow, vm: LogViewModel,
                         project_name: FloggingProject, uid: String, oldUniqueKey: String) {
        (findViewById<TextView>(R.id.detailed_log_view_timestamp) as TextView).text =
                log.timestamp.toString(Flogs.YYYY_MM_DD_PATTERN)
        val startTimeView = (findViewById<TextView>(R.id.detailed_log_view_start) as EditText)
        startTimeView.setText(
                log.startDate.toString(Flogs.HH_MM_PATTERN),
                TextView.BufferType.EDITABLE)
        startTimeView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                recalculateDiff()
            }
        })
        val endTimeView = (findViewById<TextView>(R.id.detailed_log_view_end) as EditText)
        endTimeView.setText(
                log.endDate.toString(Flogs.HH_MM_PATTERN),
                TextView.BufferType.EDITABLE)
        endTimeView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                recalculateDiff()
            }
        })
        val breakTimeView = (findViewById<TextView>(R.id.detailed_log_view_break) as EditText)
        breakTimeView.setText(log.breakMinutes.toString(), TextView.BufferType.EDITABLE)
        breakTimeView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                recalculateDiff()
            }
        })
        (findViewById<TextView>(R.id.detailed_log_view_decimal) as TextView).text =
                log.decimal
        (findViewById<TextView>(R.id.detailed_log_view_note) as TextView).text =
                log.note
        val k = findViewById<Spinner>(R.id.detailed_log_view_type_of_log)
        k.setSelection(log.status.ordinal)
        k.isClickable = false

        initOnDeleteButton(vm, project_name, uid)
        update(vm, project_name, uid, log, oldUniqueKey)
    }

    private fun recalculateDiff() {
        try {
            val startString = (findViewById<EditText>(R.id.detailed_log_view_start) as EditText).text.toString()
            val endString = (findViewById<EditText>(R.id.detailed_log_view_end) as EditText).text.toString()
            val breakString = (findViewById<EditText>(R.id.detailed_log_view_break) as EditText).text.toString().toInt()
            val diff = Flogging.calculateDiff(startString, endString, breakString)

            (findViewById<TextView>(R.id.detailed_log_view_decimal) as TextView)
                    .text = Flogs.minutesToHHMM(diff)
        } catch (e : IllegalArgumentException) {

        }
    }

    private fun update(vm: LogViewModel,
                       project_name: FloggingProject,
                       uid: String,
                       log: FloggingRow,
                       oldUniqueKey: String) {
        findViewById<Button>(R.id.detailed_log_view_button_update)
                .setOnClickListener {

                    val startString = (findViewById<EditText>(R.id.detailed_log_view_start) as EditText).text.toString()
                    val startDate = DateTime.parse(startString, Flogs.HH_MM_PATTERN)

                    val endString = (findViewById<EditText>(R.id.detailed_log_view_end) as EditText).text.toString()
                    val endDate = DateTime.parse(endString, Flogs.HH_MM_PATTERN)

                    val breakMinutes = (findViewById<EditText>(R.id.detailed_log_view_break) as EditText).text.toString().toInt()
                    try {
                        val diff = Flogs.minutesToHHMM(Flogging.calculateDiff(startString, endString, breakMinutes))
                        val typeOfLog = findViewById<Spinner>(R.id.detailed_log_view_type_of_log)
                                .selectedItem
                                .toString()
                        val noteText = (findViewById<EditText>(R.id.detailed_log_view_note) as EditText).text.toString()

                        val newLog = FloggingRow(log.timestamp, startDate, endDate, breakMinutes, diff, typeOfLog, noteText)
                        val uniqueKey = Flogging.createIndex(newLog)

                        vm.updateLog(project_name, uid, oldUniqueKey, uniqueKey, newLog, { a, m ->
                            Log.d("DetailedLogView", "Success: $a $m")
                            if (a) {
                                setResult(ActivityRequestCodes.DETAILED_LOG_UPDATED)
                                finish()
                            }
                        })
                    } catch (e: IllegalArgumentException) {
                        // Silent catch because it's good
                    }


                }
    }


}
