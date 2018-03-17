package io.flogging.activities.project

import android.app.TimePickerDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import io.flogging.R
import io.flogging.activities.activityrequests.ActivityRequestCodes
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.util.Prefs

class NewProject : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project)
        initView()
    }

    fun initView() {
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
            val user = FirebaseAuth.getInstance().currentUser ?: throw IllegalStateException("What have you gotten yourself into")

            Flogging.createProject(
                    projName,
                    dhm[0],
                    dhm[1],
                    user.uid,
                    { success ->
                        if(success) {
                            Log.d("NewProjectSave", "Success")
                            val p = Prefs(this)
                            p.activeProject = FloggingProject(projName, dhm[0], dhm[1])
                            setResult(ActivityRequestCodes.NEW_LOG_PROJECT_SUCCESS)
                        } else {
                            Log.d("NewProjectSave", "Fail")
                            setResult(ActivityRequestCodes.NEW_LOG_PROJECT_FAILURE)
                        }
                        finish()

                    }
            )
        }

    }
}
