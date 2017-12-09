package io.flogging.model

import org.joda.time.DateTime

data class FloggingRow (var timestamp: DateTime,
                        var startDate: DateTime,
                        var endDate : DateTime,
                        var breakMinutes: Int,
                        var decimal: String,
                        var status: Status,
                        var note : String = "") {
    enum class Status {
        WORKED, PAID_LEAVE, OTHER, FLEX_TIME_OFF
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
