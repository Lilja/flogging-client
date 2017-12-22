package io.flogging

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import io.flogging.api.Flogging
import io.flogging.util.Flogs
import org.joda.time.DateTime
import java.util.*

class NewLog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_log)
        setDefaults()
        initLayout()
    }

    fun textChanged(p0: Editable?) {
        if (p0 != null && p0.length == 2)
            p0.append(":")
    }

    private fun setDefaults() {
        val now = DateTime.now()
        findViewById<EditText>(R.id.new_log_timestamp)
                .setText(now.year.toString() + "-" + now.toString("MM") + "-" + now.toString("dd"),
                        TextView.BufferType.EDITABLE)
        findViewById<EditText>(R.id.new_log_start_time)
                .addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        textChanged(p0)
                    }
                })
        findViewById<EditText>(R.id.new_log_end_time)
                .addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        textChanged(p0)
                    }
                })
    }

    private fun initLayout() {
        findViewById<Button>(R.id.new_log_save)
                .setOnClickListener({
                    Log.d("Button", "Clicked")
                    Flogging.createLogEntry(
                            (findViewById<EditText>(R.id.new_log_timestamp) as EditText).text.toString(),
                            (findViewById<EditText>(R.id.new_log_start_time) as EditText).text.toString(),
                            (findViewById<EditText>(R.id.new_log_end_time) as EditText).text.toString(),
                            (findViewById<EditText>(R.id.new_log_break_time) as EditText).text.toString().toInt(),
                            (findViewById<Spinner>(R.id.new_log_log_type)).selectedItem.toString(),
                            (findViewById<EditText>(R.id.new_log_note) as EditText).text.toString(),
                            { success ->
                                Log.d("FirebasePost", "Saved?" + success)
                                finish()
                            }
                    )
                })

        findViewById<Button>(R.id.new_log_pick_timestamp).setOnClickListener({
            val c = Calendar.getInstance()
            DatePickerDialog(this@NewLog, { _: DatePicker,
                                            year: Int,
                                            month: Int,
                                            day: Int ->

                findViewById<EditText>(R.id.new_log_timestamp)
                        .setText(year.toString() + "-" + (month + 1).toString() + "-" + day.toString(),
                                TextView.BufferType.EDITABLE)

            }, c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)).show()
        })

        findViewById<Button>(R.id.new_log_pick_start_time).setOnClickListener({
            TimePickerDialog(this@NewLog, { _: TimePicker,
                                            i: Int,
                                            i1: Int ->
                val leadingZeroHour = if (i < 9) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 9) "0$i1" else i1.toString()
                findViewById<EditText>(R.id.new_log_start_time)
                        .setText(leadingZeroHour + ":" + leadingZeroMinute, TextView.BufferType.EDITABLE)
                decimalCalc()

            }, 8, 30, true).show()
        })

        findViewById<Button>(R.id.new_log_pick_end_time).setOnClickListener({
            TimePickerDialog(this@NewLog, { _: TimePicker,
                                            i: Int,
                                            i1: Int ->
                val leadingZeroHour = if (i < 9) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 9) "0$i1" else i1.toString()
                findViewById<EditText>(R.id.new_log_end_time)
                        .setText(leadingZeroHour + ":" + leadingZeroMinute, TextView.BufferType.EDITABLE)
                decimalCalc()

            }, 17, 30, true).show()
        })

        findViewById<EditText>(R.id.new_log_break_time).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                decimalCalc()
            }
        })

    }

    private fun decimalCalc() {
        val startTimeStr = (findViewById<EditText>(R.id.new_log_start_time) as EditText).text.toString()
        val startTime = if (startTimeStr == "") "8:30" else startTimeStr
        val endTimeStr = (findViewById<EditText>(R.id.new_log_end_time) as EditText).text.toString()
        val endTime = if (endTimeStr == "") "17:30" else endTimeStr
        val breakTimeStr = (findViewById<EditText>(R.id.new_log_break_time) as EditText).text.toString()
        val breakTime = if (breakTimeStr == "") 60 else breakTimeStr.toInt()


        val diff = Flogging.calculateDiff(startTime, endTime, breakTime)
        (findViewById<TextView>(R.id.new_log_output) as TextView).text =
                Flogs.minutesToHHMM(diff)
    }
}
