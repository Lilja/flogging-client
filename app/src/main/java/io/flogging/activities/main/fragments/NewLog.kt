package io.flogging.activities.main.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import io.flogging.R
import io.flogging.api.Flogging
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import org.joda.time.DateTime
import java.util.*

class NewLog : Fragment() {


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.activity_new_log, container, false) as ConstraintLayout
        setDefaults(root)
        initLayout(root)
        return root

    }

    fun textChanged(p0: Editable?) {
        if (p0 != null && p0.length == 2)
            p0.append(":")
    }

    private fun setDefaults(root: ConstraintLayout) {
        val now = DateTime.now()
        root.findViewById<EditText>(R.id.new_log_timestamp)
                .setText(now.year.toString() + "-" + now.toString("MM") + "-" + now.toString("dd"),
                        TextView.BufferType.EDITABLE)
        root.findViewById<EditText>(R.id.new_log_start_time)
                .addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        textChanged(p0)
                    }
                })
        root.findViewById<EditText>(R.id.new_log_end_time)
                .addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        textChanged(p0)
                    }
                })
    }

    private fun initLayout(root: ConstraintLayout) {
        root.findViewById<Button>(R.id.new_log_save)
                .setOnClickListener({
                    Log.d("Button", "Clicked")
                    val maybeStart = (root.findViewById<EditText>(R.id.new_log_start_time) as EditText).text.toString()
                    val start = if (maybeStart.isEmpty()) "8:00" else maybeStart

                    val maybeEnd = (root.findViewById<EditText>(R.id.new_log_end_time) as EditText).text.toString()
                    val end = if (maybeEnd.isEmpty()) "17:00" else maybeEnd

                    val maybeBreak = (root.findViewById<EditText>(R.id.new_log_break_time) as EditText).text
                    val breakTime = if (maybeBreak.isEmpty()) "60" else maybeBreak.toString()

                    val prefs = Prefs(activity)
                    val projectName = prefs.activeProject.projectName

                    val uuid = FirebaseAuth.getInstance().currentUser?.uid.toString()
                    Log.d("NewLog", "$start $end $breakTime")
                    Flogging.createLogEntry(
                            projectName,
                            uuid,
                            (root.findViewById<EditText>(R.id.new_log_timestamp) as EditText).text.toString(),
                            start,
                            end,
                            breakTime.toInt(),
                            (root.findViewById<Spinner>(R.id.new_log_log_type)).selectedItem.toString(),
                            (root.findViewById<EditText>(R.id.new_log_note) as EditText).text.toString(),
                            { success ->
                                Log.d("FirebasePost", "Saved?" + success)
                            }
                    )
                })

        root.findViewById<Button>(R.id.new_log_pick_timestamp).setOnClickListener({
            val c = Calendar.getInstance()
            DatePickerDialog(activity, { _: DatePicker,
                                         year: Int,
                                         month: Int,
                                         day: Int ->

                root.findViewById<EditText>(R.id.new_log_timestamp)
                        .setText(year.toString() + "-" + (month + 1).toString() + "-" + day.toString(),
                                TextView.BufferType.EDITABLE)

            }, c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)).show()
        })

        root.findViewById<Button>(R.id.new_log_pick_start_time).setOnClickListener({
            TimePickerDialog(activity, { _: TimePicker,
                                         i: Int,
                                         i1: Int ->
                val leadingZeroHour = if (i < 9) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 9) "0$i1" else i1.toString()
                root.findViewById<EditText>(R.id.new_log_start_time)
                        .setText(leadingZeroHour + ":" + leadingZeroMinute, TextView.BufferType.EDITABLE)
                decimalCalc(root)

            }, 8, 30, true).show()
        })

        root.findViewById<Button>(R.id.new_log_pick_end_time).setOnClickListener({
            TimePickerDialog(activity, { _: TimePicker,
                                         i: Int,
                                         i1: Int ->
                val leadingZeroHour = if (i < 9) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 9) "0$i1" else i1.toString()
                root.findViewById<EditText>(R.id.new_log_end_time)
                        .setText(leadingZeroHour + ":" + leadingZeroMinute, TextView.BufferType.EDITABLE)
                decimalCalc(root)

            }, 17, 30, true).show()
        })

        root.findViewById<EditText>(R.id.new_log_break_time).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                decimalCalc(root)
            }
        })
    }

    private fun decimalCalc(root: ConstraintLayout) {
        val startTimeStr = (root.findViewById<EditText>(R.id.new_log_start_time) as EditText).text.toString()
        val startTime = if (startTimeStr == "") "8:30" else startTimeStr
        val endTimeStr = (root.findViewById<EditText>(R.id.new_log_end_time) as EditText).text.toString()
        val endTime = if (endTimeStr == "") "17:30" else endTimeStr
        val breakTimeStr = (root.findViewById<EditText>(R.id.new_log_break_time) as EditText).text.toString()
        val breakTime = if (breakTimeStr == "") 60 else breakTimeStr.toInt()

        val diff = Flogging.calculateDiff(startTime, endTime, breakTime)
        (root.findViewById<TextView>(R.id.new_log_output) as TextView).text =
                Flogs.minutesToHHMM(diff)
    }
}
