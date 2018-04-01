package io.flogging.activities.main.fragments

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
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
import io.flogging.api.Flogging
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime

class NewLog : Fragment() {

    var vm: LogViewModel? = null
    var sub: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.new_log, container, false) as LinearLayout

        initFeedbackView(root)
        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)
        val prefs = Prefs(activity)
        sub = vm!!.logs.subscribe {
            setDefaults(root)
            initLayout(root, vm!!, prefs)
        }
        return root
    }

    override fun onPause() {
        if (sub != null) {
            sub!!.dispose()
        }
        super.onPause()
    }

    fun textChanged(p0: Editable?) {
        if (p0 != null && p0.length == 2)
            p0.append(":")
    }

    private fun setDefaults(root: LinearLayout) {
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

    private fun configureSaveButton(root: LinearLayout, viewModel: LogViewModel, prefs: Prefs) {
        root.findViewById<Button>(R.id.new_log_save)
                .setOnClickListener({ clickElem ->
                    Log.d("Button", "Clicked")
                    Log.d("Button", "Hiding feedback if possible")
                    hideFeedback(root)

                    // Set to disabled because of feedback
                    clickElem.isEnabled = false

                    val maybeStart = (root.findViewById<EditText>(R.id.new_log_start_time) as EditText).text.toString()
                    val start = if (maybeStart.isEmpty()) "8:00" else maybeStart

                    val maybeEnd = (root.findViewById<EditText>(R.id.new_log_end_time) as EditText).text.toString()
                    val end = if (maybeEnd.isEmpty()) "17:00" else maybeEnd

                    val maybeBreak = (root.findViewById<EditText>(R.id.new_log_break_time) as EditText).text
                    val breakTime = if (maybeBreak.isEmpty()) "60" else maybeBreak.toString()

                    val projectName = prefs.activeProject.projectName

                    val uuid = prefs.uid

                    val logType = (root.findViewById<Spinner>(R.id.new_log_log_type))
                            .selectedItem
                            .toString()
                            .replace(" ", "_")

                    val textOfTimestamp = (root.findViewById<EditText>(R.id.new_log_timestamp)
                            as EditText).text.toString()
                    val timestamp = DateTime.parse(textOfTimestamp, Flogs.YYYY_MM_DD_PATTERN)

                    Log.d("NewLog", "$start $end $breakTime")
                    viewModel.add_log(
                            projectName,
                            uuid,
                            textOfTimestamp,
                            start,
                            end,
                            breakTime.toInt(),
                            logType,
                            (root.findViewById<EditText>(R.id.new_log_note) as EditText).text.toString(),
                            { success: Boolean, message: String ->
                                Log.d("FirebasePost", "Saved?" + success)

                                if (success) {
                                    setPositiveFeedback(root, "Successfully created")
                                    hideFeedbackAfterXSeconds(root, 5)
                                    prefs.lastInsertedTimestamp = timestamp
                                } else {
                                    setNegativeFeedback(root, "Failed: " + message)
                                }
                                showFeedback(root)
                                clickElem.isEnabled = true
                            }
                    )
                })
    }

    private fun initLayout(root: LinearLayout,
                           viewModel: LogViewModel,
                           prefs: Prefs) {
        configureSaveButton(root, viewModel, prefs)

        // Press the timestamp button that will trigger a Datepicker to show up
        root.findViewById<Button>(R.id.new_log_pick_timestamp).setOnClickListener({
            val etText = root.findViewById<EditText>(R.id.new_log_timestamp).text.toString()
            val etTextHasText = !etText.isEmpty()

            val lastInsertedTs = prefs.lastInsertedTimestamp
            val dpYear = if (etTextHasText) etText.split("-")[0].toInt() else lastInsertedTs.year
            val dpMonth = (if (etTextHasText) etText.split("-")[1].toInt() else lastInsertedTs.monthOfYear)
            val dpDay = if (etTextHasText) etText.split("-")[2].toInt() else lastInsertedTs.dayOfMonth

            DatePickerDialog(activity, { _: DatePicker,
                                         year: Int,
                                         month: Int,
                                         day: Int ->

                val zeroedMonth = if ((month + 1) < 10) "0" + (month + 1) else month.toString()
                val zeroedDay = if (day < 10) "0" + day else day.toString()
                root.findViewById<EditText>(R.id.new_log_timestamp)
                        .setText(year.toString() + "-" + zeroedMonth + "-" + zeroedDay,
                                TextView.BufferType.EDITABLE)

            }, dpYear, dpMonth - 1, dpDay).show()
        })

        // Press the start timepicking button that will trigger a Timepicker to show up
        root.findViewById<Button>(R.id.new_log_pick_start_time).setOnClickListener({
            TimePickerDialog(activity, { _: TimePicker,
                                         i: Int,
                                         i1: Int ->
                val leadingZeroHour = if (i < 10) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 10) "0$i1" else i1.toString()
                root.findViewById<EditText>(R.id.new_log_start_time)
                        .setText(leadingZeroHour + ":" + leadingZeroMinute, TextView.BufferType.EDITABLE)
                decimalCalc(root)

            }, 8, 30, true).show()
        })

        // Press the end timepicking button that will trigger a Timepicker to show up
        root.findViewById<Button>(R.id.new_log_pick_end_time).setOnClickListener({
            TimePickerDialog(activity, { _: TimePicker,
                                         i: Int,
                                         i1: Int ->
                val leadingZeroHour = if (i < 10) "0$i" else i.toString()
                val leadingZeroMinute = if (i1 < 10) "0$i1" else i1.toString()
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

    private fun decimalCalc(root: LinearLayout) {
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

    private fun setFeedback(root: LinearLayout, color: Int, feedbackText: String) {
        val tv = root.findViewById<TextView>(R.id.new_log_feedback_text)
        tv.setBackgroundColor(color)
        tv.text = feedbackText
    }

    private fun initFeedbackView(root: LinearLayout) {
        val tv = getFeedbackElement(root)
        tv.setOnClickListener {
            hideFeedback(root)
        }
    }

    private fun setNegativeFeedback(root: LinearLayout, feedbackText: String) {
        setFeedback(root, resources.getColor(R.color.red), feedbackText)
    }

    private fun setPositiveFeedback(root: LinearLayout, feedbackText: String) {
        setFeedback(root, resources.getColor(R.color.green), feedbackText)
    }

    private fun getFeedbackElement(root: LinearLayout): TextView {
        return root.findViewById(R.id.new_log_feedback_text)
    }

    private fun measureElement(tv: TextView): Int {
        tv.measure(
                View.MeasureSpec.makeMeasureSpec(tv.width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return tv.measuredHeight

    }

    private fun showFeedback(root: LinearLayout) {
        val elem = getFeedbackElement(root)
        val targetHeight = measureElement(elem)

        val anim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                elem.layoutParams.height = (targetHeight * interpolatedTime).toInt()
                elem.requestLayout()
            }
        }
        anim.duration = 400
        view!!.startAnimation(anim)
    }

    private fun hideFeedbackAfterXSeconds(root: LinearLayout, seconds: Int) {
        Thread {
            Thread.sleep((seconds * 1000).toLong())
            activity.runOnUiThread {
                hideFeedback(root)
            }
        }.start()
    }

    private fun hideFeedback(root: LinearLayout) {
        val elem = getFeedbackElement(root)

        if (elem.height > 0) {
            val anim = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                    elem.layoutParams.height = elem.layoutParams.height - (elem.layoutParams.height * interpolatedTime).toInt()
                    elem.requestLayout()
                }
            }
            anim.duration = 2000
            view!!.startAnimation(anim)
        }
    }


}
