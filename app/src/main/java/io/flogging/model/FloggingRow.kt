package io.flogging.model

import org.joda.time.DateTime

data class FloggingRow(var timestamp: DateTime,
                       var startDate: DateTime,
                       var endDate: DateTime,
                       var breakMinutes: Int,
                       var decimal: String,
                       var status: Status,
                       var note: String = "") {

    enum class Status(val text: String) {
        WORKED("Worked"),
        PAID_LEAVE("Paid leave"),
        FLEX_TIME_OFF("Flex time off"),
        PUBLIC_HOLIDAY("Public holiday"),
        OTHER("Other");

        companion object {
            fun fromValue(status: String): Status {
                return Status.values().first {
                    it.text == status
                }
            }
        }
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
            Status.fromValue(status),
            note
    )


}
