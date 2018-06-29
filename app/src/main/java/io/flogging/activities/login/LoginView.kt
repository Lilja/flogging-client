package io.flogging.activities.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.util.Arrays.asList
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.ResultCodes
import com.google.firebase.auth.FirebaseAuth
import io.flogging.activities.main.MainActivity


class Login : AppCompatActivity() {
    private val RC_SIGN_IN = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginView", "OnCreate")
        val providers = asList(
                AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
        )

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("LoginView", "OnActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            if (resultCode == ResultCodes.OK) {
                val mainIntent = Intent(this, MainActivity::class.java)
                val auth = FirebaseAuth.getInstance().currentUser!!
                Log.d("LoginView", auth.toString())
                Log.d("LoginView", auth.uid.toString())
                Log.d("LoginView", auth.displayName.toString())
                val uuid = auth.uid
                val displayName = auth.displayName

                mainIntent.putExtra(MainActivity.INTENT_UUID, uuid)
                mainIntent.putExtra(MainActivity.INTENT_NAME, displayName)
                startActivity(mainIntent)
            }

        }
    }

}
