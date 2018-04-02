package io.flogging.activities.main.viewmodels

import android.arch.lifecycle.ViewModel
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
                    FloggingRow.Status.FLEX_TIME_OFF -> {}
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
        val decimal = Flogging.calculateDiff(
                startDate,
                endDate,
                breakMinutes
        ).toString()
        val log = createLogFromProperties(timestamp, startDate, endDate, breakMinutes, decimal, status, note)

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
        Flogging.initUser(uuid, name, {
            isSuccessful, message ->
            success(isSuccessful, message)
        })
    }

}
