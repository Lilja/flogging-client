package io.flogging.flogginmonitorer

import io.flogging.model.FloggingRow
import io.flogging.util.Flogs

class FloggingMonitorer {
    companion object {
        fun indicateMissingLogEntries(list: List<FloggingRow>): List<String> {
            val copy = list.filter { Flogs.isWorkingDay(it.timestamp) }
            val groupedPerDay = copy.groupBy {
                it.timestamp.toString("dd")
            }

            var lastEntry : FloggingRow = copy[0]
            var firstRun = false

            val listOfMissingDates = mutableListOf<String>()
            for((_, listOfDay) in groupedPerDay) {
                if (firstRun) {
                    firstRun = false
                } else {
                    val day = listOfDay[0]
                    if(!Flogs.isToday(lastEntry.timestamp.plusDays(1), day.timestamp)) {
                        lastEntry = day
                    } else {
                        listOfMissingDates.add(lastEntry.timestamp.plusDays(1).toString("yyyy-MM-dd"))
                    }

                }
            }
            return ArrayList(listOfMissingDates).toList()
        }
    }
}
