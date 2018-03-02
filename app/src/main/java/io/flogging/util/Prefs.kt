package io.flogging.util

import android.content.Context
import android.content.SharedPreferences
import io.flogging.model.FloggingProject
import org.joda.time.DateTime

class Prefs(context: Context) {
    private val PREFS_FILENAME = "io.flogging.prefs"

    private val PROJECT = "active_project"
    private val DAILY_HOUR = "active_hour"
    private val DAILY_MINUTE = "active_minute"

    private val FILTER_CHRONOLOGICAL_ORDER = "chronological_order"
    private val FILTER_ENABLE_DATE = "filter_enable_interval_filter"
    private val FILTER_START_DATE = "filter_start_date"
    private val FILTER_END_DATE = "filter_end_date"

    private val prefs: SharedPreferences = context
            .getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    var activeProject: FloggingProject
        get() {
            val pr = prefs.getString(PROJECT, "")
            val dh = prefs.getString(DAILY_HOUR, "")
            val dm = prefs.getString(DAILY_MINUTE, "")
            val proj = FloggingProject(pr, dh, dm)
            return proj
        }
        set(project) {
            val editor = prefs.edit()
                    .putString(PROJECT, project.projectName)
                    .putString(DAILY_HOUR, project.dailyHour)
                    .putString(DAILY_MINUTE, project.dailyMinute)
            editor.apply()
        }

    var chronoicalOrder: Boolean
        get() {
            return prefs.getBoolean(FILTER_CHRONOLOGICAL_ORDER, false)
        }
        set(chronologicalOrder) {
            prefs.edit().putBoolean(FILTER_CHRONOLOGICAL_ORDER, chronologicalOrder).apply()
        }

    var enableDateFilter: Boolean
        get() {
            return prefs.getBoolean(FILTER_ENABLE_DATE, false)
        }
        set(chronologicalOrder) {
            prefs.edit().putBoolean(FILTER_ENABLE_DATE, chronologicalOrder).apply()
        }

    var startDate: DateTime
        get() {
            val now = DateTime.now()
            val yymmdd = prefs.getString(FILTER_START_DATE, "")
            if (yymmdd.length == 10) {
                return DateTime.parse(yymmdd, Flogs.YYYY_MM_DD_PATTERN)
            }
            return now
        }
        set(startDate) {
            prefs.edit().putString(FILTER_START_DATE, startDate.toString(Flogs.YYYY_MM_DD_PATTERN)).apply()
        }

    var endDate: DateTime
        get() {
            val now = DateTime.now()
            val yymmdd = prefs.getString(FILTER_END_DATE, "")
            if (yymmdd.length == 10) {
                return DateTime.parse(yymmdd, Flogs.YYYY_MM_DD_PATTERN)
            }
            return now
        }
        set(endDate) {
            prefs.edit().putString(FILTER_END_DATE, endDate.toString(Flogs.YYYY_MM_DD_PATTERN)).apply()
        }

}