package io.flogging.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.Log
import io.flogging.model.FloggingProject

class Prefs(context: Context) {
    private val PREFS_FILENAME = "io.flogging.prefs"
    private val PROJECT = "active_project"
    private val DAILY_HOUR = "active_hour"
    private val DAILY_MINUTE = "active_minute"

    private val prefs: SharedPreferences = context
            .getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    var activeProject: FloggingProject
        get() {
            val pr = prefs.getString(PROJECT, "")
            val dh = prefs.getString(DAILY_HOUR, "")
            val dm = prefs.getString(DAILY_MINUTE, "")
            val proj = FloggingProject(pr, dh, dm)
            Log.d("PrefsGet", proj.projectName)
            Log.d("PrefsGet", "Is empty: "+proj.projectName.isEmpty().toString())
            return proj
        }
        set(project) {
            Log.d("PrefsSet", project.projectName)
            val editor = prefs.edit()
                .putString(PROJECT, project.projectName)
                .putString(DAILY_HOUR, project.dailyHour)
                .putString(DAILY_MINUTE, project.dailyMinute)
            editor.apply()

            Log.d("PrefsSet", ":" + prefs.getString(PROJECT, "Default"))
        }

    fun configureListener(foo: (value : String) -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener {
            asdf, key ->
            if (key == PROJECT)
                foo(asdf.getString(PROJECT, ""))
        }
    }

    fun registerListener(listener : SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener : SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}