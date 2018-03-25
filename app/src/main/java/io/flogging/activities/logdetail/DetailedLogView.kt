package io.flogging.activities.logdetail

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import io.flogging.R
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import org.joda.time.DateTime

class DetailedLogView : AppCompatActivity() {

    fun compareUniqueKey(log: FloggingRow, uniqueKey: String): Boolean {
        return Flogging.createIndex(log).equals(uniqueKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_log_view)
        val prefs = Prefs(this)
        val uniqueKey = intent.getStringExtra("index")
        val vm = ViewModelProviders.of(this).get(LogViewModel::class.java)
        val user = prefs.uid

        vm.logs.subscribe {
            Log.d("DetailedLogView", "size: ${vm.logs.value.size}")
            val log = it.first { compareUniqueKey(it, uniqueKey) }
            initView(log, vm, prefs.activeProject, user)
        }
        vm.loadSpecificLogForProject(prefs.activeProject.projectName, user, uniqueKey)
    }

    private fun initOnDeleteButton(vm : LogViewModel,
                                   currentProject : FloggingProject,
                                   uid: String) {
        findViewById<Button>(R.id.detailed_log_view_button_delete).setOnClickListener {
            val ts = (findViewById<TextView>(R.id.detailed_log_view_timestamp) as TextView).text.toString()
            val start = (findViewById<TextView>(R.id.detailed_log_view_start) as TextView).text.toString()
            val end = (findViewById<TextView>(R.id.detailed_log_view_end) as TextView).text.toString()

            val timestamp = DateTime.parse(ts, Flogs.YYYY_MM_DD_PATTERN)
            val startDate = DateTime.parse(start, Flogs.HH_MM_PATTERN)
            val endDate = DateTime.parse(end, Flogs.HH_MM_PATTERN)
            val log = FloggingRow(timestamp, startDate, endDate, 0, "", FloggingRow.Status.WORKED, "")
            vm.deleteLog(currentProject, uid, log, { _, _ ->
                vm.omitCurrentLogs()
                finish()
            })
        }
    }

    private fun initView(log: FloggingRow, vm: LogViewModel,
                         project_name: FloggingProject, uid: String) {
        (findViewById<TextView>(R.id.detailed_log_view_timestamp) as TextView).text =
                log.timestamp.toString(Flogs.YYYY_MM_DD_PATTERN)
        (findViewById<TextView>(R.id.detailed_log_view_start) as TextView).text =
                log.startDate.toString(Flogs.HH_MM_PATTERN)
        (findViewById<TextView>(R.id.detailed_log_view_end) as TextView).text =
                log.endDate.toString(Flogs.HH_MM_PATTERN)
        (findViewById<TextView>(R.id.detailed_log_view_decimal) as TextView).text =
                log.decimal
        (findViewById<TextView>(R.id.detailed_log_view_note) as TextView).text =
                log.note
        val k = findViewById<Spinner>(R.id.detailed_log_view_type_of_log)
        k.setSelection(log.status.ordinal)
        k.isClickable = false

        initOnDeleteButton(vm, project_name, uid)

    }

}
