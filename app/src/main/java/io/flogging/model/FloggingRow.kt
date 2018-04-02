package io.flogging.model

import android.util.Log
import io.flogging.R
import org.joda.time.DateTime

data class FloggingRow(var timestamp: DateTime,
                       var startDate: DateTime,
                       var endDate: DateTime,
                       var breakMinutes: Int,
                       var decimal: String,
                       var status: Status,
                       var note: String = "") {

    enum class Status(val text: String) {
        WORKED("WORKED"),
        PAID_LEAVE("PAID_LEAVE"),
        FLEX_TIME_OFF("FLEX_TIME_OFF"),
        PUBLIC_HOLIDAY("PUBLIC_HOLIDAY"),
        OTHER("OTHER");

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
