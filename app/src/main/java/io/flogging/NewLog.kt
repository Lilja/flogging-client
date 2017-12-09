package io.flogging

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*

import kotlinx.android.synthetic.main.activity_new_log.*
import org.joda.time.DateTime

class NewLog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_log)
        initLayout()
    }

    fun initLayout() {
        findViewById<Button>(R.id.new_log_save)
                .setOnClickListener({
                    Log.d("Button", "Clicked")
                })

        findViewById<Button>(R.id.new_log_pick_timestamp).setOnClickListener({
            Log.d("initLayout", "Did you press pick timestamp?")
            val now = DateTime.now()
            DatePickerDialog(this@NewLog, {
                _: DatePicker,
                year: Int,
                month: Int,
                day: Int ->

                findViewById<EditText>(R.id.new_log_timestamp)
                .setText(year.toString() + "-" + month.toString() + "-" + day.toString(),
                        TextView.BufferType.EDITABLE)

            }, now.year, now.monthOfYear, now.dayOfMonth).show()
        })

        findViewById<Button>(R.id.new_log_pick_start_time).setOnClickListener({
            TimePickerDialog(this@NewLog, {
                _: TimePicker,
                i: Int,
                i1: Int ->
                findViewById<EditText>(R.id.new_log_start_time)
                        .setText(i.toString() + ":" + i1.toString(), TextView.BufferType.EDITABLE)

            }, 8, 30, true).show()
        })

        findViewById<Button>(R.id.new_log_pick_end_time).setOnClickListener({
            TimePickerDialog(this@NewLog, {
                _: TimePicker,
                i: Int,
                i1: Int ->
                findViewById<EditText>(R.id.new_log_end_time)
                        .setText(i.toString() + ":" + i1.toString(), TextView.BufferType.EDITABLE)

            }, 17, 30, true).show()
        })
    }

}
