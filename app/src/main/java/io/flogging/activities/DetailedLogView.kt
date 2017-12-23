package io.flogging.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.R
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import org.joda.time.DateTime

class DetailedLogView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_log_view)
        val k = intent.getStringExtra("index")
        initView(k)
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
            Flogging.deleteLogEntry("funnel",ts, start, end, {
                finish()
            })
        }

    }

    private fun initView(index: String) {
        val instance = FirebaseFirestore.getInstance()
        instance.document("projects/funnel/timestamps/$index")
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
