package com.example.labourbook

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.labourbook.model.CustomerRequest
import com.example.labourbook.network.CustomerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.scrollView)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        val etFullName =
            findViewById<EditText>(R.id.etFullName)

        val etEmail =
            findViewById<EditText>(R.id.etEmail)

        val etPhone =
            findViewById<EditText>(R.id.etPhone)

        val etPassword =
            findViewById<EditText>(R.id.etPassword)

        val etConfirmPassword =
            findViewById<EditText>(R.id.etConfirmPassword)

        val btnSignUp =
            findViewById<Button>(R.id.btnSignUp)

        val tvLogIn =
            findViewById<TextView>(R.id.tvLogIn)

        tvLogIn.setOnClickListener {
            finish()
        }

        btnSignUp.setOnClickListener {

            val fullName =
                etFullName.text.toString().trim()

            val email =
                etEmail.text.toString().trim()

            val phone =
                etPhone.text.toString().trim()

            val password =
                etPassword.text.toString().trim()

            val confirmPassword =
                etConfirmPassword.text.toString().trim()

            // Validation
            if (fullName.isEmpty()) {
                etFullName.error = "Full Name is required"
                etFullName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Enter a valid email address"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etPhone.error = "Phone number is required"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error =
                    "Password must be at least 6 characters"

                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error =
                    "Passwords do not match"

                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false

            // Create Firebase user
            auth.createUserWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    val profileUpdates =
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                    user?.updateProfile(profileUpdates)

                    if (user != null) {

                        // Create Customer in Salesforce
                        createSalesforceCustomer(
                            user = user,
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            button = btnSignUp
                        )

                    } else {
                        btnSignUp.isEnabled = true

                        Toast.makeText(
                            this,
                            "Firebase user not found",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    btnSignUp.isEnabled = true

                    Log.w(
                        TAG,
                        "createUserWithEmail:failure",
                        task.exception
                    )

                    Toast.makeText(
                        this,
                        "Sign up failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createSalesforceCustomer(
        user: FirebaseUser,
        fullName: String,
        email: String,
        phone: String,
        button: Button
    ) {

        // Get Salesforce authentication details
        val preferences =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            preferences.getString(
                "access_token",
                null
            )

        val instanceUrl =
            preferences.getString(
                "instance_url",
                null
            )

        // Salesforce is not connected
        if (accessToken == null || instanceUrl == null) {

            button.isEnabled = true

            Toast.makeText(
                this,
                "Firebase account created. Please connect Salesforce.",
                Toast.LENGTH_LONG
            ).show()

            updateUI(user)
            return
        }

        // Create Salesforce Customer object
        val customer = CustomerRequest(
            Name = fullName,
            Email__c = email,
            Firebase_UID__c = user.uid,
            Phone_Number__c = phone
        )

        lifecycleScope.launch {

            try {

                val response =
                    CustomerRepository.createCustomer(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        customer = customer
                    )

                if (response != null && response.success) {

                    // Save Salesforce Customer ID
                    preferences.edit()
                        .putString(
                            "customer_id",
                            response.id
                        )
                        .apply()

                    Toast.makeText(
                        this@SignUp,
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateUI(user)

                } else {

                    button.isEnabled = true

                    Toast.makeText(
                        this@SignUp,
                        "Firebase account created, but Salesforce customer creation failed",
                        Toast.LENGTH_LONG
                    ).show()

                    updateUI(user)
                }

            } catch (e: Exception) {

                button.isEnabled = true

                Toast.makeText(
                    this@SignUp,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                updateUI(user)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {

        if (user != null) {

            val intent =
                Intent(this, MainActivity::class.java)

            intent.putExtra("is_from_login", true)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }
}