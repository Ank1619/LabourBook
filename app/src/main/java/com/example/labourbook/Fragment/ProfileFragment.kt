package com.example.labourbook.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.labourbook.Login
import com.example.labourbook.R
import com.example.labourbook.model.UpdateCustomerRequest
import com.example.labourbook.network.CustomerRepository
import com.example.labourbook.network.SalesforceAuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    private var currentName: String = ""
    private var currentEmail: String = ""
    private var currentPhone: String = ""
    private var currentAddress: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        val currentUser = auth.currentUser

        val tvName = view.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        val tvPhone = view.findViewById<TextView>(R.id.tvUserPhone)
        val tvAddress = view.findViewById<TextView>(R.id.tvUserAddress)

        if (currentUser != null) {
            currentName = currentUser.displayName ?: "LabourBook User"
            currentEmail = currentUser.email ?: ""
            tvName?.text = currentName
            tvEmail?.text = currentEmail
        }

        // ===================================
        // LOAD PROFILE FROM SALESFORCE/PREFS
        // ===================================
        loadCustomerProfile(tvName, tvEmail, tvPhone, tvAddress)

        // ===================================
        // EDIT PROFILE (PENCIL ICON CLICK)
        // ===================================
        val btnEditProfile = view.findViewById<ImageView>(R.id.btnEditProfile)
        btnEditProfile?.setOnClickListener {
            showEditProfileDialog(tvName, tvEmail, tvPhone, tvAddress)
        }

        // ===================================
        // MY BOOKINGS CLICK -> BOOKING FRAGMENT
        // ===================================
        val cardMyBookings = view.findViewById<View>(R.id.cardMyBookings)
        cardMyBookings?.setOnClickListener {
            findNavController().navigate(
                R.id.action_profileFragment_to_bookingFragment
            )
        }

        // ===================================
        // NOTIFICATIONS CLICK
        // ===================================
        val cardNotifications = view.findViewById<View>(R.id.cardNotifications)
        cardNotifications?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "You have no new notifications",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ===================================
        // HELP & SUPPORT CLICK
        // ===================================
        val cardHelp = view.findViewById<View>(R.id.cardHelp)
        cardHelp?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "LabourBook Support: support@labourbook.com",
                Toast.LENGTH_LONG
            ).show()
        }

        // ===================================
        // CONNECT SALESFORCE BUTTON
        // ===================================
        val connectButton = view.findViewById<Button>(R.id.btnConnectSalesforce)
        connectButton?.setOnClickListener {
            SalesforceAuthManager.login(requireContext())
        }

        // ===================================
        // LOGOUT BUTTON
        // ===================================
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadCustomerProfile(
        tvName: TextView?,
        tvEmail: TextView?,
        tvPhone: TextView?,
        tvAddress: TextView?
    ) {
        val sharedPreferences =
            requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        val appPrefs =
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val accessToken = sharedPreferences.getString("access_token", null)
        val instanceUrl = sharedPreferences.getString("instance_url", null)
        val customerId = sharedPreferences.getString("customer_id", null)

        // Load saved values from preferences if available
        val savedPhone = sharedPreferences.getString("customer_phone", null)
        var savedAddress = sharedPreferences.getString("customer_address", null)

        if (savedAddress.isNullOrBlank()) {
            savedAddress = appPrefs.getString("selected_location", null)
        }

        val defaultAddress = "Mumbai, Maharashtra"

        if (!savedPhone.isNullOrBlank()) {
            currentPhone = savedPhone
            tvPhone?.text = currentPhone
        }

        currentAddress = if (!savedAddress.isNullOrBlank()) savedAddress else defaultAddress
        tvAddress?.text = currentAddress

        if (savedAddress.isNullOrBlank()) {
            sharedPreferences.edit().putString("customer_address", defaultAddress).apply()
            appPrefs.edit().putString("selected_location", defaultAddress).apply()
        }

        if (accessToken.isNullOrEmpty() || instanceUrl.isNullOrEmpty() || customerId.isNullOrEmpty()) {
            return
        }

        // Fetch latest profile details from Salesforce L_Customer__c
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val customer = CustomerRepository.getCustomerDetails(
                    instanceUrl = instanceUrl,
                    accessToken = accessToken,
                    customerId = customerId
                )

                if (customer != null) {
                    if (!customer.Name.isNullOrBlank()) {
                        currentName = customer.Name
                        tvName?.text = currentName
                    }
                    if (!customer.Email__c.isNullOrBlank()) {
                        currentEmail = customer.Email__c
                        tvEmail?.text = currentEmail
                    }
                    if (!customer.Phone_Number__c.isNullOrBlank()) {
                        currentPhone = customer.Phone_Number__c
                        tvPhone?.text = currentPhone
                        sharedPreferences.edit().putString("customer_phone", currentPhone).apply()
                    }
                    if (!customer.Address__c.isNullOrBlank()) {
                        currentAddress = customer.Address__c
                        tvAddress?.text = currentAddress
                        sharedPreferences.edit().putString("customer_address", currentAddress).apply()
                        appPrefs.edit().putString("selected_location", currentAddress).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEditProfileDialog(
        tvName: TextView?,
        tvEmail: TextView?,
        tvPhone: TextView?,
        tvAddress: TextView?
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        val etAddress = dialogView.findViewById<EditText>(R.id.etEditAddress)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelEdit)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        // Pre-fill current details
        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etPhone.setText(currentPhone)
        etAddress.setText(currentAddress)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()

            if (newName.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            val sharedPreferences =
                requireContext().getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

            val accessToken = sharedPreferences.getString("access_token", null)
            val instanceUrl = sharedPreferences.getString("instance_url", null)
            val customerId = sharedPreferences.getString("customer_id", null)

            // Update in Salesforce L_Customer__c if connected
            if (!accessToken.isNullOrEmpty() && !instanceUrl.isNullOrEmpty() && !customerId.isNullOrEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val updateReq = UpdateCustomerRequest(
                            Name = newName,
                            Email__c = newEmail,
                            Phone_Number__c = newPhone,
                            Address__c = newAddress
                        )

                        val success = CustomerRepository.updateCustomer(
                            instanceUrl = instanceUrl,
                            accessToken = accessToken,
                            customerId = customerId,
                            customer = updateReq
                        )

                        if (success) {
                            Toast.makeText(
                                requireContext(),
                                "Profile updated in Salesforce!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Updated locally. Salesforce sync failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Update Firebase User display name
            val currentUser = auth.currentUser
            if (currentUser != null && newName != currentUser.displayName) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                currentUser.updateProfile(profileUpdates)
            }

            // Update local state and UI
            currentName = newName
            currentEmail = newEmail
            currentPhone = newPhone
            currentAddress = newAddress

            tvName?.text = currentName
            tvEmail?.text = currentEmail
            tvPhone?.text = if (currentPhone.isNotBlank()) currentPhone else "+91 Phone Not Set"
            tvAddress?.text = if (currentAddress.isNotBlank()) currentAddress else "Address: Not Provided"

            // Save to preferences
            sharedPreferences.edit()
                .putString("customer_phone", currentPhone)
                .putString("customer_address", currentAddress)
                .apply()

            val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            appPrefs.edit()
                .putString("selected_location", currentAddress)
                .apply()

            dialog.dismiss()
        }

        dialog.show()
    }
}
