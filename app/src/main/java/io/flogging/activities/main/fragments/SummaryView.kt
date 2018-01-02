package io.flogging.activities.main.fragments

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import io.flogging.R
import io.flogging.activities.project.NewProject
import io.flogging.api.Flogging
import io.flogging.util.Flogs
import io.flogging.util.Prefs

class SummaryView : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("SummaryView", "OnCreateView")
        val root = inflater!!.inflate(R.layout.fragment_summary_view, container, false) as FrameLayout
        val pref = Prefs(activity)
        val listener = pref.configureListener {
            val btn = root.findViewById<Button>(R.id.summary_delete_project)
            btn.text = "Delete Project $it"
            Log.d("SummaryView", "Am i visible?" + this@SummaryView.isVisible)
            if (this@SummaryView.isVisible) {
                setupGraph(root)
                setupBtn(root)
            }
        }
        Log.d("SummaryView", "Registered callback")
        pref.registerListener(listener)
        setupGraph(root)
        setupBtn(root)
        return root
    }

    private fun setupBtn(root: FrameLayout) {
        val btn = root.findViewById<Button>(R.id.summary_delete_project)
        val pref = Prefs(activity)
        val proj = pref.activeProject.projectName
        btn.text = "Delete project $proj"
        btn.setOnClickListener {
            val alert = AlertDialog.Builder(activity).create()
            val pref = Prefs(activity)
            val proj = pref.activeProject.projectName
            val uid = FirebaseAuth.getInstance().uid.toString()
            alert.setTitle("Delete a project")
            alert.setMessage("Are you REALLY sure you want to delete the project $proj")

            alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", { _, _ -> })
            alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", { _, _ ->
                Flogging.deleteProject(proj, uid, { success ->
                    if (success) {
                        Flogging.getProjectsFromUser(uid, { projects ->
                            if (projects.isEmpty()) {
                                val intent = Intent(activity, NewProject::class.java)
                                startActivity(intent)
                            } else {
                                pref.activeProject = projects.last()
                            }

                        })
                    }
                })
            })
            alert.show()
        }
    }

    private fun setupGraph(root: FrameLayout) {
        val prefs = Prefs(activity)

        val uid = FirebaseAuth.getInstance().uid.toString()
        Log.d("SummaryView", "Getting project name")
        val projName = prefs.activeProject.projectName
        Log.d("SummaryView", "Getting project name: $projName")

        if (projName.isNotEmpty()) {
            Flogging.getLogsForProject(projName, uid, { logs ->
                val rows = Flogging.getLogsWithDiff(logs)
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
                }
            })
        }
    }

    private fun setupTotalHHMM(root: FrameLayout, decimal: Int) {
        val tv = root.findViewById<TextView>(R.id.summary_view_diff_hh_mm_diff)
        if (decimal < 0)
            tv.setTextColor(resources.getColor(R.color.red))
        if (decimal == 0)
            tv.setTextColor(resources.getColor(R.color.black))

        tv.text = Flogs.hhMMWithDiff(Flogs.minutesToHHMM(decimal))
    }

}
