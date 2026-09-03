
package com.example.labourbook

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Show Splash Screen for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if user is already logged in
            if (auth.currentUser != null) {

                val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val role = appPrefs.getString("user_role", null)

                if (role == "worker") {
                    startActivity(Intent(this, WorkerMainActivity::class.java))
                } else if (role == "customer") {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                }

            } else {

                // User is not logged in -> Open Role Selection
                startActivity(Intent(this, RoleSelectionActivity::class.java))
            }

            // Close SplashActivity
            finish()

        }, 2000)
    }
}

