package io.flogging.activities.main

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.flogging.R
import io.flogging.activities.activityrequests.ActivityRequestCodes
import io.flogging.activities.main.fragments.HistoricView
import io.flogging.activities.main.fragments.NewLog
import io.flogging.activities.main.fragments.SummaryView
import io.flogging.activities.main.viewmodels.LogViewModel
import io.flogging.activities.project.NewProject
import io.flogging.adapters.ViewPagerAdapter
import io.flogging.api.Flogging
import io.flogging.model.FloggingProject
import io.flogging.model.FloggingRow
import io.flogging.util.Prefs
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {
    companion object {
        val INTENT_UUID = "INTENT_UUID_EXTRAS"
        val INTENT_NAME = "INTENT_DISPLAY_NAME_EXTRAS"
    }

    private val context = this
    var vm: LogViewModel? = null
    var disposable: Disposable? = null

    private fun setSpinnerListener(spinner: Spinner, prefs: Prefs) {
        val uuid = vm!!.uuid!!

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.d("OnItemSelected", "Clicked!")
                val item = p0!!.getItemAtPosition(p2)
                when (item.toString()) {
                    "* New Project" -> {
                        val intent = Intent(context, NewProject::class.java)
                        startActivityForResult(intent, ActivityRequestCodes.NEW_LOG_PROJECT)
                    }
                    else -> {
                        Log.d("OnClickItemSelected", "Item: $item")
                        val projName = item.toString()
                        val matchingProject = vm!!.projects.value.filter { it.projectName == projName }

                        if (matchingProject.size == 1) {
                            val currentLog = matchingProject.first()
                            Log.d("OnClickItemSelected", "Active Project" + prefs.activeProject.projectName)
                            Log.d("OnClickItemSelected", currentLog.projectName)
                            prefs.activeProject = currentLog
                            vm!!.loadLogsForProject(prefs.activeProject.projectName, uuid)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("OnActivityResult", "Result")
        super.onActivityResult(requestCode, resultCode, data)
        val prefs = Prefs(this)
        if (requestCode == ActivityRequestCodes.NEW_LOG_PROJECT) {
            if (resultCode == ActivityRequestCodes.NEW_LOG_PROJECT_SUCCESS) {
                getProjects(prefs)
            } else {
                val spinner = findViewById<Spinner>(R.id.main_spinner)
                setSpinnerSelection(spinner, vm!!.projects.value, prefs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "OnCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val uuid = intent!!.getStringExtra(MainActivity.INTENT_UUID)
        val name = intent.getStringExtra(MainActivity.INTENT_NAME)

        val tb: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(tb)

        vm = ViewModelProviders.of(this).get(LogViewModel::class.java)
        vm!!.uuid = uuid
        vm!!.displayName = name

        val prefs = Prefs(context)

        // Set up viewpager
        val pager = findViewById<ViewPager>(R.id.main_view_pager)

        pager.adapter = ViewPagerAdapter(
                supportFragmentManager,
                listOf(
                        NewLog(),
                        SummaryView(),
                        HistoricView()
                ),
                listOf("New log", "Summary", "Historic")
        )
        pager.currentItem = 1

        findViewById<TabLayout>(R.id.main_tab_layout)
                .setupWithViewPager(pager)

        getProjects(prefs)
        vm!!.loadLogsForProject(prefs.activeProject.projectName, uuid)
        setUpIfFirstStartUp()
    }

    private fun getProjects(prefs: Prefs) {
        vm!!.loadProjects(vm!!.uuid!!)

        disposable = vm!!.projects.subscribe { rows ->
            Log.d("GetProjects", rows.toString())
            val spinner = findViewById<Spinner>(R.id.main_spinner)

            val newMap = rows.map { it.projectName }.plus("* New Project")
            val arrayAdapter = ArrayAdapter<String>(
                    this,
                    R.layout.project_layout_list,
                    newMap
            )
            Log.d("GetProjects", "Setting adapter")
            spinner.adapter = arrayAdapter
            setSpinnerSelection(spinner, rows, prefs)

            Log.d("GetProjects", "Setting listener")
            setSpinnerListener(spinner, prefs)
        }
    }

    private fun setSpinnerSelection(spinner: Spinner,
                                    rows: List<FloggingProject>,
                                    prefs: Prefs) {
        Log.d("SpinnerSelection", "Setting spinner selection")
        spinner.setSelection(rows.indexOf(prefs.activeProject))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    private fun setUpIfFirstStartUp() {
        val uuid = vm!!.uuid!!
        val name = vm!!.displayName!!

        Flogging.initUser(uuid, name, {
            val intent = Intent(this, NewProject::class.java)
            startActivity(intent)
        })
    }

}
