package io.flogging.util

import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import org.joda.time.DateTime
import org.junit.Assert.*
import org.junit.Test

class TestFlogDiff {
    val vm = LogViewModel()

    @Test
    fun testGeneratedDiff() {
        val project = FloggingProject("", "8", "0")
        val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "08:30",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-05", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-06", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        0,
                       "07:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val res = vm.getLogsWithDiff(k, project)
        assertEquals(res[0].first, 30)
        assertEquals(res[1].first, 30)
        assertEquals(res[2].first, -30)
    }

    @Test
    fun testPaidLeave() {
        val project = FloggingProject("", "8", "0")
         val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "06:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "02:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                )
        )
        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, -120)
        assertEquals(list[1].first, 0)
    }

    @Test
    fun testDiffOnWeekend(){
        val project = FloggingProject("", "8", "0")
        val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-11-30", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-01", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-02", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        0,
                       "07:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, 0)
        assertEquals(list[1].first, 0)
        assertEquals(list[2].first, 480)
        assertEquals(list[3].first, 420)
    }

    @Test
    fun testFlex() {
        val project = FloggingProject("", "8", "0")
        val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "09:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-05", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                )
        )
        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, 60)
        assertEquals(list[1].first, -420)
    }

    @Test
    fun testMultipleLogsAtSameDay() {
        val project = FloggingProject("", "8", "0")
        val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "01:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )
        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, -240)
        assertEquals(list[1].first, -300)
        assertEquals(list[2].first, 0)
    }

    @Test
    fun testMultiplelogs() {
        val project = FloggingProject("", "8", "0")
       val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )

        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, -240)
        assertEquals(list[1].first, 0)

        val doups = k + listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "01:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                )
        )

        val list2 = vm.getLogsWithDiff(doups, project)
        assertEquals(list2[0].first, -240)
        assertEquals(list2[1].first, 0)
        assertEquals(list2[2].first, -60)
    }

    @Test
    fun testMultiplePaidLeave() {
        val project = FloggingProject("", "8", "0")
        val k = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:30", Flogs.HH_MM_PATTERN),
                        0,
                       "06:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-04", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "02:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-05", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "08:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                ),
               FloggingRow(
                        DateTime.parse("2017-12-06", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                       "09:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                )
        )
        val list = vm.getLogsWithDiff(k, project)
        assertEquals(list[0].first, -120)
        assertEquals(list[1].first, 0)
        assertEquals(list[2].first, 0)
        assertEquals(list[3].first, 0)
    }
}