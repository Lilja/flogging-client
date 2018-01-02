package io.flogging.activities.main

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.flogging.R
import io.flogging.activities.main.fragments.HistoricView
import io.flogging.activities.main.fragments.NewLog
import io.flogging.activities.main.fragments.SummaryView
import io.flogging.activities.project.NewProject
import io.flogging.adapters.ViewPagerAdapter
import io.flogging.api.Flogging
import io.flogging.util.Prefs

class MainActivity : AppCompatActivity() {

    private val context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("__", "About to do stuff::")
        setContentView(R.layout.activity_main)

        val tb: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(tb)

        val sh = getSharedPreferences("io.flogging.prefs", 0)
        Log.d("Main", sh.all.entries.toString())

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

        val tabs = findViewById<TabLayout>(R.id.main_tab_layout)
        tabs.setupWithViewPager(pager)

        val spinner = findViewById<Spinner>(R.id.main_spinner)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val item = p0!!.getItemAtPosition(p2)
                when (item.toString()) {
                    "* New Project" -> {
                        val intent = Intent(context, NewProject::class.java)
                        startActivity(intent)
                    }
                    else -> {
                        Log.d("OnClickItemSelected", "Another project selected")
                        val k: String = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception()
                        val projName = item.toString()
                        Flogging.getProjectsFromUser(k, { rows ->
                            Log.d("OnClickItemSelected", "Gotten project from db")
                            val currentLog = rows.first { it.projectName == projName }
                            Prefs(context)
                                    .activeProject = currentLog
                        })

                    }
                }
            }
        }

        setUpIfFirstStartUp()
        getProjects()
    }

    override fun onResume() {
        super.onResume()
        getProjects()
    }

    private fun getProjects() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Abo!")
        Flogging.getProjectsFromUser(
                uid,
                { rows ->
                    Log.d("GetProjects", "Done")
                    Log.d("GetProjects", rows.toString())
                    val spinner = findViewById<Spinner>(R.id.main_spinner)

                    val newMap = rows.map { it.projectName }.plus("* New Project")
                    val aa = ArrayAdapter<String>(this,
                            R.layout.project_layout_list,
                            newMap)
                    spinner.adapter = aa
                }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    private fun setUpIfFirstStartUp() {
        val fb: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (fb != null) {
            val uuid = fb.uid
            val name = fb.displayName.toString()
            FirebaseFirestore.getInstance().document("users/$uuid")
                    .get()
                    .addOnCompleteListener { docSnapshot ->
                        if (!docSnapshot.result.exists()) {
                            val map = hashMapOf<String, Any>(
                                    "name" to name,
                                    "uuid" to uuid
                            )
                            val db = FirebaseFirestore.getInstance()
                            db.document("users/$uuid")
                                    .set(map)
                                    .addOnSuccessListener {
                                        val intent = Intent(this, NewProject::class.java)
                                        startActivity(intent)
                                    }
                        }
                    }


        }

    }

    /*
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.toolbar_add_log -> {
                val intent = Intent(applicationContext, NewLog::class.java)
                startActivity(intent)
                return true
            }
            R.id.toolbar_historic_view -> {
                val intent = Intent(applicationContext, HistoricView::class.java)
                startActivity(intent)
                return true
            }
        }
        return true
    }
    */
}
