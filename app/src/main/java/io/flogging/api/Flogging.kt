package io.flogging.api

import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Global

class Flogging {
    companion object {
        fun getLogsWithDiff(rows: List<FloggingRow>): List<Pair<Int, FloggingRow>> {
            val perDay = rows
                    .sortedWith(compareBy(FloggingRow::timestamp))
                    .groupBy { it.timestamp.toString("dd") }
            var previousEntryDiff = 0
            val mutator = mutableListOf<Pair<Int, FloggingRow>>()
            perDay.map { (_, days) ->
                var todaysDiff = previousEntryDiff
                var hasSubtractedWithDailyLimit = false
                for((_, entry) in days.withIndex()) {
                    val listOfHHMM = entry.decimal.split(":")
                    val hours = (if(listOfHHMM.size==2) listOfHHMM[0].toInt() else entry.decimal.toInt())*60
                    val minutes = if(listOfHHMM.size==2) listOfHHMM[1].toInt() else 0
                    var calc = 0
                    // Only calculate flex
                    if(entry.status == FloggingRow.Status.WORKED) {
                        calc = if(!Flogs.isWorkingDay(entry.timestamp)) {
                            // If working on a weekend, we should just add to the difference.
                            (hours + minutes) + previousEntryDiff
                        } else if(!hasSubtractedWithDailyLimit) {
                            // If the current of this is day is the first one we should subtract by hours_per_day
                            hasSubtractedWithDailyLimit = true
                            todaysDiff = ((hours + minutes) - (Global.HOURS_PER_DAY * 60) + Global.MINUTES_PER_DAY) + previousEntryDiff
                            todaysDiff
                        } else {
                            (hours + minutes) + todaysDiff
                        }
                    } else if(entry.status == FloggingRow.Status.FLEX_TIME_OFF) {
                        calc = previousEntryDiff - (hours + minutes)
                    }
                    previousEntryDiff = calc
                    mutator.add(Pair(calc, entry))
                }
            }
            return ArrayList(mutator)
        }
    }
}