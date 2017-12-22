package io.flogging.testing

import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.util.*

class GenerateLogs {
    companion object {
        private fun generateLog(forTimestamp: DateTime): FloggingRow {
            val rnd = Random()
            val str = listOf(
                    "0" + listOf(6, 7, 8)[rnd.nextInt(3)] + ":" + rnd.nextInt(6) + "" + rnd.nextInt(10),
                    "1" + rnd.nextInt(10) + ":" + rnd.nextInt(6) + "" + rnd.nextInt(10)
            )

            val breakMinutes = rnd.nextInt(60)
            val startDate = DateTime.parse(str[0], Flogs.HH_MM_PATTERN)
            val endDate = DateTime.parse(str[1], Flogs.HH_MM_PATTERN).minusMinutes(breakMinutes)

            val hours = Hours.hoursBetween(startDate, endDate).hours
            val minutes = Minutes.minutesBetween(startDate, endDate).minutes % 60

            val hoursStr = if (hours < 10) "0" + hours else hours.toString()
            val minutesStr = if (minutes < 9) "0" + minutes else minutes.toString()

            return FloggingRow(forTimestamp, startDate, endDate, breakMinutes,
                    (hoursStr + ":" + minutesStr), FloggingRow.Status.WORKED, "")
        }

        fun generateFlogs(count: Int, from: Int): List<FloggingRow> {
            var generatedTimestamp = DateTime.now().minusDays(from)
            val output = mutableListOf<FloggingRow>()
            for (i in 1..count) {
                output.add(generateLog(generatedTimestamp))
                generatedTimestamp = generatedTimestamp.plusDays(1)
                if (Flogs.isWorkingDay(generatedTimestamp))
                    generatedTimestamp = generatedTimestamp.plusDays(2)

            }
            return output
        }
    }
}