package io.flogging.activities.project

import android.app.TimePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import io.flogging.R
import io.flogging.activities.activityrequests.ActivityRequestCodes
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.model.FloggingProject
import io.flogging.util.Prefs

class NewProject : AppCompatActivity() {

    private var vm: LogViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_project)

        vm = ViewModelProviders.of(this).get(LogViewModel::class.java)
        val prefs = Prefs(this)
        initView(prefs)
    }

    private fun initView(prefs: Prefs) {
        findViewById<Button>(R.id.new_project_pick_time)
                .setOnClickListener {
                    TimePickerDialog(this, { _: TimePicker,
                                             hour: Int,
                                             minutes: Int ->

                        val a = if (hour < 10) "0" + hour.toString() else hour.toString()
                        val b = if (minutes < 10) "0" + minutes.toString() else minutes.toString()
                        findViewById<EditText>(R.id.new_project_daily_hour)
                                .setText(a + ":" + b,
                                        TextView.BufferType.EDITABLE)

                    }, 8, 0, true).show()
                }

        findViewById<Button>(R.id.new_project_save).setOnClickListener {
            val projName = (findViewById<EditText>(R.id.new_project_name) as EditText).text.toString()
            val dailyHourAndMinute = (findViewById<EditText>(R.id.new_project_daily_hour) as EditText).text.toString()
            val dhm = (if (dailyHourAndMinute.isEmpty()) "08:00" else dailyHourAndMinute).split(":")

            vm!!.createProject(projName, dhm[0], dhm[1], prefs.uid, { success, message ->
                if (success) {
                    Log.d("NewProjectSave", "Success")
                    val p = Prefs(this)
                    p.activeProject = FloggingProject(projName, dhm[0], dhm[1])
                    setResult(ActivityRequestCodes.NEW_LOG_PROJECT_SUCCESS)
                } else {
                    Log.d("NewProjectSave", "Fail")
                    setResult(ActivityRequestCodes.NEW_LOG_PROJECT_FAILURE)
                }
                finish()
            })
        }
    }
}
