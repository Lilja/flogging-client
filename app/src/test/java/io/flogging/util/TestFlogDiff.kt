package io.flogging.util

import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import org.joda.time.DateTime
import org.junit.Assert.*
import org.junit.Test

class TestFlogDiff {
    val vm = LogViewModel()
    val project = FloggingProject("", "8", "0")

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
        assertEquals(30, res[0].first)
        assertEquals(30, res[1].first)
        assertEquals(-30, res[2].first)
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
        assertEquals(-120, list[0].first)
        assertEquals(0, list[1].first)
    }

    @Test
    fun testDiffOnWeekend() {
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
        assertEquals(list[1].first, -240)
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
        assertEquals(-240, list2[0].first)
        assertEquals(0, list2[1].first)
        assertEquals(0, list2[2].first)
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
        assertEquals(-120, list[0].first)
        assertEquals(0, list[1].first)
        assertEquals(0, list[2].first)
        assertEquals(60, list[3].first)
    }

    @Test
    fun testMultipleWorkLogsOverSameDay() {
        val sheet = listOf(
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("11:00", Flogs.HH_MM_PATTERN),
                        0,
                        "03:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("11:15", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:15", Flogs.HH_MM_PATTERN),
                        0,
                        "01:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("12:15", Flogs.HH_MM_PATTERN),
                        DateTime.parse("14:15", Flogs.HH_MM_PATTERN),
                        0,
                        "02:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("14:15", Flogs.HH_MM_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        0,
                        "00:45",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("18:00", Flogs.HH_MM_PATTERN),
                        0,
                        "03:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )

        val project = FloggingProject("", "8", "0")
        val d = vm.getLogsWithDiff(sheet, project)
        val expected = listOf(-300, -240, -120, -120, 60)
        assertEquals(expected, d.map { it.first })
    }

    @Test
    fun testFlexInDay() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "12:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-20", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "12:00",
                        FloggingRow.Status.WORKED,
                        ""
                ), FloggingRow(
                DateTime.parse("2018-03-21", Flogs.YYYY_MM_DD_PATTERN),
                DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                0,
                "12:00",
                FloggingRow.Status.WORKED,
                ""
        ),
                FloggingRow(
                        DateTime.parse("2018-03-22", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "04:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-22", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("15:00", Flogs.HH_MM_PATTERN),
                        0,
                        "03:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )

        val expected = listOf(240, 480, 720, 240, 420)
        val actual = vm.getLogsWithDiff(list, project).map { it.first }
        assertEquals(expected, actual)
    }

    @Test
    fun testPublicHoliday() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2018-03-19", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                        "08:30",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-20", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("16:00", Flogs.HH_MM_PATTERN),
                        0,
                        "08:00",
                        FloggingRow.Status.PUBLIC_HOLIDAY,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-21", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "08:00",
                        FloggingRow.Status.PAID_LEAVE,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-22", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "01:00",
                        FloggingRow.Status.FLEX_TIME_OFF,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-22", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "07:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-22", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("00:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "01:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-23", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2018-03-23", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("12:00", Flogs.HH_MM_PATTERN),
                        0,
                        "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                )
        )

        val expected = listOf(30, 30, 30, -450, -30, 30, -210, 30)
        assertEquals(expected, vm.getLogsWithDiff(list, project).map { it.first })

    }

    @Test
    fun testOtherKeyword() {
        val list = listOf(
                FloggingRow(
                        DateTime.parse("2017-12-12", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("08:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("14:00", Flogs.HH_MM_PATTERN),
                        0,
                        "04:00",
                        FloggingRow.Status.WORKED,
                        ""
                ),
                FloggingRow(
                        DateTime.parse("2017-12-12", Flogs.YYYY_MM_DD_PATTERN),
                        DateTime.parse("14:00", Flogs.HH_MM_PATTERN),
                        DateTime.parse("18:00", Flogs.HH_MM_PATTERN),
                        0,
                        "04:00",
                        FloggingRow.Status.OTHER,
                        "Christmas party"
                )
        )
        val exp = listOf(-240, 0)
        assertEquals(exp, vm.getLogsWithDiff(list, project).map{it.first})
    }

}