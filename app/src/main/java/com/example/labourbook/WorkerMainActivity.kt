package com.example.labourbook

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.Fragment.WorkerBookingDetailsFragment
import com.example.labourbook.Fragment.WorkerEarningsFragment
import com.example.labourbook.Fragment.WorkerJobsFragment
import com.example.labourbook.Fragment.WorkerSettingsFragment
import com.example.labourbook.model.BookingModel
import com.example.labourbook.model.UpdateWorkerRequest
import com.example.labourbook.network.BookingListRepository
import com.example.labourbook.network.PendingBookingRepository
import com.example.labourbook.network.WorkerProfileRepository
import com.example.labourbook.network.WorkerRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkerMainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentPendingBooking: PendingBookingModel? = null
    private var currentTodayScheduleBooking: BookingModel? = null

    // Realtime Worker Statistics
    private var totalJobsCount = 0
    private var completedJobsCount = 0
    private var inProgressJobsCount = 0
    private var totalEarningsAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_worker_main
        )

        auth = Firebase.auth

        // Handle back button / gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
                if (scrollViewDashboard != null && scrollViewDashboard.visibility != View.VISIBLE) {
                    showDashboardView()
                } else {
                    finish()
                }
            }
        })


        // ==========================================
        // DASHBOARD VIEWS
        // ==========================================

        val tvName =
            findViewById<TextView>(
                R.id.tvWorkerProfileName
            )

        val tvCategory =
            findViewById<TextView>(
                R.id.tvWorkerCategory
            )

        val tvStatus =
            findViewById<TextView>(
                R.id.tvAvailabilityStatus
            )

        val switchAvailability =
            findViewById<SwitchCompat>(
                R.id.switchAvailability
            )

        val btnLogout =
            findViewById<View>(
                R.id.btnWorkerLogout
            )

        val btnEditWorkerProfile =
            findViewById<ImageView>(R.id.btnEditWorkerProfile)

        // Statistics Views
        val tvTotalJobs =
            findViewById<TextView>(R.id.tvTotalJobs)

        val tvCompletedJobs =
            findViewById<TextView>(R.id.tvCompletedJobs)

        val tvInProgressJobs =
            findViewById<TextView>(R.id.tvInProgressJobs)

        val tvTotalEarnings =
            findViewById<TextView>(R.id.tvTotalEarnings)


        // ==========================================
        // PENDING BOOKING VIEWS
        // ==========================================

        val cardPending =
            findViewById<View>(
                R.id.cardPendingRequest
            )

        val cardNoPending =
            findViewById<View>(
                R.id.cardNoPendingRequest
            )

        val tvPendingJobTitle =
            findViewById<TextView>(
                R.id.tvPendingJobTitle
            )

        val tvPendingJobTime =
            findViewById<TextView>(
                R.id.tvPendingJobTime
            )

        val tvPendingJobLocation =
            findViewById<TextView>(
                R.id.tvPendingJobLocation
            )

        val tvPendingJobPrice =
            findViewById<TextView>(
                R.id.tvPendingJobPrice
            )

        val btnAccept =
            findViewById<MaterialButton>(
                R.id.btnAcceptJob
            )

        val btnReject =
            findViewById<MaterialButton>(
                R.id.btnRejectJob
            )

        val btnViewAllPending =
            findViewById<View>(R.id.btnViewAllPending)


        // ==========================================
        // TODAY'S SCHEDULE VIEWS
        // ==========================================

        val cardTodaySchedule =
            findViewById<View>(R.id.cardTodaySchedule)

        val cardNoSchedule =
            findViewById<View>(R.id.cardNoSchedule)

        val tvScheduleJobTitle =
            findViewById<TextView>(R.id.tvScheduleJobTitle)

        val tvScheduleJobTime =
            findViewById<TextView>(R.id.tvScheduleJobTime)

        val tvScheduleJobLocation =
            findViewById<TextView>(R.id.tvScheduleJobLocation)

        val tvScheduleJobStatus =
            findViewById<TextView>(R.id.tvScheduleJobStatus)

        val btnViewAllSchedule =
            findViewById<View>(R.id.btnViewAllSchedule)


        // ==========================================
        // CARD CLICK LISTENERS -> OPEN BOOKING DETAILS
        // ==========================================

        cardPending?.setOnClickListener {
            val booking = currentPendingBooking
            if (booking != null) {
                val bundle = Bundle().apply {
                    putString("bookingId", booking.Id)
                    putString("bookingName", booking.Name)
                    putString("customerName", "Customer")
                    putString("workDescription", booking.Work_Description__c ?: booking.Booking_Type__c ?: "New Job Request")
                    putString("workAddress", booking.Work_Address__c ?: "Location not specified")
                    putString("bookingDate", booking.Booking_Date__c)
                    putDouble("workingHours", booking.Working_Hour__c?.toDouble() ?: 0.0)
                    putString("bookingType", booking.Booking_Type__c ?: "")
                    putDouble("amount", booking.Amount__c ?: 0.0)
                    putString("status", booking.Booking_Labour_Status__c ?: "Pending")
                }
                showWorkerBookingDetailsFragment(bundle)
            }
        }

        cardTodaySchedule?.setOnClickListener {
            val booking = currentTodayScheduleBooking
            if (booking != null) {
                val bundle = Bundle().apply {
                    putString("bookingId", booking.Id)
                    putString("bookingName", booking.Name)
                    putString("customerName", booking.L_Customer__r?.Name ?: "Customer")
                    putString("workDescription", booking.Work_Description__c ?: booking.Booking_Type__c ?: "Scheduled Work")
                    putString("workAddress", booking.Work_Address__c ?: "Location not specified")
                    putString("bookingDate", booking.Booking_Date__c)
                    putDouble("workingHours", booking.Working_Hour__c ?: 0.0)
                    putString("bookingType", booking.Booking_Type__c ?: "")
                    putDouble("amount", booking.Amount__c ?: 0.0)
                    putString("status", booking.Booking_Labour_Status__c ?: "Accepted")
                }
                showWorkerBookingDetailsFragment(bundle)
            }
        }


        // ==========================================
        // APP PREFERENCES
        // ==========================================

        val appPrefs =
            getSharedPreferences(
                "app_prefs",
                Context.MODE_PRIVATE
            )

        val workerId =
            appPrefs.getString(
                "worker_id",
                null
            )

        val workerName =
            appPrefs.getString(
                "worker_name",
                auth.currentUser?.displayName
                    ?: "Worker"
            )

        val isAvailable =
            appPrefs.getBoolean(
                "worker_available",
                true
            )


        // ==========================================
        // INITIAL DASHBOARD DATA
        // ==========================================

        tvName.text =
            "Hello, $workerName"

        tvCategory.text =
            "Loading..."

        switchAvailability.isChecked =
            isAvailable

        updateStatusText(
            tvStatus,
            isAvailable
        )


        // ==========================================
        // EDIT PROFILE CLICK
        // ==========================================

        btnEditWorkerProfile?.setOnClickListener {
            showEditWorkerProfileDialog()
        }


        // ==========================================
        // VIEW ALL BUTTON CLICKS
        // ==========================================

        btnViewAllPending?.setOnClickListener {
            showWorkerJobsFragment(1) // Upcoming jobs
        }

        btnViewAllSchedule?.setOnClickListener {
            showWorkerJobsFragment(1) // Upcoming jobs
        }


        // ==========================================
        // INITIAL DASHBOARD REFRESH
        // ==========================================

        refreshAllDashboardData()


        // ==========================================
        // AVAILABILITY SWITCH
        // ==========================================

        switchAvailability.setOnCheckedChangeListener {

                _,
                isChecked ->

            updateStatusText(
                tvStatus,
                isChecked
            )

            appPrefs.edit()
                .putBoolean(
                    "worker_available",
                    isChecked
                )
                .apply()


            if (!workerId.isNullOrEmpty()) {

                val salesforcePrefs =
                    getSharedPreferences(
                        "salesforce_auth",
                        Context.MODE_PRIVATE
                    )

                val accessToken =
                    salesforcePrefs.getString(
                        "access_token",
                        null
                    )

                val instanceUrl =
                    salesforcePrefs.getString(
                        "instance_url",
                        null
                    )

                if (
                    !accessToken.isNullOrEmpty() &&
                    !instanceUrl.isNullOrEmpty()
                ) {

                    lifecycleScope.launch {

                        try {

                            WorkerRepository.updateWorkerAvailability(
                                instanceUrl = instanceUrl,
                                accessToken = accessToken,
                                workerId = workerId,
                                available = isChecked
                            )

                        } catch (e: Exception) {

                            e.printStackTrace()

                            Log.e(
                                "WORKER_PROFILE",
                                "Availability update error: ${e.message}"
                            )
                        }
                    }
                }
            }
        }


        // ==========================================
        // ACCEPT PENDING REQUEST
        // ==========================================

        btnAccept.setOnClickListener {

            val booking = currentPendingBooking
            if (booking == null) {
                Toast.makeText(this, "No pending booking available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val salesforcePrefs =
                getSharedPreferences(
                    "salesforce_auth",
                    Context.MODE_PRIVATE
                )

            val accessToken =
                salesforcePrefs.getString(
                    "access_token",
                    null
                )

            val instanceUrl =
                salesforcePrefs.getString(
                    "instance_url",
                    null
                )

            if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Salesforce connection missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnAccept.isEnabled = false
            btnReject.isEnabled = false

            lifecycleScope.launch {
                try {
                    val success =
                        PendingBookingRepository.updateBookingStatus(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            bookingId = booking.Id,
                            status = "Accepted"
                        )

                    if (success) {
                        Toast.makeText(
                            this@WorkerMainActivity,
                            "Request Accepted",
                            Toast.LENGTH_SHORT
                        ).show()

                        refreshAllDashboardData()

                    } else {
                        Toast.makeText(
                            this@WorkerMainActivity,
                            "Failed to update booking status in Salesforce",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@WorkerMainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = true
                }
            }
        }


        // ==========================================
        // REJECT PENDING REQUEST
        // ==========================================

        btnReject.setOnClickListener {

            val booking = currentPendingBooking
            if (booking == null) {
                Toast.makeText(this, "No pending booking available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val salesforcePrefs =
                getSharedPreferences(
                    "salesforce_auth",
                    Context.MODE_PRIVATE
                )

            val accessToken =
                salesforcePrefs.getString(
                    "access_token",
                    null
                )

            val instanceUrl =
                salesforcePrefs.getString(
                    "instance_url",
                    null
                )

            if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Salesforce connection missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnAccept.isEnabled = false
            btnReject.isEnabled = false

            lifecycleScope.launch {
                try {
                    val success =
                        PendingBookingRepository.updateBookingStatus(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            bookingId = booking.Id,
                            status = "Rejected"
                        )

                    if (success) {
                        Toast.makeText(
                            this@WorkerMainActivity,
                            "Request Rejected",
                            Toast.LENGTH_SHORT
                        ).show()

                        refreshAllDashboardData()

                    } else {
                        Toast.makeText(
                            this@WorkerMainActivity,
                            "Failed to update booking status in Salesforce",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@WorkerMainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = true
                }
            }
        }


        // ==========================================
        // BOTTOM NAVIGATION
        // ==========================================

        findViewById<View>(
            R.id.navDashboard
        )?.setOnClickListener {
            showDashboardView()
        }


        findViewById<View>(
            R.id.navMyJobs
        )?.setOnClickListener {
            showWorkerJobsFragment(0) // All jobs tab
        }


        findViewById<View>(
            R.id.navEarnings
        )?.setOnClickListener {
            showWorkerEarningsFragment() // Separate Earnings Fragment
        }


        findViewById<View>(
            R.id.navProfile
        )?.setOnClickListener {
            showEditWorkerProfileDialog()
        }


        findViewById<View>(
            R.id.navMore
        )?.setOnClickListener {
            showWorkerSettingsFragment() // More Settings Fragment
        }


        // ==========================================
        // LOGOUT
        // ==========================================

        btnLogout.setOnClickListener {

            FirebaseAuth.getInstance()
                .signOut()

            auth.signOut()


            getSharedPreferences(
                "app_prefs",
                Context.MODE_PRIVATE
            ).edit()
                .clear()
                .apply()


            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            ).edit()
                .clear()
                .apply()


            Toast.makeText(
                this,
                "Logged out successfully",
                Toast.LENGTH_SHORT
            ).show()


            val intent =
                Intent(
                    this,
                    RoleSelectionActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(
                intent
            )

            finish()
        }
    }


    // ==========================================
    // AUTOMATIC REFRESH ON RESUME
    // ==========================================

    override fun onResume() {
        super.onResume()
        refreshAllDashboardData()
    }


    // ==========================================
    // REFRESH ALL DASHBOARD DATA
    // ==========================================

    fun refreshAllDashboardData() {

        val appPrefs =
            getSharedPreferences(
                "app_prefs",
                Context.MODE_PRIVATE
            )

        val workerId =
            appPrefs.getString(
                "worker_id",
                null
            )

        val tvName =
            findViewById<TextView>(R.id.tvWorkerProfileName)

        val tvCategory =
            findViewById<TextView>(R.id.tvWorkerCategory)

        val tvStatus =
            findViewById<TextView>(R.id.tvAvailabilityStatus)

        val switchAvailability =
            findViewById<SwitchCompat>(R.id.switchAvailability)

        val tvTotalJobs =
            findViewById<TextView>(R.id.tvTotalJobs)

        val tvCompletedJobs =
            findViewById<TextView>(R.id.tvCompletedJobs)

        val tvInProgressJobs =
            findViewById<TextView>(R.id.tvInProgressJobs)

        val tvTotalEarnings =
            findViewById<TextView>(R.id.tvTotalEarnings)

        val cardPending =
            findViewById<View>(R.id.cardPendingRequest)

        val cardNoPending =
            findViewById<View>(R.id.cardNoPendingRequest)

        val tvPendingJobTitle =
            findViewById<TextView>(R.id.tvPendingJobTitle)

        val tvPendingJobTime =
            findViewById<TextView>(R.id.tvPendingJobTime)

        val tvPendingJobLocation =
            findViewById<TextView>(R.id.tvPendingJobLocation)

        val tvPendingJobPrice =
            findViewById<TextView>(R.id.tvPendingJobPrice)

        val cardTodaySchedule =
            findViewById<View>(R.id.cardTodaySchedule)

        val cardNoSchedule =
            findViewById<View>(R.id.cardNoSchedule)

        val tvScheduleJobTitle =
            findViewById<TextView>(R.id.tvScheduleJobTitle)

        val tvScheduleJobTime =
            findViewById<TextView>(R.id.tvScheduleJobTime)

        val tvScheduleJobLocation =
            findViewById<TextView>(R.id.tvScheduleJobLocation)

        val tvScheduleJobStatus =
            findViewById<TextView>(R.id.tvScheduleJobStatus)


        if (!workerId.isNullOrEmpty()) {

            loadWorkerProfile(
                workerId = workerId,
                tvName = tvName,
                tvCategory = tvCategory,
                tvStatus = tvStatus,
                switchAvailability = switchAvailability
            )

            loadWorkerDashboardStats(
                workerId = workerId,
                tvTotalJobs = tvTotalJobs,
                tvCompletedJobs = tvCompletedJobs,
                tvInProgressJobs = tvInProgressJobs,
                tvTotalEarnings = tvTotalEarnings
            )

            loadPendingBookings(
                workerId = workerId,
                cardPending = cardPending,
                cardNoPending = cardNoPending,
                tvPendingJobTitle = tvPendingJobTitle,
                tvPendingJobTime = tvPendingJobTime,
                tvPendingJobLocation = tvPendingJobLocation,
                tvPendingJobPrice = tvPendingJobPrice
            )

            loadTodaySchedule(
                workerId = workerId,
                cardTodaySchedule = cardTodaySchedule,
                cardNoSchedule = cardNoSchedule,
                tvScheduleJobTitle = tvScheduleJobTitle,
                tvScheduleJobTime = tvScheduleJobTime,
                tvScheduleJobLocation = tvScheduleJobLocation,
                tvScheduleJobStatus = tvScheduleJobStatus
            )

        } else {

            cardPending.visibility = View.GONE
            cardNoPending?.visibility = View.VISIBLE
            cardTodaySchedule?.visibility = View.GONE
            cardNoSchedule?.visibility = View.VISIBLE
        }
    }


    // ==========================================
    // SWITCH BETWEEN DASHBOARD & WORKER SUB-FRAGMENTS
    // ==========================================

    private fun showDashboardView() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
        val fragmentContainer = findViewById<View>(R.id.workerFragmentContainer)

        tvHeaderTitle?.text = "Dashboard"
        scrollViewDashboard?.visibility = View.VISIBLE
        fragmentContainer?.visibility = View.GONE

        refreshAllDashboardData()
    }

    private fun showWorkerJobsFragment(initialTabPosition: Int = 0) {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
        val fragmentContainer = findViewById<View>(R.id.workerFragmentContainer)

        tvHeaderTitle?.text = "My Jobs"
        scrollViewDashboard?.visibility = View.GONE
        fragmentContainer?.visibility = View.VISIBLE

        val fragment = WorkerJobsFragment().apply {
            arguments = Bundle().apply {
                putInt("initialTab", initialTabPosition)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.workerFragmentContainer, fragment)
            .commit()
    }

    private fun showWorkerEarningsFragment() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
        val fragmentContainer = findViewById<View>(R.id.workerFragmentContainer)

        tvHeaderTitle?.text = "My Earnings"
        scrollViewDashboard?.visibility = View.GONE
        fragmentContainer?.visibility = View.VISIBLE

        val fragment = WorkerEarningsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.workerFragmentContainer, fragment)
            .commit()
    }

    fun showWorkerSettingsFragment() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
        val fragmentContainer = findViewById<View>(R.id.workerFragmentContainer)

        tvHeaderTitle?.text = "More Settings"
        scrollViewDashboard?.visibility = View.GONE
        fragmentContainer?.visibility = View.VISIBLE

        val fragment = WorkerSettingsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.workerFragmentContainer, fragment)
            .commit()
    }

    fun showWorkerBookingDetailsFragment(bundle: Bundle) {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val scrollViewDashboard = findViewById<View>(R.id.scrollViewWorkerDashboard)
        val fragmentContainer = findViewById<View>(R.id.workerFragmentContainer)

        tvHeaderTitle?.text = "Booking Details"
        scrollViewDashboard?.visibility = View.GONE
        fragmentContainer?.visibility = View.VISIBLE

        val fragment = WorkerBookingDetailsFragment().apply {
            arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.workerFragmentContainer, fragment)
            .commit()
    }


    // ==========================================
    // EDIT WORKER PROFILE DIALOG (UPDATES SALESFORCE)
    // ==========================================

    fun showEditWorkerProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_worker_profile, null)

        val etName = dialogView.findViewById<EditText>(R.id.etEditWorkerName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditWorkerPhone)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditWorkerEmail)
        val etAddress = dialogView.findViewById<EditText>(R.id.etEditWorkerAddress)
        val etExperience = dialogView.findViewById<EditText>(R.id.etEditWorkerExperience)
        val etHourlyRate = dialogView.findViewById<EditText>(R.id.etEditWorkerHourlyRate)
        val etDailyRate = dialogView.findViewById<EditText>(R.id.etEditWorkerDailyRate)
        val etSkills = dialogView.findViewById<EditText>(R.id.etEditWorkerSkills)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelWorkerEdit)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWorkerProfile)

        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salesforcePrefs = getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        val workerId = appPrefs.getString("worker_id", null)
        val accessToken = salesforcePrefs.getString("access_token", null)
        val instanceUrl = salesforcePrefs.getString("instance_url", null)

        if (workerId.isNullOrEmpty() || accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Salesforce worker credentials missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch current details from Salesforce
        lifecycleScope.launch {
            try {
                val worker = WorkerProfileRepository.getWorkerProfile(instanceUrl, accessToken, workerId)
                if (worker != null) {
                    etName.setText(worker.Name)
                    etPhone.setText(worker.Phone_Number__c ?: "")
                    etEmail.setText(worker.Email__c ?: "")
                    etAddress.setText(worker.Address__c ?: "")
                    etExperience.setText(worker.Experience__c?.toString() ?: "")
                    etHourlyRate.setText(worker.Hourly_Rate__c?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
                    etDailyRate.setText(worker.Daily_Rate__c?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
                    etSkills.setText(worker.Skills__c ?: "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()
            val newExp = etExperience.text.toString().trim().toIntOrNull()
            val newHourly = etHourlyRate.text.toString().trim().toDoubleOrNull()
            val newDaily = etDailyRate.text.toString().trim().toDoubleOrNull()
            val newSkills = etSkills.text.toString().trim()

            if (newName.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            lifecycleScope.launch {
                try {
                    val updateReq = UpdateWorkerRequest(
                        Name = newName,
                        Phone_Number__c = newPhone,
                        Address__c = newAddress,
                        Email__c = newEmail,
                        Experience__c = newExp,
                        Hourly_Rate__c = newHourly,
                        Daily_Rate__c = newDaily,
                        Skills__c = newSkills
                    )

                    val success = WorkerRepository.updateWorkerProfile(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        workerId = workerId,
                        updateReq = updateReq
                    )

                    if (success) {
                        Toast.makeText(this@WorkerMainActivity, "Worker profile updated in Salesforce!", Toast.LENGTH_SHORT).show()
                        appPrefs.edit().putString("worker_name", newName).apply()
                        refreshAllDashboardData()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@WorkerMainActivity, "Failed to update profile in Salesforce", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@WorkerMainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                }
            }
        }

        dialog.show()
    }


    // ==========================================
    // LOAD REALTIME WORKER DASHBOARD STATS
    // ==========================================

    private fun loadWorkerDashboardStats(
        workerId: String,
        tvTotalJobs: TextView?,
        tvCompletedJobs: TextView?,
        tvInProgressJobs: TextView?,
        tvTotalEarnings: TextView?
    ) {

        val salesforcePrefs =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            salesforcePrefs.getString(
                "access_token",
                null
            )

        val instanceUrl =
            salesforcePrefs.getString(
                "instance_url",
                null
            )

        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {
            return
        }

        lifecycleScope.launch {

            try {

                val response =
                    BookingListRepository.getWorkerBookings(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        workerId = workerId
                    )

                if (response != null) {

                    val bookings = response.records

                    totalJobsCount = bookings.size

                    completedJobsCount = bookings.count {
                        it.Booking_Labour_Status__c?.trim()?.lowercase() == "completed"
                    }

                    inProgressJobsCount = bookings.count {
                        val status = it.Booking_Labour_Status__c?.trim()?.lowercase()
                        status == "accepted" || status == "in progress" || status == "confirmed" || status == "pending"
                    }

                    totalEarningsAmount = bookings.filter {
                        val status = it.Booking_Labour_Status__c?.trim()?.lowercase()
                        status == "completed"
                    }.sumOf { it.Amount__c ?: 0.0 }

                    tvTotalJobs?.text = totalJobsCount.toString()
                    tvCompletedJobs?.text = completedJobsCount.toString()
                    tvInProgressJobs?.text = inProgressJobsCount.toString()
                    tvTotalEarnings?.text = "₹ ${totalEarningsAmount.toInt()}"
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }


    // ==========================================
    // LOAD TODAY'S SCHEDULE FROM SALESFORCE
    // ==========================================

    private fun loadTodaySchedule(
        workerId: String,
        cardTodaySchedule: View?,
        cardNoSchedule: View?,
        tvScheduleJobTitle: TextView?,
        tvScheduleJobTime: TextView?,
        tvScheduleJobLocation: TextView?,
        tvScheduleJobStatus: TextView?
    ) {

        val salesforcePrefs =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            salesforcePrefs.getString(
                "access_token",
                null
            )

        val instanceUrl =
            salesforcePrefs.getString(
                "instance_url",
                null
            )

        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            cardTodaySchedule?.visibility = View.GONE
            cardNoSchedule?.visibility = View.VISIBLE

            return
        }

        lifecycleScope.launch {

            try {

                val response =
                    BookingListRepository.getWorkerBookings(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken,
                        workerId = workerId
                    )

                if (response != null && response.records.isNotEmpty()) {

                    val bookings = response.records

                    // Filter for today's bookings or active scheduled jobs
                    val todayBookings = bookings.filter { booking ->
                        val status = booking.Booking_Labour_Status__c?.trim()?.lowercase()
                        isToday(booking.Booking_Date__c) && status != "cancelled" && status != "rejected"
                    }

                    val scheduleJob = if (todayBookings.isNotEmpty()) {
                        todayBookings.first()
                    } else {
                        // Fallback to active in-progress or accepted booking
                        bookings.firstOrNull { booking ->
                            val status = booking.Booking_Labour_Status__c?.trim()?.lowercase()
                            status == "accepted" || status == "in progress" || status == "confirmed"
                        }
                    }

                    if (scheduleJob != null) {
                        currentTodayScheduleBooking = scheduleJob

                        tvScheduleJobTitle?.text =
                            scheduleJob.Work_Description__c
                                ?: scheduleJob.Booking_Type__c
                                        ?: "Scheduled Work"

                        val bookingType = scheduleJob.Booking_Type__c ?: ""
                        val formattedDate = formatDate(scheduleJob.Booking_Date__c)

                        tvScheduleJobTime?.text =
                            if (bookingType.isNotEmpty() && formattedDate != "Date N/A") {
                                "$bookingType • $formattedDate"
                            } else if (bookingType.isNotEmpty()) {
                                bookingType
                            } else {
                                formattedDate
                            }

                        tvScheduleJobLocation?.text =
                            scheduleJob.Work_Address__c
                                ?: "Location not specified"

                        val status = scheduleJob.Booking_Labour_Status__c?.trim() ?: "Scheduled"
                        tvScheduleJobStatus?.text = status

                        cardTodaySchedule?.visibility = View.VISIBLE
                        cardNoSchedule?.visibility = View.GONE

                    } else {

                        currentTodayScheduleBooking = null
                        cardTodaySchedule?.visibility = View.GONE
                        cardNoSchedule?.visibility = View.VISIBLE
                    }

                } else {

                    currentTodayScheduleBooking = null
                    cardTodaySchedule?.visibility = View.GONE
                    cardNoSchedule?.visibility = View.VISIBLE
                }

            } catch (e: Exception) {

                e.printStackTrace()
                currentTodayScheduleBooking = null

                cardTodaySchedule?.visibility = View.GONE
                cardNoSchedule?.visibility = View.VISIBLE
            }
        }
    }


    // ==========================================
    // LOAD PENDING BOOKINGS
    // ==========================================

    private fun loadPendingBookings(
        workerId: String,
        cardPending: View,
        cardNoPending: View?,
        tvPendingJobTitle: TextView,
        tvPendingJobTime: TextView,
        tvPendingJobLocation: TextView,
        tvPendingJobPrice: TextView
    ) {

        val salesforcePrefs =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            salesforcePrefs.getString(
                "access_token",
                null
            )

        val instanceUrl =
            salesforcePrefs.getString(
                "instance_url",
                null
            )


        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            currentPendingBooking = null
            cardPending.visibility =
                View.GONE
            cardNoPending?.visibility =
                View.VISIBLE

            return
        }


        lifecycleScope.launch {

            try {

                Log.d(
                    "PENDING_BOOKING",
                    "Fetching pending bookings..."
                )


                val bookings =
                    PendingBookingRepository
                        .getPendingBookings(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            workerId = workerId
                        )


                if (bookings.isNotEmpty()) {

                    val booking =
                        bookings.first()
                    currentPendingBooking = booking


                    // Work Description / Booking Type
                    tvPendingJobTitle.text =
                        booking.Work_Description__c
                            ?: booking.Booking_Type__c
                                    ?: "New Job Request"


                    // Working Hours + Booking Date
                    val hours =
                        booking.Working_Hour__c?.let {
                            "$it Hours"
                        } ?: ""

                    val formattedDate =
                        formatDate(booking.Booking_Date__c)

                    tvPendingJobTime.text =
                        if (
                            hours.isNotEmpty() &&
                            formattedDate.isNotEmpty() &&
                            formattedDate != "Date N/A"
                        ) {
                            "$hours • $formattedDate"
                        } else if (
                            hours.isNotEmpty()
                        ) {
                            hours
                        } else if (
                            formattedDate.isNotEmpty() &&
                            formattedDate != "Date N/A"
                        ) {
                            formattedDate
                        } else {
                            "Time not specified"
                        }


                    // Work Address
                    tvPendingJobLocation.text =
                        booking.Work_Address__c
                            ?: "Location not specified"


                    // Amount
                    tvPendingJobPrice.text =
                        booking.Amount__c?.let {
                            "₹ ${it.toInt()}"
                        } ?: "Amount not specified"


                    cardPending.visibility =
                        View.VISIBLE

                    cardNoPending?.visibility =
                        View.GONE


                    Log.d(
                        "PENDING_BOOKING",
                        "Pending booking found: ${booking.Name}"
                    )

                } else {

                    Log.d(
                        "PENDING_BOOKING",
                        "No pending bookings found"
                    )

                    currentPendingBooking = null

                    cardPending.visibility =
                        View.GONE

                    cardNoPending?.visibility =
                        View.VISIBLE
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Log.e(
                    "PENDING_BOOKING",
                    "Error loading pending bookings",
                    e
                )

                currentPendingBooking = null

                cardPending.visibility =
                    View.GONE

                cardNoPending?.visibility =
                    View.VISIBLE
            }
        }
    }


    // ==========================================
    // LOAD WORKER PROFILE FROM SALESFORCE
    // ==========================================

    private fun loadWorkerProfile(
        workerId: String,
        tvName: TextView,
        tvCategory: TextView,
        tvStatus: TextView,
        switchAvailability: SwitchCompat
    ) {

        val salesforcePrefs =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            salesforcePrefs.getString(
                "access_token",
                null
            )

        val instanceUrl =
            salesforcePrefs.getString(
                "instance_url",
                null
            )


        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            tvCategory.text =
                "Salesforce connection missing"

            return
        }


        lifecycleScope.launch {

            try {

                val worker =
                    WorkerProfileRepository
                        .getWorkerProfile(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            workerId = workerId
                        )


                if (worker != null) {

                    tvName.text =
                        "Hello, ${worker.Name}"


                    // Labour Category
                    if (
                        worker.Labour_Category__r != null &&
                        !worker.Labour_Category__r.Name.isNullOrEmpty()
                    ) {

                        tvCategory.text =
                            worker.Labour_Category__r.Name

                    } else if (
                        !worker.Labour_Category__c.isNullOrEmpty()
                    ) {

                        tvCategory.text =
                            "Category assigned but name not fetched"

                    } else {

                        tvCategory.text =
                            "No Category Assigned"
                    }


                    // Availability
                    val available =
                        worker.Available__c
                            ?: false


                    switchAvailability.isChecked =
                        available


                    updateStatusText(
                        tvStatus,
                        available
                    )


                    // Save locally
                    getSharedPreferences(
                        "app_prefs",
                        Context.MODE_PRIVATE
                    ).edit()
                        .putString(
                            "worker_name",
                            worker.Name
                        )
                        .putBoolean(
                            "worker_available",
                            available
                        )
                        .apply()

                } else {

                    tvCategory.text =
                        "Profile not found"
                }


            } catch (e: Exception) {

                e.printStackTrace()

                Log.e(
                    "WORKER_PROFILE",
                    "Error loading profile",
                    e
                )

                tvCategory.text =
                    "Unable to load category"
            }
        }
    }


    // ==========================================
    // UPDATE AVAILABILITY STATUS
    // ==========================================

    private fun updateStatusText(
        tvStatus: TextView,
        isAvailable: Boolean
    ) {

        if (isAvailable) {

            tvStatus.text =
                "Available"

            tvStatus.setTextColor(
                Color.parseColor("#16A34A")
            )

        } else {

            tvStatus.text =
                "Busy"

            tvStatus.setTextColor(
                Color.parseColor("#DC2626")
            )
        }
    }


    // ==========================================
    // CHECK IF DATE IS TODAY
    // ==========================================

    private fun isToday(dateString: String?): Boolean {

        if (dateString.isNullOrBlank()) return false

        val todayCalendar = Calendar.getInstance()
        val todayYear = todayCalendar.get(Calendar.YEAR)
        val todayMonth = todayCalendar.get(Calendar.MONTH)
        val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (format in inputFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    return cal.get(Calendar.YEAR) == todayYear &&
                            cal.get(Calendar.MONTH) == todayMonth &&
                            cal.get(Calendar.DAY_OF_MONTH) == todayDay
                }
            } catch (_: Exception) {
                // Ignore and try next format
            }
        }

        return false
    }


    // ==========================================
    // FORMAT DATE FOR DISPLAY
    // ==========================================

    private fun formatDate(dateString: String?): String {

        if (dateString.isNullOrBlank()) return "Date N/A"

        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        val outputFormat =
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        for (format in inputFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (_: Exception) {
                // Ignore and try next format
            }
        }

        return dateString
    }
}
