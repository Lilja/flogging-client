package io.flogging.util

import android.util.Log
import io.flogging.model.FloggingProject
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*

class Flogs {
    companion object {
        val HH_MM_PATTERN = DateTimeFormat.forPattern("HH:mm")
        val YYYY_MM_DD_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd")
        val HEADER_PATTERN = "E, d MMM y"

        fun isWorkingDay(date: DateTime): Boolean {
            val foo = date.dayOfWeek().getAsText(Locale.ENGLISH).toLowerCase()

            if (foo == "sunday" || foo == "saturday") {
                return false
            }
            return true
        }

        fun isToday(d1: DateTime, d2: DateTime): Boolean {
            return d1.toLocalDate().isEqual(d2.toLocalDate())
        }

        fun minutesToHHMM(int: Int): String {
            Log.d("minutesToHHMM", int.toString())
            val hourRef = Math.abs(int / 60)
            val minuteRef = Math.abs(int % 60)
            val hours = (if (int < 0) "-" else "") + (if (hourRef < 10) "0" + hourRef else hourRef).toString()
            val minutes = (if (minuteRef < 10) "0" + minuteRef else minuteRef).toString()

            Log.d("minutesToHHMM", hours + ":" + minutes)
            return hours + ":" + minutes
        }

        fun hhMMWithDiff(str: String): String {
            if ("-" !in str) {
                return "+" + str
            }
            return str
        }
    }
}