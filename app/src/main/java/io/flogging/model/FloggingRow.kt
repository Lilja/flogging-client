package io.flogging.model

import io.flogging.R
import org.joda.time.DateTime

data class FloggingRow(var timestamp: DateTime,
                       var startDate: DateTime,
                       var endDate: DateTime,
                       var breakMinutes: Int,
                       var decimal: String,
                       var status: Status,
                       var note: String = "") {
    enum class Status(value: Int) {
        WORKED(R.string.flog_status_type_w),
        PAID_LEAVE(R.string.flog_status_type_pl),
        FLEX_TIME_OFF(R.string.flog_status_type_fto),
        OTHER(R.string.flog_status_type_other)
    }

    constructor(timestamp: DateTime,
                startDate: DateTime,
                endDate: DateTime,
                breakMinutes: Int,
                decimal: String,
                status: String,
                note: String) : this(
            timestamp,
            startDate,
            endDate,
            breakMinutes,
            decimal,
            Status.valueOf(status),
            note
    )
}
