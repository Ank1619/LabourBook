package com.example.labourbook.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.R
import com.example.labourbook.WorkerMainActivity
import com.example.labourbook.network.WorkerProfileRepository
import com.example.labourbook.network.WorkerRepository
import kotlinx.coroutines.launch

class WorkerSettingsFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvWorkerId: TextView
    private lateinit var switchAvailability: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_settings,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tvSettingsWorkerName)
        tvCategory = view.findViewById(R.id.tvSettingsWorkerCategory)
        tvPhone = view.findViewById(R.id.tvSettingsPhone)
        tvEmail = view.findViewById(R.id.tvSettingsEmail)
        tvAddress = view.findViewById(R.id.tvSettingsAddress)
        tvWorkerId = view.findViewById(R.id.tvSettingsWorkerId)
        switchAvailability = view.findViewById(R.id.switchSettingsAvailability)

        // Edit Profile Click
        view.findViewById<View>(R.id.btnSettingsEditProfile)?.setOnClickListener {
            (activity as? WorkerMainActivity)?.showEditWorkerProfileDialog()
        }

        // Sync Data Click
        view.findViewById<View>(R.id.btnSettingsSyncSalesforce)?.setOnClickListener {
            Toast.makeText(requireContext(), "Syncing data with Salesforce...", Toast.LENGTH_SHORT).show()
            (activity as? WorkerMainActivity)?.refreshAllDashboardData()
            loadWorkerSettingsProfile()
        }

        // Support Click
        view.findViewById<View>(R.id.btnSettingsSupport)?.setOnClickListener {
            Toast.makeText(requireContext(), "Support Email: support@labourbook.com", Toast.LENGTH_LONG).show()
        }

        // Logout Click
        view.findViewById<View>(R.id.btnSettingsLogout)?.setOnClickListener {
            (activity as? WorkerMainActivity)?.findViewById<View>(R.id.btnWorkerLogout)?.performClick()
        }

        loadWorkerSettingsProfile()
    }

    override fun onResume() {
        super.onResume()
        loadWorkerSettingsProfile()
    }

    private fun loadWorkerSettingsProfile() {
        val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val salesforcePrefs = requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        val workerId = appPrefs.getString("worker_id", null)
        val accessToken = salesforcePrefs.getString("access_token", null)
        val instanceUrl = salesforcePrefs.getString("instance_url", null)

        tvWorkerId.text = "Worker ID: ${workerId ?: "Not Available"}"

        if (workerId.isNullOrEmpty() || accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val worker = WorkerProfileRepository.getWorkerProfile(instanceUrl, accessToken, workerId)
                if (worker != null) {
                    tvName.text = worker.Name
                    tvCategory.text = worker.Labour_Category__r?.Name ?: worker.Labour_Category__c ?: "Worker"
                    tvPhone.text = "📞 Phone: ${worker.Phone_Number__c ?: "Not Provided"}"
                    tvEmail.text = "✉ Email: ${worker.Email__c ?: "Not Provided"}"
                    tvAddress.text = "📍 Address: ${worker.Address__c ?: "Not Provided"}"

                    val isAvailable = worker.Available__c ?: true
                    switchAvailability.isChecked = isAvailable

                    switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                        lifecycleScope.launch {
                            try {
                                WorkerRepository.updateWorkerAvailability(instanceUrl, accessToken, workerId, isChecked)
                                appPrefs.edit().putBoolean("worker_available", isChecked).apply()
                                (activity as? WorkerMainActivity)?.refreshAllDashboardData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
