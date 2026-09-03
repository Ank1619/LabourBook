package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labourbook.Adapter.WorkerAdapter
import com.example.labourbook.R
import com.example.labourbook.model.WorkerModel
import com.example.labourbook.network.WorkerRepository
import kotlinx.coroutines.launch

class WorkerList : Fragment() {

    private lateinit var recyclerWorkers: RecyclerView

    // Category received from HomeFragment
    private var selectedCategoryId: String? = null
    private var selectedCategoryName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_list,
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

        // Get selected category from HomeFragment
        selectedCategoryId =
            arguments?.getString(
                "selectedCategoryId"
            )

        selectedCategoryName =
            arguments?.getString(
                "selectedCategoryName"
            )

        // Back button
        view.findViewById<View>(
            R.id.btnBack
        )?.setOnClickListener {

            findNavController().navigateUp()
        }

        // RecyclerView
        recyclerWorkers =
            view.findViewById(
                R.id.recyclerWorkers
            )

        recyclerWorkers.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        // Load workers from Salesforce
        loadWorkers()
    }

    private fun loadWorkers() {

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

        // Check Salesforce connection
        if (
            accessToken == null ||
            instanceUrl == null
        ) {

            Toast.makeText(
                requireContext(),
                "Please connect Salesforce first",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Fetch workers from Salesforce
        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    WorkerRepository.getWorkers(
                        instanceUrl,
                        accessToken
                    )

                if (response != null) {

                    // Convert Salesforce Worker records
                    // to WorkerModel
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

                                specialty =
                                    worker.Skills__c
                                        ?: "Worker",

                                rating = "4.9",

                                rate = rateDisplay,

                                avatarEmoji = "👷",

                                address =
                                    worker.Address__c,

                                phoneNumber =
                                    worker.Phone_Number__c,

                                email =
                                    worker.Email__c,

                                experience =
                                    worker.Experience__c,

                                available =
                                    worker.Available__c,

                                skills =
                                    worker.Skills__c,

                                profileUrl =
                                    worker.Profile_URL__c,

                                // Salesforce Lookup ID
                                labourCategory =
                                    worker.Labour_Category__c,

                                hourlyRate = hRate,

                                dailyRate = dRate
                            )
                        }


                    // =====================================
                    // FILTER WORKERS BY SELECTED CATEGORY
                    // =====================================

                    val filteredWorkerList =
                        if (
                            selectedCategoryId.isNullOrBlank()
                        ) {

                            // No category selected
                            // Show all workers
                            workerList

                        } else {

                            // Compare Salesforce Lookup ID or Category Name
                            workerList.filter { worker ->

                                worker.labourCategory
                                    ?.trim()
                                    ?.equals(
                                        selectedCategoryId?.trim(),
                                        ignoreCase = true
                                    ) == true ||
                                (!selectedCategoryName.isNullOrBlank() &&
                                 worker.specialty
                                     .contains(
                                         selectedCategoryName!!,
                                         ignoreCase = true
                                     ))
                            }
                        }


                    // =====================================
                    // SHOW MESSAGE IF NO WORKERS FOUND
                    // =====================================

                    if (filteredWorkerList.isEmpty()) {

                        val message =
                            if (
                                selectedCategoryId.isNullOrBlank()
                            ) {

                                "No workers found"

                            } else {

                                "No workers found in $selectedCategoryName"
                            }

                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }


                    // =====================================
                    // SET WORKER ADAPTER
                    // =====================================

                    val adapter =
                        WorkerAdapter(
                            filteredWorkerList
                        ) { worker ->

                            val bundle =
                                Bundle().apply {

                                    putString(
                                        "workerId",
                                        worker.id
                                    )

                                    putString(
                                        "workerName",
                                        worker.name
                                    )

                                    putString(
                                        "workerSpecialty",
                                        worker.specialty
                                    )

                                    putString(
                                        "workerRating",
                                        worker.rating
                                    )

                                    putString(
                                        "workerRate",
                                        worker.rate
                                    )

                                    putString(
                                        "workerAvatarEmoji",
                                        worker.avatarEmoji
                                    )

                                    putString(
                                        "workerAddress",
                                        worker.address
                                    )

                                    putString(
                                        "workerPhone",
                                        worker.phoneNumber
                                    )

                                    putString(
                                        "workerEmail",
                                        worker.email
                                    )

                                    putInt(
                                        "workerExperience",
                                        worker.experience ?: 0
                                    )

                                    putBoolean(
                                        "workerAvailable",
                                        worker.available ?: false
                                    )

                                    putString(
                                        "workerSkills",
                                        worker.skills
                                    )

                                    putString(
                                        "workerProfileUrl",
                                        worker.profileUrl
                                    )

                                    putString(
                                        "workerCategory",
                                        worker.labourCategory
                                    )

                                    putDouble(
                                        "workerHourlyRate",
                                        worker.hourlyRate ?: 0.0
                                    )

                                    putDouble(
                                        "workerDailyRate",
                                        worker.dailyRate ?: 0.0
                                    )
                                }

                            findNavController().navigate(
                                R.id.action_workerList_to_workerDetails,
                                bundle
                            )
                        }

                    recyclerWorkers.adapter =
                        adapter

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch workers from Salesforce",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}