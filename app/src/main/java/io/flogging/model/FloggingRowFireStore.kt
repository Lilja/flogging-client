package io.flogging.model

data class FloggingRowFireStore(var timestamp: String,
                                var startDate: String,
                                var endDate: String,
                                var breakMinutes: Int,
                                var decimal: String,
                                var status: String,
                                var note: String = "") {
    // From a real row to firestore row
    constructor(floggingRow: FloggingRow) : this(
            floggingRow.timestamp.toString("yyyy-MM-dd"),
            floggingRow.startDate.toString("HH:mm"),
            floggingRow.endDate.toString("HH:mm"),
            floggingRow.breakMinutes,
            floggingRow.decimal,
            floggingRow.status.text,
            floggingRow.note
    )
}