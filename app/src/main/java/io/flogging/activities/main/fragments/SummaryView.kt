package io.flogging.activities.main.fragments

import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
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

class SummaryView : Fragment() {
    var vm: LogViewModel? = null
    var sub: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("SummaryView", "OnCreateView")
        val root = inflater!!.inflate(R.layout.fragment_summary_view, container, false) as FrameLayout
        vm = ViewModelProviders.of(activity).get(LogViewModel::class.java)
        sub = vm!!.logs.subscribe {
            if (this@SummaryView.isVisible) {
                setupGraph(root, it)
                setupBtn(root)
            }
        }
        return root
    }

    override fun onPause() {
        if (sub!=null) {
            sub!!.dispose()
        }
        super.onPause()
    }

    private fun setupBtn(root: FrameLayout) {
        val btn = root.findViewById<Button>(R.id.summary_delete_project)
        val pref = Prefs(activity)
        val proj = pref.activeProject.projectName
        btn.text = "Delete project $proj"
        btn.setOnClickListener {
            val alert = AlertDialog.Builder(activity).create()
            val uid = FirebaseAuth.getInstance().uid.toString()
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

    private fun setupGraph(root: FrameLayout, logs : List<FloggingRow>) {
        val prefs = Prefs(activity)

        val uid = FirebaseAuth.getInstance().uid.toString()
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
            tv.setTextColor(resources.getColor(R.color.red))
        if (decimal == 0)
            tv.setTextColor(resources.getColor(R.color.black))
        if (decimal > 0)
            tv.setTextColor(resources.getColor(R.color.green))

        tv.text = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(decimal))
    }

}
