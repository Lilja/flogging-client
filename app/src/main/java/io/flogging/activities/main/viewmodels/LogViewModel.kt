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
    var uuid: String? = null
    var displayName: String? = null

    fun getLogsWithDiff(rows: List<FloggingRow>,
                        project: FloggingProject): List<Pair<Int, FloggingRow>> {
        val dailyHour = project.dailyHour.toInt()
        val dailyMinute = project.dailyMinute.toInt()

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
                        todaysDiff = if (otherMinutes > 0) {
                            ((hours + minutes) - (((dailyHour * 60) + dailyMinute) - otherMinutes)) + previousEntryDiff
                        } else {
                            ((hours + minutes) - (dailyHour * 60) + dailyMinute) + previousEntryDiff
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

    fun loadLogsForProject(projectName: String, uuid: String) {
        Flogging.getLogsForProject(
                projectName,
                uuid,
                { rows ->
                    logs.onNext(rows)
                }
        )
    }

    fun set_project_name(projectName: String, uuid: String) {
        loadLogsForProject(projectName, uuid)
    }

    fun create_log_from_properties(timestamp: String,
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
                status.toUpperCase(),
                note
        )
    }

    fun add_log(projectName: String,
                uid: String,
                log: FloggingRow,
                success: (b: Boolean, s: String) -> Unit) {
        Flogging.createLogEntryFromObject(projectName, uid, log, { b: Boolean, s: String ->
            if (b) {
                loadLogsForProject(projectName, uid)
                success(b, s)
            }
        })
    }

    fun set_logs(logs: List<FloggingRow>) {
        this.logs.onNext(logs)
    }

    fun add_log(projectName: String,
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
        val log = create_log_from_properties(timestamp, startDate, endDate, breakMinutes, decimal, status, note)

        Flogging.createLogEntryFromObject(projectName, uid, log, { b: Boolean, s: String ->
            if (b) {
                loadLogsForProject(projectName, uid)
            }
            success(b, s)
        })
    }

    fun omitCurrentLogs() {
        logs.onNext(logs.value)
    }

    fun loadProjects(user : String) {
        Flogging.getProjectsFromUser(user, {
            projects.onNext(it)
        })
    }

}
