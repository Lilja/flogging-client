package io.flogging.activities.main

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.flogging.R
import io.flogging.activities.main.fragments.HistoricView
import io.flogging.activities.main.fragments.NewLog
import io.flogging.activities.main.fragments.SummaryView
import io.flogging.adapters.ViewPagerAdapter

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("__", "About to do stuff::")
        setContentView(R.layout.activity_main)

        val tb: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(tb)

        // Set up viewpager
        val pager = findViewById<ViewPager>(R.id.main_view_pager)

        pager.adapter = ViewPagerAdapter(
                supportFragmentManager, listOf(
                NewLog(),
                SummaryView(),
                HistoricView()
            ),
                listOf("New log", "Summary", "Historic")
        )

        val tabs = findViewById<TabLayout>(R.id.main_tab_layout)
        tabs.setupWithViewPager(pager)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
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
