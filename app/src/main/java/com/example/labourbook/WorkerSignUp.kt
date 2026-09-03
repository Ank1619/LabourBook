package com.example.labourbook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.model.LabourCategoryModel
import com.example.labourbook.model.WorkerRequest
import com.example.labourbook.network.LabourCategoryRepository
import com.example.labourbook.network.WorkerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class WorkerSignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private var categoryList: List<LabourCategoryModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_worker_sign_up)

        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<EditText>(R.id.etWorkerName)
        val etEmail = findViewById<EditText>(R.id.etWorkerEmail)
        val etPhone = findViewById<EditText>(R.id.etWorkerPhone)
        val etAddress = findViewById<EditText>(R.id.etWorkerAddress)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerLabourCategory)
        val etSkills = findViewById<EditText>(R.id.etWorkerSkills)
        val etExperience = findViewById<EditText>(R.id.etWorkerExperience)
        val etHourlyRate = findViewById<EditText>(R.id.etWorkerHourlyRate)
        val etDailyRate = findViewById<EditText>(R.id.etWorkerDailyRate)
        val etProfileUrl = findViewById<EditText>(R.id.etWorkerProfileUrl)
        val etPassword = findViewById<EditText>(R.id.etWorkerPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etWorkerConfirmPassword)

        val btnSignUp = findViewById<Button>(R.id.btnWorkerSignUp)
        val tvLogIn = findViewById<TextView>(R.id.tvWorkerLogIn)

        tvLogIn.setOnClickListener {
            finish()
        }

        // Load Categories from Salesforce
        loadLabourCategories(spinnerCategory)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val skills = etSkills.text.toString().trim()
            val experienceStr = etExperience.text.toString().trim()
            val hourlyRateStr = etHourlyRate.text.toString().trim()
            val dailyRateStr = etDailyRate.text.toString().trim()
            val profileUrl = etProfileUrl.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid email is required"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Phone number is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val selectedCategoryPos = spinnerCategory.selectedItemPosition
            val selectedCategoryId = if (selectedCategoryPos in categoryList.indices) {
                categoryList[selectedCategoryPos].Id
            } else null

            btnSignUp.isEnabled = false

            // Register in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()
                        )

                        // Save Worker in Salesforce
                        saveWorkerToSalesforce(
                            name = name,
                            email = email,
                            phone = phone,
                            address = address,
                            categoryId = selectedCategoryId,
                            skills = skills,
                            experience = experienceStr.toIntOrNull(),
                            hourlyRate = hourlyRateStr.toDoubleOrNull(),
                            dailyRate = dailyRateStr.toDoubleOrNull(),
                            profileUrl = if (profileUrl.isNotBlank()) profileUrl else null,
                            button = btnSignUp
                        )
                    } else {
                        btnSignUp.isEnabled = true
                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun loadLabourCategories(spinner: Spinner) {
        val sharedPreferences = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            val defaultAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("General Labour", "Plumber", "Electrician", "Painter", "Carpenter")
            )
            spinner.adapter = defaultAdapter
            return
        }

        lifecycleScope.launch {
            try {
                val response = LabourCategoryRepository.getLabourCategories(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken
                )
                if (response != null && response.records.isNotEmpty()) {
                    categoryList = response.records
                    val namesList = categoryList.map { it.Name }
                    val adapter = ArrayAdapter(
                        this@WorkerSignUp,
                        android.R.layout.simple_spinner_dropdown_item,
                        namesList
                    )
                    spinner.adapter = adapter
                } else {
                    val defaultAdapter = ArrayAdapter(
                        this@WorkerSignUp,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("General Labour", "Plumber", "Electrician", "Painter", "Carpenter")
                    )
                    spinner.adapter = defaultAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveWorkerToSalesforce(
        name: String,
        email: String,
        phone: String,
        address: String,
        categoryId: String?,
        skills: String?,
        experience: Int?,
        hourlyRate: Double?,
        dailyRate: Double?,
        profileUrl: String?,
        button: Button
    ) {
        val sharedPreferences = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)

        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit()
            .putString("user_role", "worker")
            .putString("worker_name", name)
            .putString("worker_email", email)
            .putString("worker_phone", phone)
            .putString("worker_address", address)
            .putBoolean("worker_available", true)
            .apply()

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Registered locally. Salesforce not connected.", Toast.LENGTH_LONG).show()
            startLocationVerification()
            return
        }

        val workerReq = WorkerRequest(
            Name = name,
            Email__c = email,
            Phone_Number__c = phone,
            Address__c = address,
            Available__c = true,
            Hourly_Rate__c = hourlyRate,
            Daily_Rate__c = dailyRate,
            Experience__c = experience,
            Labour_Category__c = categoryId,
            Skills__c = skills,
            Profile_URL__c = profileUrl
        )

        lifecycleScope.launch {
            try {
                val response = WorkerRepository.createWorker(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken,
                    worker = workerReq
                )

                if (response != null && response.success) {
                    if (!response.id.isNullOrEmpty()) {
                        appPrefs.edit().putString("worker_id", response.id).apply()
                    }
                    Toast.makeText(this@WorkerSignUp, "Worker registered successfully in Salesforce!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@WorkerSignUp, "Worker created in app. Salesforce sync failed.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

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
