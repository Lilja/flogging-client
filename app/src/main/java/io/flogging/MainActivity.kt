package io.flogging

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("__", "About to do stuff::")
        setContentView(R.layout.activity_main)

        val tb: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(tb)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

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


}
