package io.flogging.api

import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.model.FloggingRow
import io.flogging.model.FloggingRowFireStore
import io.flogging.model.FloggingProject
import io.flogging.util.Flogs
import org.joda.time.DateTime
import org.joda.time.Minutes
import org.joda.time.format.DateTimeFormat

class Flogging {

    companion object {
        val HH_MM_PATTERN = DateTimeFormat.forPattern("HH:mm")
        val YYYY_MM_DD_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd")
        val HEADER_PATTERN = "E, d MMM y"

        fun calculateDiff(startTime: String, endTime: String, breakMinutes: Int): Int {
            val d1 = DateTime.parse(startTime, Flogs.HH_MM_PATTERN)
            val d2 = DateTime.parse(endTime, Flogs.HH_MM_PATTERN).minusMinutes(breakMinutes)

            return Minutes.minutesBetween(d1, d2).minutes
        }

        private fun createFloggingRow(timestamp: String,
                                      startTime: String,
                                      endTime: String,
                                      breakMinutes: Int,
                                      typeOfLog: String,
                                      note: String)
                : FloggingRow {
            return FloggingRow(
                    DateTime.parse(timestamp, Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(startTime, Flogs.HH_MM_PATTERN),
                    DateTime.parse(endTime, Flogs.HH_MM_PATTERN),
                    breakMinutes,
                    Flogs.minutesToHHMM(calculateDiff(startTime, endTime, breakMinutes)),
                    FloggingRow.Status.fromValue(typeOfLog),
                    note
            )
        }

        fun createIndex(floggingRow: FloggingRow): String {
            return Flogs.YYYY_MM_DD_PATTERN.print(floggingRow.timestamp) + " " +
                    Flogs.HH_MM_PATTERN.print(floggingRow.startDate) + " " +
                    Flogs.HH_MM_PATTERN.print(floggingRow.endDate)
        }

        fun createProject(projectName: String,
                          dailyHour: String,
                          dailyMinutes: String,
                          uid: String,
                          succeeded: (status: Boolean, message: String) -> Unit) {
            val projectSettings = mapOf(
                    "daily_hour" to dailyHour,
                    "daily_minute" to dailyMinutes,
                    "name" to projectName
            )

            val instance = FirebaseFirestore.getInstance()
            instance
                    .document("users/$uid/projects/$projectName")
                    .set(projectSettings)
                    .addOnCompleteListener {
                        succeeded(it.isSuccessful, it.exception?.message ?: "")
                    }
        }

        fun getProjectsFromUser(uuid: String, complete: (projects: List<FloggingProject>) -> Unit) {
            FirebaseFirestore.getInstance().collection("users/$uuid/projects")
                    .get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                           complete(it.result.map { firebaseProjectToFloggingProject(it.data) })
                        } else {
                            complete(listOf())
                        }
                    }
        }

        private fun firebaseRowToFloggingRow(values: MutableMap<String, Any>): FloggingRow {
            return FloggingRow(
                    DateTime.parse(values.getOrDefault("timestamp", "").toString(), Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(values.getOrDefault("startDate", "").toString(), Flogs.HH_MM_PATTERN),
                    DateTime.parse(values.getOrDefault("endDate", "").toString(), Flogs.HH_MM_PATTERN),
                    values.getOrDefault("breakMinutes", "").toString().toInt(),
                    values.getOrDefault("decimal", "").toString(),
                    values.getOrDefault("status", FloggingRow.Status.WORKED).toString(),
                    values.getOrDefault("note", "").toString()
            )
        }

        private fun firebaseProjectToFloggingProject(values: MutableMap<String, Any>): FloggingProject {
            return FloggingProject(
                    values.getOrDefault("name", "Default").toString(),
                    values.getOrDefault("daily_hour", "Default").toString(),
                    values.getOrDefault("daily_minute", "Default").toString()
            )
        }

        fun createLogEntryFromObject(projectName: String,
                                     uuid: String,
                                     log: FloggingRow,
                                     success: (status: Boolean, reason: String) -> Unit) {
            createLogEntry(
                    projectName,
                    uuid,
                    log.timestamp.toString(YYYY_MM_DD_PATTERN),
                    log.startDate.toString(HH_MM_PATTERN),
                    log.endDate.toString(HH_MM_PATTERN),
                    log.breakMinutes,
                    log.status.text,
                    log.note,
                    success
            )
        }

        private fun createLogEntry(projectName: String,
                                   uuid: String,
                                   timestamp: String,
                                   startTime: String,
                                   endTime: String,
                                   breakMinutes: Int,
                                   typeOfLog: String,
                                   note: String,
                                   success: (status: Boolean, reason: String) -> Unit) {
            val d1 = DateTime.parse(startTime, Flogs.HH_MM_PATTERN)
            val d2 = DateTime.parse(endTime, Flogs.HH_MM_PATTERN)
            if (d2.millis < d1.millis)
                throw IllegalArgumentException("Start is greater(sooner) than end time")
            if ((d2.millis - breakMinutes * 60) - d1.millis < 0)
                throw IllegalArgumentException("Break is larger than start time and end time")

            val row = createFloggingRow(timestamp, startTime, endTime, breakMinutes, typeOfLog, note)
            val index = createIndex(row)
            val obj = FloggingRowFireStore(row)

            val instance = FirebaseFirestore.getInstance()
            instance
                    .collection("/users/$uuid/projects/$projectName/timestamps")
                    .whereEqualTo("timestamp", timestamp)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val k = querySnapshot.documents.map {
                            firebaseRowToFloggingRow(it.data)
                        }.map {
                            val start1 = it.startDate
                            val start2 = it.endDate
                            (start1 <= d2) and (d1 <= start2)
                        }.contains(true)

                        if (k) {
                            success(false,
                                    "The log intersected on something that has already been created")
                        } else {
                            instance
                                    .document("/users/$uuid/projects/$projectName/timestamps/$index")
                                    .set(obj)
                                    .addOnCompleteListener {
                                        success(it.isSuccessful, "")
                                    }

                        }
                    }
                    .addOnFailureListener {
                        success(false, "Log already exists")
                    }
        }

        fun deleteLogEntry(projectName: String,
                           uid: String,
                           log: FloggingRow,
                           completed: (status: Boolean, message: String) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            val index = createIndex(log)
            instance.document("/users/$uid/projects/$projectName/timestamps/$index")
                    .delete()
                    .addOnCompleteListener {
                        completed(it.isSuccessful, it.exception?.message ?: "")
                    }
        }

        fun deleteProject(projectName: String,
                          uid: String,
                          succeeded: (status: Boolean, message: String) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            instance.document("/users/$uid/projects/$projectName")
                    .delete()
                    .addOnCompleteListener {
                        succeeded(it.isSuccessful, it.exception?.message ?: "")
                    }
        }

        private fun timesToFloggingRow(timestamp: String, startTime: String, endTime: String): FloggingRow {
            return FloggingRow(
                    DateTime.parse(timestamp, Flogs.YYYY_MM_DD_PATTERN),
                    DateTime.parse(startTime, Flogs.HH_MM_PATTERN),
                    DateTime.parse(endTime, Flogs.HH_MM_PATTERN),
                    0,
                    "",
                    FloggingRow.Status.WORKED,
                    ""
            )
        }

        fun getLogsForProject(projectName: String,
                              uuid: String,
                              callback: (rows: List<FloggingRow>) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            instance.collection("/users/$uuid/projects/$projectName/timestamps")
                    .get()
                    .addOnCompleteListener { task ->
                        val results = task.result
                        val records = results.documents.map {
                            firebaseRowToFloggingRow(it.data)
                        }

                        callback(records)
                    }

        }

        fun getSpecifcLogForProject(projectName: String,
                                    uuid: String,
                                    uniqueKey: String,
                                    callback: (rows: List<FloggingRow>) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            instance.document("/users/$uuid/projects/$projectName/timestamps/$uniqueKey")
                    .get()
                    .addOnCompleteListener { task ->
                        val results = task.result
                        val record = firebaseRowToFloggingRow(results.data)

                        callback(listOf(record))
                    }

        }

        fun initUser(uuid: String,
                     name: String,
                     callback: (success: Boolean, message: String) -> Unit) {
            val instance = FirebaseFirestore.getInstance()
            instance.document("users/$uuid")
                    .get()
                    .addOnCompleteListener { docSnapshot ->
                        if (!docSnapshot.result.exists()) {
                            val map = hashMapOf<String, Any>(
                                    "name" to name,
                                    "uuid" to uuid
                            )
                            instance.document("users/$uuid")
                                    .set(map)
                                    .addOnCompleteListener {
                                        callback(it.isSuccessful, it.exception?.message ?: "")
                                    }
                        }
                    }
        }

        fun updateLog(projectName: FloggingProject,
                      user: String,
                      oldUniqueKey: String,
                      uniqueKey: String,
                      log: FloggingRow,
                      success: (b: Boolean, s: String) -> Unit) {
            val fbLog = FloggingRowFireStore(log)
            val instance = FirebaseFirestore.getInstance()
            instance.document("/users/$user/projects/${projectName.projectName}/timestamps/$oldUniqueKey")
                    .delete()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            instance.document("/users/$user/projects/${projectName.projectName}/timestamps/$uniqueKey")
                                    .set(fbLog)
                                    .addOnCompleteListener {
                                        success(it.isSuccessful, it.exception?.message ?: "")
                                    }
                        } else {
                            success(false, it.exception?.message ?: "")
                        }
                    }
        }
    }

}