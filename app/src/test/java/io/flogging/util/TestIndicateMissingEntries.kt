package io.flogging.util

import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class TestIndicateMissingEntries {
    val vm = LogViewModel()

    @Test
    fun testIndicateMissingEntries() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2018-01-01", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-01-02", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-01-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val res = vm.indicateMissingEntries(
                list
        )

        val exp = listOf(DateTime.parse("2018-01-03",
                DateTimeFormat.forPattern("YYYY-MM-DD")))
        assertEquals(exp, res)
    }

    @Test
    fun testMultipleMissingLogs() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2018-01-01", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-01-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val res = vm.indicateMissingEntries(
                list
        )

        val p = DateTimeFormat.forPattern("YYYY-MM-DD")
        val exp = listOf(
                DateTime.parse("2018-01-02", p),
                DateTime.parse("2018-01-03", p)
        )
        assertEquals(exp, res)
    }

    @Test
    fun testSameDayLogs() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2018-01-01", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-01-01", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        40,
                        "00:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val res = vm.indicateMissingEntries(
                list
        )

        val p = DateTimeFormat.forPattern("YYYY-MM-DD")
        val exp = listOf<DateTime>()
        assertEquals(exp, res)
    }
}