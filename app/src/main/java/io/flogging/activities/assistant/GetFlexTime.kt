package io.flogging.activities.assistant

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import io.flogging.api.Flogging
import io.flogging.util.Prefs

class GetFlexTime : AppCompatActivity() {
    val uri = Uri.parse("android-app://io.flogging/get_flex_time")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uuid = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val prefs = Prefs(this)
        Flogging.getLogsForProject(prefs.activeProject.projectName, uuid, {
            val decimal = Flogging.getLogsWithDiff(it).last()
        })
    }
}
