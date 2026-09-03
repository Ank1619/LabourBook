package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.Adapter.LabourCategoryAdapter
import com.example.labourbook.Adapter.WorkerAdapter
import com.example.labourbook.R
import com.example.labourbook.model.WorkerModel
import com.example.labourbook.network.LabourCategoryRepository
import com.example.labourbook.network.WorkerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.labourbook.network.ServiceAreaRepository

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    // Dynamic category adapter
    private lateinit var categoryAdapter: LabourCategoryAdapter

    // Popular workers recyclerview
    private lateinit var recyclerWorkers: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        // Firebase Authentication
        auth = Firebase.auth

        val currentUser = auth.currentUser

        // Greeting
        val tvGreeting =
            view.findViewById<TextView>(
                R.id.tvGreeting
            )

        if (
            currentUser != null &&
            !currentUser.displayName.isNullOrEmpty()
        ) {

            val name =
                currentUser.displayName
                    ?.split(" ")
                    ?.get(0)
                    ?: currentUser.displayName

            tvGreeting.text =
                "Hello, $name 👋"

        } else {

            tvGreeting.text =
                "Hello, Welcome! 👋"
        }

        // ==========================================
        // LOCATION SELECTOR
        // ==========================================

        val tvLocation = view.findViewById<TextView>(R.id.tvLocation)
        val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salesforcePrefs = requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        var savedLocation = appPrefs.getString("selected_location", null)
        if (savedLocation.isNullOrBlank()) {
            savedLocation = salesforcePrefs.getString("customer_address", null)
        }

        val defaultAddress = "Mumbai, Maharashtra"
        val finalAddress = if (!savedLocation.isNullOrBlank()) savedLocation else defaultAddress

        tvLocation?.text = finalAddress

        if (savedLocation.isNullOrBlank()) {
            appPrefs.edit().putString("selected_location", defaultAddress).apply()
            salesforcePrefs.edit().putString("customer_address", defaultAddress).apply()
        }

        tvLocation?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_selectLocation)
        }

        // ==========================================
// TEST SERVICE AREA FETCH
// ==========================================

        loadServiceAreas()


        // ==========================================
        // DYNAMIC LABOUR CATEGORIES RECYCLERVIEW
        // ==========================================

        val recyclerCategories =
            view.findViewById<RecyclerView>(
                R.id.recyclerCategories
            )

        recyclerCategories.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // Category Adapter
        categoryAdapter =
            LabourCategoryAdapter(
                emptyList()
            ) { category ->

                // Send selected category to WorkerList
                val bundle =
                    Bundle().apply {

                        putString(
                            "selectedCategoryId",
                            category.Id
                        )

                        putString(
                            "selectedCategoryName",
                            category.Name
                        )
                    }

                findNavController().navigate(
                    R.id.action_homeFragment_to_workerList,
                    bundle
                )
            }

        recyclerCategories.adapter =
            categoryAdapter

        // Load categories from Salesforce
        loadLabourCategories()


        // ==========================================
        // POPULAR WORKERS RECYCLERVIEW
        // ==========================================

        recyclerWorkers =
            view.findViewById(
                R.id.recyclerPopularWorkers
            )

        recyclerWorkers.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        recyclerWorkers.adapter =
            WorkerAdapter(emptyList()) { worker ->
                navigateToWorkerDetails(worker)
            }

        // Load 5 random popular workers from Salesforce
        loadPopularWorkers()


        // ==========================================
        // VIEW ALL CATEGORIES
        // ==========================================

        view.findViewById<View>(
            R.id.tvViewAllCategories
        )?.setOnClickListener {

            findNavController().navigate(
                R.id.action_homeFragment_to_categoryList
            )
        }


        // ==========================================
        // VIEW ALL WORKERS
        // ==========================================

        view.findViewById<View>(
            R.id.tvViewAllWorkers
        )?.setOnClickListener {

            findNavController().navigate(
                R.id.action_homeFragment_to_workerList
            )
        }


        // ==========================================
        // BOOK NOW BUTTON
        // ==========================================

        view.findViewById<View>(
            R.id.btnBookNow
        )?.setOnClickListener {

            findNavController().navigate(
                R.id.action_homeFragment_to_workerList
            )
        }


        // ==========================================
        // PROFILE
        // ==========================================

        view.findViewById<View>(
            R.id.tvProfile
        )?.setOnClickListener {

            findNavController().navigate(
                R.id.profileFragment
            )
        }
    }


    // ==========================================
    // LOAD LABOUR CATEGORIES FROM SALESFORCE
    // ==========================================

    private fun loadLabourCategories() {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString(
                "access_token",
                null
            )

        val instanceUrl =
            sharedPreferences.getString(
                "instance_url",
                null
            )

        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    LabourCategoryRepository.getLabourCategories(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken
                    )

                if (response != null) {

                    categoryAdapter.updateList(
                        response.records
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // LOAD 5 RANDOM POPULAR WORKERS FROM SALESFORCE
    // ==========================================

    private fun loadPopularWorkers() {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString("access_token", null)

        val instanceUrl =
            sharedPreferences.getString("instance_url", null)

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    WorkerRepository.getWorkers(
                        instanceUrl,
                        accessToken
                    )

                if (response != null && response.records.isNotEmpty()) {

                    val workerList =
                        response.records.map { worker ->

                            val hRate = worker.Hourly_Rate__c
                            val dRate = worker.Daily_Rate__c
                            val rateDisplay = when {
                                hRate != null && dRate != null -> "₹${hRate.toInt()}/hr • ₹${dRate.toInt()}/day"
                                hRate != null -> "₹${hRate.toInt()}/hr"
                                dRate != null -> "₹${dRate.toInt()}/day"
                                else -> "₹350/hr"
                            }

                            WorkerModel(
                                id = worker.Id,
                                name = worker.Name,
                                specialty = worker.Skills__c ?: "Worker",
                                rating = "4.9",
                                rate = rateDisplay,
                                avatarEmoji = "👷",
                                address = worker.Address__c ?: "Location N/A",
                                phoneNumber = worker.Phone_Number__c,
                                email = worker.Email__c,
                                experience = worker.Experience__c,
                                available = worker.Available__c ?: true,
                                skills = worker.Skills__c,
                                profileUrl = worker.Profile_URL__c,
                                labourCategory = worker.Labour_Category__c,
                                hourlyRate = hRate,
                                dailyRate = dRate
                            )
                        }

                    // Pick random 5 workers
                    val random5Workers =
                        workerList.shuffled().take(5)

                    recyclerWorkers.adapter =
                        WorkerAdapter(random5Workers) { worker ->
                            navigateToWorkerDetails(worker)
                        }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigateToWorkerDetails(worker: WorkerModel) {

        val bundle = Bundle().apply {
            putString("workerId", worker.id)
            putString("workerName", worker.name)
            putString("workerSpecialty", worker.specialty)
            putString("workerRating", worker.rating)
            putString("workerRate", worker.rate)
            putString("workerAvatarEmoji", worker.avatarEmoji)
            putString("workerAddress", worker.address)
            putString("workerPhone", worker.phoneNumber)
            putString("workerEmail", worker.email)
            putInt("workerExperience", worker.experience ?: 0)
            putBoolean("workerAvailable", worker.available ?: true)
            putString("workerSkills", worker.skills)
            putString("workerProfileUrl", worker.profileUrl)
            putString("workerCategory", worker.labourCategory)
            putDouble("workerHourlyRate", worker.hourlyRate ?: 0.0)
            putDouble("workerDailyRate", worker.dailyRate ?: 0.0)
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_workerDetails,
            bundle
        )
    }

    // ==========================================
// LOAD SERVICE AREAS FROM SALESFORCE
// ==========================================

    private fun loadServiceAreas() {

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        val accessToken =
            sharedPreferences.getString(
                "access_token",
                null
            )

        val instanceUrl =
            sharedPreferences.getString(
                "instance_url",
                null
            )

        if (
            accessToken.isNullOrEmpty() ||
            instanceUrl.isNullOrEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "Salesforce connection not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    ServiceAreaRepository.getServiceAreas(
                        instanceUrl = instanceUrl,
                        accessToken = accessToken
                    )

                if (response != null) {

                    Toast.makeText(
                        requireContext(),
                        "${response.records.size} service areas found",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch service areas",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                e.printStackTrace()
            }
        }
    }
}