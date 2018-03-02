package io.flogging.activities.logdetail

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.R
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import org.joda.time.DateTime

class DetailedLogView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_log_view)
        val prefs = Prefs(this)
        val k = intent.getStringExtra("index")
        val user = FirebaseAuth.getInstance().currentUser!!.uid
        initView(k, prefs.activeProject.projectName, user)
    }

    private fun initMate(log: FloggingRow) {
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
        findViewById<Button>(R.id.detailed_log_view_button_delete).setOnClickListener {
            val ts = (findViewById<TextView>(R.id.detailed_log_view_timestamp) as TextView).text.toString()
            val start = (findViewById<TextView>(R.id.detailed_log_view_start) as TextView).text.toString()
            val end = (findViewById<TextView>(R.id.detailed_log_view_end) as TextView).text.toString()
            val prefs = Prefs(this)
            val uid = FirebaseAuth.getInstance().uid.toString()

            Flogging.deleteLogEntry(prefs.activeProject.projectName, uid, ts, start, end, {
                finish()
            })
        }

    }

    private fun initView(index: String, project_name : String, uid: String) {
        Log.d("InitViewGetLog", index)
        val instance = FirebaseFirestore.getInstance()
        instance.document("users/$uid/projects/$project_name/timestamps/$index")
                .get()
                .addOnSuccessListener { task ->
                    val values: MutableMap<String, Any> = task.data
                    val log = FloggingRow(
                            DateTime.parse(values.getOrDefault("timestamp", "").toString(), Flogs.YYYY_MM_DD_PATTERN),
                            DateTime.parse(values.getOrDefault("startDate", "").toString(), Flogs.HH_MM_PATTERN),
                            DateTime.parse(values.getOrDefault("endDate", "").toString(), Flogs.HH_MM_PATTERN),
                            values.getOrDefault("breakMinutes", "").toString().toInt(),
                            values.getOrDefault("decimal", "").toString(),
                            values.getOrDefault("status", FloggingRow.Status.WORKED).toString(),
                            values.getOrDefault("note", "").toString()
                    )
                    initMate(log)
                }
    }

}
