package io.flogging.activities.main.fragments

import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import io.flogging.R
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.activities.project.NewProject
import io.flogging.api.Flogging
import io.flogging.model.FloggingRow
import io.flogging.util.Flogs
import io.flogging.util.Prefs
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.w3c.dom.Text

class SummaryView : Fragment() {
    var vm: LogViewModel? = null
    var sub: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("SummaryView", "OnCreateView")
        val root = inflater!!.inflate(R.layout.summary_view, container, false) as FrameLayout
        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)

        return root
    }

    override fun onStart() {
        super.onStart()
        val root = view!! as FrameLayout

        sub = vm!!.logs.subscribe {
            Thread(Runnable {
                val sortedLogs = it.sortedBy { it.timestamp }
                val logs = vm!!.indicateMissingEntries(it)
                activity.runOnUiThread {
                    setupGraph(root, sortedLogs)
                    setupBtn(root)
                    buildMissingLogEntries(root, logs)
                }
            }).start()
        }
    }

    override fun onStop() {
        if (sub != null) {
            sub!!.dispose()
        }

        super.onStop()
    }

    private fun setupBtn(root: FrameLayout) {
        val btn = root.findViewById<Button>(R.id.summary_delete_project)
        val pref = Prefs(activity)
        val proj = pref.activeProject.projectName
        btn.text = "Delete project $proj"
        btn.setOnClickListener {
            val alert = AlertDialog.Builder(activity).create()
            val uid = pref.uid
            alert.setTitle("Delete a project")
            alert.setMessage("Are you REALLY sure you want to delete the project $proj")

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", { _, _ -> })
            alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", { _, _ ->
                Flogging.deleteProject(proj, uid, { success, _ ->
                    if (success) {
                        Flogging.getProjectsFromUser(uid, { projects ->
                            if (projects.isEmpty()) {
                                val intent = Intent(activity, NewProject::class.java)
                                startActivity(intent)
                            } else {
                                pref.activeProject = projects.last()
                                vm!!.loadProjects(uid)
                            }

                        })
                    }
                })
            })
            alert.show()
        }
    }

    private fun setupGraph(root: FrameLayout, logs: List<FloggingRow>) {
        val prefs = Prefs(activity)

        Log.d("SummaryView", "Getting project name")
        val projName = prefs.activeProject.projectName
        Log.d("SummaryView", "Getting project name: $projName")

        if (projName.isNotEmpty()) {
            val rows: List<Pair<Int, FloggingRow>> = vm!!.getLogsWithDiff(logs, prefs.activeProject)
            if (rows.isNotEmpty()) {
                setupTotalHHMM(root, rows.last().first)

                val graph = root.findViewById<GraphView>(R.id.summary_graph)

                val series = LineGraphSeries(
                        rows.map {
                            DataPoint(it.second.timestamp.millis.toDouble(), it.first.toDouble())
                        }.toTypedArray()
                )
                graph.addSeries(series)
                graph.gridLabelRenderer.labelFormatter = (DateAsXAxisLabelFormatter(activity))
            } else {
                setupTotalHHMM(root, 0)
                val graph = root.findViewById<GraphView>(R.id.summary_graph)
                graph.removeAllSeries()
            }
        }
    }

    private fun setupTotalHHMM(root: FrameLayout, decimal: Int) {
        Log.d("SetupTotalHHMM", "Decimal: $decimal")
        val tv = root.findViewById<TextView>(R.id.summary_view_diff_hh_mm_diff)
        if (decimal < 0)
            tv.setTextColor(ContextCompat.getColor(activity, R.color.red))
        if (decimal == 0)
            tv.setTextColor(ContextCompat.getColor(activity, R.color.black))
        if (decimal > 0)
            tv.setTextColor(ContextCompat.getColor(activity, R.color.green))

        tv.text = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(decimal))
    }

    private fun buildMissingLogEntries(root: FrameLayout, missingEntries: List<DateTime>) {
        val table = root.findViewById<TableLayout>(R.id.summary_missing_log_table)
        val tableRow = root.findViewById<TextView>(R.id.summary_missing_view_header)
        table.removeAllViews()

        if (missingEntries.isNotEmpty()) {
            tableRow.text = resources.getString(R.string.days_missed_logging)

            Log.d("SummaryView", "Build missing entries " + missingEntries)
            missingEntries.forEach {
                val btn = TextView(activity)
                btn.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
                btn.text = it.toString(Flogs.YYYY_MM_DD_PATTERN)
                table.addView(btn)
            }
        } else {
            tableRow.text = resources.getString(R.string.no_days_missed_logging)
        }
    }

}
