package io.flogging.activities.main.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

import io.flogging.R
import io.flogging.api.Flogging
import io.flogging.util.Flogs

class SummaryView : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.fragment_summary_view, container, false) as FrameLayout
        setupGraph(root)
        return root
    }


    private fun setupGraph(root : FrameLayout) {
        Flogging.getLogsForProject("funnel", {
            logs ->
            Log.d("SetupGraph", "Got stuff")
            val rows = Flogging.getLogsWithDiff(logs)
            val lengths = rows.size
            val smth = rows.slice(listOf(lengths-5, lengths-1))
            setupTotalHHMM(root, rows.last().first)

            val graph = root.findViewById<GraphView>(R.id.summary_graph)

            val series = LineGraphSeries(
                    smth.map {
                        DataPoint(it.second.timestamp.millis.toDouble(), it.first.toDouble())
                    }.toTypedArray()
            )
            graph.addSeries(series)
        })
    }

    private fun setupTotalHHMM(root : FrameLayout, decimal: Int) {
        root.findViewById<TextView>(R.id.summary_view_diff_hh_mm_diff).text = Flogs.minutesToHHMM(decimal)
    }

}
