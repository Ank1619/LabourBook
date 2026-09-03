package com.example.labourbook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.network.WorkerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class WorkerLogin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_worker_login)

        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etWorkerEmail)
        val etPassword = findViewById<EditText>(R.id.etWorkerPassword)
        val btnLogin = findViewById<Button>(R.id.btnWorkerLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvWorkerSignUp)

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, WorkerSignUp::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        fetchWorkerDetailsAndNavigate(email)
                    } else {
                        btnLogin.isEnabled = true
                        Toast.makeText(
                            this,
                            "Authentication failed: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun fetchWorkerDetailsAndNavigate(email: String) {
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit().putString("user_role", "worker").apply()

        val salesforcePrefs = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        val accessToken = salesforcePrefs.getString("access_token", null)
        val instanceUrl = salesforcePrefs.getString("instance_url", null)

        if (!accessToken.isNullOrEmpty() && !instanceUrl.isNullOrEmpty()) {
            lifecycleScope.launch {
                try {
                    val worker = WorkerRepository.getWorkerByEmail(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        email = email
                    )
                    if (worker != null) {
                        appPrefs.edit()
                            .putString("worker_id", worker.Id)
                            .putString("worker_name", worker.Name)
                            .putString("worker_email", worker.Email__c ?: email)
                            .putString("worker_phone", worker.Phone_Number__c)
                            .putString("worker_address", worker.Address__c)
                            .putBoolean("worker_available", worker.Available__c ?: true)
                            .apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                startLocationVerification()
            }
        } else {
            startLocationVerification()
        }
    }

    private fun startLocationVerification() {
        val intent = Intent(this, WorkerSelectLocationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
