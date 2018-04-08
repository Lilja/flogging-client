package io.flogging.activities.main.viewmodels

import android.arch.lifecycle.ViewModel
import android.util.Log
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.reactivex.subjects.BehaviorSubject
import org.joda.time.DateTime

class LogViewModel : ViewModel() {
    val logs: BehaviorSubject<List<FloggingRow>> = BehaviorSubject.create()
    val projects: BehaviorSubject<List<FloggingProject>> = BehaviorSubject.create()

    fun getLogsWithDiff(rows: List<FloggingRow>,
                        project: FloggingProject): List<Pair<Int, FloggingRow>> {
        val dailyHour = project.dailyHour.toInt()
        val dailyMinute = project.dailyMinute.toInt()

        val perDay = rows
                .sortedWith(compareBy(FloggingRow::timestamp))
                .groupBy { it.timestamp.toString("MM-dd") }
        var previousEntryDiff = 0
        val workSheetDays = mutableListOf<Pair<Int, FloggingRow>>()

        perDay.map { (_, days) ->
            // Tuple over current day and the list of records of that day
            var todaysDiff = 0
            val otherMinutes = days.filter { it.status == FloggingRow.Status.OTHER }
                    .sumBy {
                        val listOfHHMM = it.decimal.split(":")
                        val hours = (if (listOfHHMM.size == 2) listOfHHMM[0].toInt() else it.decimal.toInt()) * 60
                        val minutes = if (listOfHHMM.size == 2) listOfHHMM[1].toInt() else 0
                        hours + minutes
                    }

            for ((idx, entry) in days.withIndex()) {
                // If first record, assume that we're supposed to work by a fixed amount, and we
                // "owe" to the client/company.

                if (idx == 0 && Flogs.isWorkingDay(entry.timestamp)) {
                    if (entry.status == FloggingRow.Status.WORKED ||
                            entry.status == FloggingRow.Status.FLEX_TIME_OFF ||
                            entry.status == FloggingRow.Status.PAID_LEAVE) {

                        todaysDiff -= ((dailyHour * 60) + dailyMinute)
                    }
                }

                val listOfHHMM = entry.decimal.split(":")
                val hours = (if (listOfHHMM.size == 2) listOfHHMM[0].toInt() else entry.decimal.toInt()) * 60
                val minutes = if (listOfHHMM.size == 2) listOfHHMM[1].toInt() else 0
                var currentEntryDiff = 0

                when (entry.status) {
                    FloggingRow.Status.WORKED,
                    FloggingRow.Status.PAID_LEAVE,
                    FloggingRow.Status.OTHER -> {
                        currentEntryDiff = (hours + minutes)
                    }
                    FloggingRow.Status.FLEX_TIME_OFF -> {
                    }
                    FloggingRow.Status.PUBLIC_HOLIDAY -> {
                        currentEntryDiff = 0
                    }
                }

                //println("${entry.status} ${entry.startDate} " +
                //        "Current: $currentEntryDiff Today: $todaysDiff Prev: $previousEntryDiff")
                currentEntryDiff = (todaysDiff + currentEntryDiff) + previousEntryDiff
                todaysDiff = 0
                workSheetDays.add(Pair(currentEntryDiff, entry))
                previousEntryDiff = currentEntryDiff
            }
        }
        return ArrayList(workSheetDays)
    }


    private fun <T> List<T>.sliding(windowSize: Int): List<List<T>> {
        return this.dropLast(windowSize - 1).mapIndexed { i, s -> this.subList(i, i + windowSize) }
    }

    fun indicateMissingEntries(logs: List<FloggingRow>): List<DateTime> {
        fun dayIsNotNextLog(first: DateTime, second: DateTime): Boolean {
            return first != second && first.plusDays(1) != second
        }

        fun missingDate(first: FloggingRow, second: FloggingRow): List<DateTime> {
            var walker = first.timestamp
            val listOfMissingEntries = mutableListOf<DateTime>()

            while (dayIsNotNextLog(walker, second.timestamp)) {
                walker = walker.plusDays(1)
                listOfMissingEntries.add(walker)
            }

            return listOfMissingEntries
        }

        val sorted = logs.sortedBy { it.timestamp }
        // [{ "ts": "01"}, {"ts": "02"}, {"ts": "04"}]
        //      .sliding(2)
        // [ [{"ts2: "01"}, {"ts": "02"}], [{"ts": "02"}, {"ts": "04"}], [{"ts":"04"}]
        val slidedLogs = sorted.sliding(2)

        // If it's an uneven list, remove the last element
        val slidedLogs2 = if (slidedLogs.size > 1 && slidedLogs.size % 2 != 0)
            slidedLogs.dropLast(1)
        else
            slidedLogs

        return slidedLogs2.flatMap {
            missingDate(it[0], it[1])
        }.filter {
            Flogs.isWorkingDay(it)
        }

    }

    fun loadLogsForProject(projectName: String, uuid: String) {
        Flogging.getLogsForProject(
                projectName,
                uuid,
                { rows ->
                    logs.onNext(rows)
                }
        )
    }

    fun loadSpecificLogForProject(projectName: String, uuid: String, uniqueKey: String) {
        Flogging.getSpecifcLogForProject(
                projectName,
                uuid,
                uniqueKey,
                { rows ->
                    logs.onNext(rows)
                }
        )
    }

    private fun createLogFromProperties(timestamp: String,
                                        startDate: String,
                                        endDate: String,
                                        breakMinutes: Int,
                                        decimal: String,
                                        status: String,
                                        note: String): FloggingRow {
        val ts = DateTime.parse(timestamp, Flogs.YYYY_MM_DD_PATTERN)
        val start = DateTime.parse(startDate, Flogs.HH_MM_PATTERN)
        val end = DateTime.parse(endDate, Flogs.HH_MM_PATTERN)

        return FloggingRow(
                ts,
                start,
                end,
                breakMinutes,
                decimal,
                status,
                note
        )
    }

    fun addLog(projectName: String,
               uid: String,
               timestamp: String,
               startDate: String,
               endDate: String,
               breakMinutes: Int,
               status: String,
               note: String,
               success: (b: Boolean, s: String) -> Unit) {
        Log.d("LogViewModel", "Status " + status)
        val decimal = Flogging.calculateDiff(
                startDate,
                endDate,
                breakMinutes
        ).toString()
        val log = createLogFromProperties(timestamp, startDate, endDate, breakMinutes, decimal, status, note)

        Log.d("LogViewModel", "AddLog" + log)
        Flogging.createLogEntryFromObject(projectName, uid, log, { b: Boolean, s: String ->
            if (b) {
                loadLogsForProject(projectName, uid)
            }
            success(b, s)
        })
    }

    fun omitCurrentLogs() {
        if (logs.value != null)
            logs.onNext(logs.value)
    }

    fun loadProjects(user: String) {
        Flogging.getProjectsFromUser(user, {
            projects.onNext(it)
        })
    }

    fun deleteLog(project: FloggingProject,
                  user: String,
                  log: FloggingRow,
                  success: (b: Boolean, s: String) -> Unit) {
        Flogging.deleteLogEntry(project.projectName, user, log, success)
    }

    fun updateLog(projectName: FloggingProject,
                  user: String,
                  oldUniqueKey: String,
                  uniqueKey: String,
                  log: FloggingRow,
                  success: (b: Boolean, s: String) -> Unit) {
        Flogging.updateLog(projectName, user, oldUniqueKey,
                uniqueKey, log, success)
    }

    fun initUser(uuid: String,
                 name: String,
                 success: (b: Boolean, s: String) -> Unit) {
        Flogging.initUser(uuid, name, { isSuccessful, message ->
            success(isSuccessful, message)
        })
    }

    fun createProject(projectName: String,
                      dailyHour: String,
                      dailyMinute: String,
                      uid: String,
                      success: (b: Boolean, s: String) -> Unit) {
        Flogging.createProject(
                projectName,
                dailyHour,
                dailyMinute,
                uid,
                { completed, message -> success(completed, message) }
        )
    }

}
