package io.flogging.util

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.model.FloggingRow
import io.flogging.model.FloggingRowFireStore
import io.flogging.testing.GenerateLogs
import org.joda.time.*

class Foo {
    companion object {
        fun getLogsWithDiff(rows: List<FloggingRow>): List<Pair<Int, FloggingRow>> {
            val perDay = rows
                    .sortedWith(compareBy(FloggingRow::timestamp))
                    .groupBy { it.timestamp.toString("dd") }
            var previousEntryDiff = 0
            val mutator = mutableListOf<Pair<Int, FloggingRow>>()
            perDay.map { (_, days) ->
                for((index, entry) in days.withIndex()) {
                    val listOfHHMM = entry.decimal.split(":")
                    val hours = (if(listOfHHMM.size==2) listOfHHMM[0].toInt() else entry.decimal.toInt())*60
                    val minutes = if(listOfHHMM.size==2) listOfHHMM[1].toInt() else 0
                    var calc = 0
                    // Only calculate flex
                    if(entry.status == FloggingRow.Status.WORKED) {
                        calc = if(!Flogs.isWorkingDay(entry.timestamp)) {
                            // If working on a weekend, we should just add to the difference.
                            (hours + minutes) + previousEntryDiff
                        } else if(index == 0) {
                            // If the current of this is day is the first one we should subtract by hours_per_day
                            (hours + minutes) - (Global.HOURS_PER_DAY * 60) + Global.MINUTES_PER_DAY
                        } else {
                            (hours + minutes) + previousEntryDiff
                        }
                    } else if(entry.status == FloggingRow.Status.FLEX_TIME_OFF) {
                        // If using flex. Subtract the used flex.
                        calc = previousEntryDiff - (hours + minutes)
                    }
                    previousEntryDiff = calc
                    mutator.add(Pair(calc, entry))
                }
            }
            return ArrayList(mutator)
        }

        fun uploadRowsToFirestore(rows: List<Pair<String, FloggingRowFireStore>>, fbRef : CollectionReference) {
           rows.forEach { (index, flog) ->
                Log.d("FLOGFS", flog.toString())
                fbRef.document(index).set(flog)
            } 
        }

        fun flexMeUp() {
            val rowsWithIndex = GenerateLogs
                    .generateFlogs(10, 90)
                    .map {
                        Pair(Flogs.YYYY_MM_DD_PATTERN.print(it.timestamp) + " " +
                             Flogs.HH_MM_PATTERN.print(it.startDate) + " " +
                             Flogs.HH_MM_PATTERN.print(it.endDate),
                             FloggingRowFireStore(it))
                    }

            val instance = FirebaseFirestore.getInstance()
            val ref = instance.collection("projects/funnel/timestamps")
            uploadRowsToFirestore(rowsWithIndex, ref)

            ref.get().addOnCompleteListener({ task ->
                run {
                    val fbResult = task.result

                    val resultLogs = fbResult.documents.map {
                        val values = it.data
                        FloggingRow(
                                DateTime.parse(values.getOrDefault("timestamp", "").toString(), Flogs.YYYY_MM_DD_PATTERN),
                                DateTime.parse(values.getOrDefault("startDate", "").toString(), Flogs.HH_MM_PATTERN),
                                DateTime.parse(values.getOrDefault("endDate", "").toString(), Flogs.HH_MM_PATTERN),
                                values.getOrDefault("breakMinutes", "").toString().toInt(),
                                values.getOrDefault("decimal", "").toString(),
                                values.getOrDefault("status", FloggingRow.Status.WORKED).toString(),
                                values.getOrDefault("note", "").toString()
                        )
                    }

                    resultLogs.forEach { Log.d("Gather", it.toString()) }
                    getLogsWithDiff(resultLogs)
                }
            })
        }
    }

}