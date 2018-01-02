package io.flogging.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.model.FloggingRow
import io.flogging.model.FloggingRowFireStore
import io.flogging.model.FloggingProject
import io.flogging.util.Flogs
import io.flogging.util.Global
import org.joda.time.DateTime
import org.joda.time.Minutes

class Flogging {

    companion object {
        fun getLogsWithDiff(rows: List<FloggingRow>): List<Pair<Int, FloggingRow>> {
            val perDay = rows
                    .sortedWith(compareBy(FloggingRow::timestamp))
                    .groupBy { it.timestamp.toString("MM-dd") }
            var previousEntryDiff = 0
            val mutator = mutableListOf<Pair<Int, FloggingRow>>()
            perDay.map { (_, days) ->
                var todaysDiff = previousEntryDiff
                var hasSubtractedWithDailyLimit = false
                val otherMinutes = days.filter { it.status == FloggingRow.Status.OTHER }
                        .sumBy {
                            val listOfHHMM = it.decimal.split(":")
                            val hours = (if (listOfHHMM.size == 2) listOfHHMM[0].toInt() else it.decimal.toInt()) * 60
                            val minutes = if (listOfHHMM.size == 2) listOfHHMM[1].toInt() else 0
                            hours + minutes
                        }
                for ((_, entry) in days.withIndex()) {
                    val listOfHHMM = entry.decimal.split(":")
                    val hours = (if (listOfHHMM.size == 2) listOfHHMM[0].toInt() else entry.decimal.toInt()) * 60
                    val minutes = if (listOfHHMM.size == 2) listOfHHMM[1].toInt() else 0
                    var calc = 0
                    // Only calculate flex
                    if (entry.status == FloggingRow.Status.WORKED) {
                        calc = if (!Flogs.isWorkingDay(entry.timestamp)) {
                            // If working on a weekend, we should just add to the difference.
                            (hours + minutes) + previousEntryDiff
                        } else if (!hasSubtractedWithDailyLimit) {
                            // If the current of this is day is the first one we should subtract by hours_per_day
                            hasSubtractedWithDailyLimit = true
                            if (otherMinutes > 0) {
                                todaysDiff = ((hours + minutes) - (((Global.HOURS_PER_DAY * 60) + Global.MINUTES_PER_DAY) - otherMinutes)) + previousEntryDiff
                            } else {
                                todaysDiff = ((hours + minutes) - (Global.HOURS_PER_DAY * 60) + Global.MINUTES_PER_DAY) + previousEntryDiff
                            }
                            todaysDiff
                        } else {
                            (hours + minutes) + todaysDiff
                        }
                    } else if (entry.status == FloggingRow.Status.FLEX_TIME_OFF) {
                        calc = previousEntryDiff - (hours + minutes)
                    } else if (entry.status == FloggingRow.Status.OTHER) {
                        calc = previousEntryDiff
                    }
                    previousEntryDiff = calc
                    mutator.add(Pair(calc, entry))
                }
            }
            return ArrayList(mutator)
        }

        fun indicateMissingLogEntries(list: List<FloggingRow>): List<String> {
            val copy = list.filter { Flogs.isWorkingDay(it.timestamp) }
            val groupedPerDay = copy.groupBy {
                it.timestamp.toString("dd")
            }

            var lastEntry: FloggingRow = copy[0]
            var firstRun = false

            val listOfMissingDates = mutableListOf<String>()
            for ((_, listOfDay) in groupedPerDay) {
                if (firstRun) {
                    firstRun = false
                } else {
                    val day = listOfDay[0]
                    if (!Flogs.isToday(lastEntry.timestamp.plusDays(1), day.timestamp)) {
                        lastEntry = day
                    } else {
                        listOfMissingDates.add(lastEntry.timestamp.plusDays(1).toString("yyyy-MM-dd"))
                    }

                }
            }
            return ArrayList(listOfMissingDates).toList()
        }

        fun calculateDiff(startTime: String, endTime: String, breakMinutes: Int): Int {
            val d1 = DateTime.parse(startTime, Flogs.HH_MM_PATTERN)
            val d2 = DateTime.parse(endTime, Flogs.HH_MM_PATTERN).minusMinutes(breakMinutes)

            Log.d("CalculateDiff", (d1).toString())
            Log.d("CalculateDiff", (d1).toString())
            Log.d("CalculateDiff", Minutes.minutesBetween(d1, d2).minutes.toString())
            return Minutes.minutesBetween(d1, d2).minutes
        }

        private fun createFloggingRow(timestamp: String,
                                      startTime: String,
                                      endTime: String,
                                      breakMinutes: Int,
                                      typeOfLog: String,
                                      note: String)
                : FloggingRow {
            return FloggingRow(
                    DateTime.parse(timestamp, Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(startTime, Flogs.HH_MM_PATTERN),
                    DateTime.parse(endTime, Flogs.HH_MM_PATTERN),
                    breakMinutes,
                    Flogs.minutesToHHMM(calculateDiff(startTime, endTime, breakMinutes)),
                    FloggingRow.Status.valueOf(typeOfLog
                            .toUpperCase()
                            .replace(" ", "_")),
                    note
            )
        }

        private fun createIndex(floggingRow: FloggingRow): String {
            return Flogs.YYYY_MM_DD_PATTERN.print(floggingRow.timestamp) + " " +
                    Flogs.HH_MM_PATTERN.print(floggingRow.startDate) + " " +
                    Flogs.HH_MM_PATTERN.print(floggingRow.endDate)
        }

        fun createProject(projectName: String,
                          dailyHour: String,
                          dailyMinutes: String,
                          uid: String,
                          succeeded: (status: Boolean) -> Unit) {
            val projectSettings = mapOf(
                    "daily_hour" to dailyHour,
                    "daily_minute" to dailyMinutes,
                    "name" to projectName
            )

            val instance = FirebaseFirestore.getInstance()
            instance
                    .document("users/$uid/projects/$projectName")
                    .set(projectSettings)
                    .addOnSuccessListener {
                        succeeded(true)
                    }
                    .addOnFailureListener {
                        succeeded(false)
                    }
        }

        fun getProjectsFromUser(uuid: String, complete: (projects: List<FloggingProject>) -> Unit) {
            FirebaseFirestore.getInstance().collection("users/$uuid/projects")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        Log.d("GetProjectsFromUser", "Success")
                        val k = querySnapshot.documents.map {
                            firebaseProjectToFloggingProject(it.data)
                        }
                        complete(k)
                    }
                    .addOnFailureListener {
                        Log.d("GetProjectsFromUser", "Failure!")
                        Log.d("GetProjectsFromUser", it.toString())
                    }
        }

        private fun firebaseRowToFloggingRow(values: MutableMap<String, Any>): FloggingRow {
            return FloggingRow(
                    DateTime.parse(values.getOrDefault("timestamp", "").toString(), Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(values.getOrDefault("startDate", "").toString(), Flogs.HH_MM_PATTERN),
                    DateTime.parse(values.getOrDefault("endDate", "").toString(), Flogs.HH_MM_PATTERN),
                    values.getOrDefault("breakMinutes", "").toString().toInt(),
                    values.getOrDefault("decimal", "").toString(),
                    values.getOrDefault("status", FloggingRow.Status.WORKED).toString(),
                    values.getOrDefault("note", "").toString()
            )
        }

        private fun firebaseProjectToFloggingProject(values: MutableMap<String, Any>): FloggingProject {
            return FloggingProject(
                    values.getOrDefault("name", "Default").toString(),
                    values.getOrDefault("daily_hour", "Default").toString(),
                    values.getOrDefault("daily_minute", "Default").toString()
            )
        }

        fun createLogEntry(projectName: String,
                           uuid: String,
                           timestamp: String,
                           startTime: String,
                           endTime: String,
                           breakMinutes: Int,
                           typeOfLog: String,
                           note: String,
                           success: (status: Boolean) -> Unit) {
            val d1 = DateTime.parse(startTime, Flogs.HH_MM_PATTERN)
            val d2 = DateTime.parse(endTime, Flogs.HH_MM_PATTERN)
            if (d2.millis < d1.millis)
                throw IllegalArgumentException("Start is greater(sooner) than end time")
            if ((d2.millis - breakMinutes * 60) - d1.millis < 0)
                throw IllegalArgumentException("Break is larger than start time and end time")

            val row = createFloggingRow(timestamp, startTime, endTime, breakMinutes, typeOfLog, note)
            val index = createIndex(row)
            val obj = FloggingRowFireStore(row)

            val instance = FirebaseFirestore.getInstance()
            instance
                    .document("/users/$uuid/projects/$projectName/timestamps/$index")
                    .set(obj)
                    .addOnSuccessListener {
                        success(true)
                    }
                    .addOnFailureListener {
                        success(false)
                    }
        }

        fun deleteLogEntry(projectName: String,
                           uid: String,
                           timestamp: String,
                           startTime: String,
                           endTime: String,
                           succeeded: (status: Boolean) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            val index = createIndex(timesToFloggingRow(timestamp, startTime, endTime))
            instance.document("/users/$uid/projects/$projectName/timestamps/$index")
                    .delete()
                    .addOnSuccessListener {
                        succeeded(true)
                    }
                    .addOnFailureListener {
                        succeeded(false)
                    }
        }

        fun deleteProject(projectName: String,
                          uid: String,
                          succeeded: (status: Boolean) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            instance.document("/users/$uid/projects/$projectName")
                    .delete()
                    .addOnCompleteListener {
                        succeed ->
                        succeeded(succeed.isSuccessful)
                    }
        }

        private fun timesToFloggingRow(timestamp: String, startTime: String, endTime: String): FloggingRow {
            return FloggingRow(
                    DateTime.parse(timestamp, Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(startTime, Flogs.HH_MM_PATTERN),
                    DateTime.parse(endTime, Flogs.HH_MM_PATTERN),
                    0,
                    "",
                    FloggingRow.Status.WORKED,
                    ""
            )
        }

        fun getLogsForProject(projectName: String,
                              uuid: String,
                              callback: (rows: List<FloggingRow>) -> Unit) {
            Log.d("GetLogsForProject", "$projectName $uuid")
            val instance = FirebaseFirestore.getInstance()
            instance.collection("/users/$uuid/projects/$projectName/timestamps")
                    .get()
                    .addOnCompleteListener { task ->
                        val results = task.result
                        val records = results.documents.map {
                            firebaseRowToFloggingRow(it.data)
                        }
                        callback(records)
                    }

        }
    }

}