package com.example.labourbook.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.labourbook.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class fragment_worker_details : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_worker_details,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Receive all worker data from bundle
        val workerId = arguments?.getString("workerId")
        val workerName = arguments?.getString("workerName")
        val workerSpecialty = arguments?.getString("workerSpecialty")
        val workerRating = arguments?.getString("workerRating")
        val workerRate = arguments?.getString("workerRate")
        val workerAvatarEmoji = arguments?.getString("workerAvatarEmoji")

        val workerAddress = arguments?.getString("workerAddress")
        val workerPhone = arguments?.getString("workerPhone")
        val workerEmail = arguments?.getString("workerEmail")
        val workerExperience = arguments?.getInt("workerExperience")
        val workerAvailable = arguments?.getBoolean("workerAvailable")
        val workerSkills = arguments?.getString("workerSkills")
        val workerCategory = arguments?.getString("workerCategory")
        val workerHourlyRate = arguments?.getDouble("workerHourlyRate") ?: 0.0
        val workerDailyRate = arguments?.getDouble("workerDailyRate") ?: 0.0

        // 1. Profile Hero Section
        val tvWorkerAvatar = view.findViewById<TextView>(R.id.tvWorkerAvatar)
        val tvWorkerName = view.findViewById<TextView>(R.id.tvWorkerName)
        val tvProfession = view.findViewById<TextView>(R.id.tvProfession)
        val tvAvailabilityBadge = view.findViewById<TextView>(R.id.tvAvailabilityBadge)

        tvWorkerAvatar.text = if (!workerAvatarEmoji.isNullOrBlank()) workerAvatarEmoji else "👷"
        tvWorkerName.text = workerName ?: "Worker"
        tvProfession.text = workerSpecialty ?: workerCategory ?: "Professional Worker"

        val isAvailable = workerAvailable ?: true
        if (isAvailable) {
            tvAvailabilityBadge.text = "● Available for Work"
            tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_available)
            tvAvailabilityBadge.setTextColor(Color.parseColor("#15803D"))
        } else {
            tvAvailabilityBadge.text = "● Currently Unavailable"
            tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_unavailable)
            tvAvailabilityBadge.setTextColor(Color.parseColor("#64748B"))
        }

        // 2. Highlights / Stats Row
        val tvRating = view.findViewById<TextView>(R.id.tvRating)
        val tvExperience = view.findViewById<TextView>(R.id.tvExperience)
        val tvRate = view.findViewById<TextView>(R.id.tvRate)
        val tvBottomRate = view.findViewById<TextView>(R.id.tvBottomRate)

        val formattedRating = workerRating ?: "4.9"
        tvRating.text = if (formattedRating.startsWith("⭐")) formattedRating else "⭐ $formattedRating"
        
        tvExperience.text = if (workerExperience != null && workerExperience > 0) {
            "$workerExperience Yrs"
        } else {
            "3+ Yrs"
        }

        val rateString = when {
            workerHourlyRate > 0 && workerDailyRate > 0 -> "₹${workerHourlyRate.toInt()}/hr • ₹${workerDailyRate.toInt()}/day"
            workerHourlyRate > 0 -> "₹${workerHourlyRate.toInt()}/hr"
            workerDailyRate > 0 -> "₹${workerDailyRate.toInt()}/day"
            else -> workerRate ?: "₹350/hr"
        }
        tvRate.text = rateString
        tvBottomRate.text = rateString

        // 3. About Section (Main Feature)
        val tvAboutSummary = view.findViewById<TextView>(R.id.tvAboutSummary)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
        val tvLocation = view.findViewById<TextView>(R.id.tvLocation)
        val tvAvailabilityDetail = view.findViewById<TextView>(R.id.tvAvailabilityDetail)

        val name = workerName ?: "This worker"
        val profession = workerSpecialty ?: workerCategory ?: "service professional"
        val expStr = if (workerExperience != null && workerExperience > 0) {
            "$workerExperience years of hands-on"
        } else {
            "extensive"
        }
        val locStr = if (!workerAddress.isNullOrBlank()) " based in $workerAddress" else ""

        tvAboutSummary.text = "$name is a highly skilled $profession with $expStr experience$locStr. Dedicated to providing prompt response, high reliability, and top-quality service."

        tvPhone.text = if (!workerPhone.isNullOrBlank()) workerPhone else "+91 98765 43210"
        tvEmail.text = if (!workerEmail.isNullOrBlank()) workerEmail else "Not Provided"
        tvCategory.text = workerCategory ?: workerSpecialty ?: "Skilled Labour"
        tvLocation.text = if (!workerAddress.isNullOrBlank()) workerAddress else "Location Not Specified"

        if (isAvailable) {
            tvAvailabilityDetail.text = "Available for Immediate Hire"
            tvAvailabilityDetail.setTextColor(Color.parseColor("#16A34A"))
        } else {
            tvAvailabilityDetail.text = "Currently Busy / Unavailable"
            tvAvailabilityDetail.setTextColor(Color.parseColor("#64748B"))
        }

        // 4. Skills & Expertise Chips
        val chipGroupSkills = view.findViewById<ChipGroup>(R.id.chipGroupSkills)
        val skillsList = if (!workerSkills.isNullOrBlank()) {
            workerSkills.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(
                workerSpecialty ?: "Quality Work",
                "Installation & Repair",
                "Maintenance",
                "On-Time Service"
            )
        }

        chipGroupSkills.removeAllViews()
        skillsList.forEach { skill ->
            val chip = Chip(requireContext()).apply {
                text = skill
                setChipBackgroundColorResource(R.color.bg_light)
                setTextColor(Color.parseColor("#1D4ED8"))
                chipCornerRadius = 24f
                isClickable = false
                isCheckable = false
                elevation = 0f
            }
            chipGroupSkills.addView(chip)
        }

        // Back button
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        // Favorite button toggle animation/toast
        var isFavorite = false
        val btnFavorite = view.findViewById<ImageView>(R.id.btnFavorite)
        btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            if (isFavorite) {
                btnFavorite.setColorFilter(Color.parseColor("#EF4444"))
            } else {
                btnFavorite.setColorFilter(Color.parseColor("#6C737F"))
            }
        }

        // Book Worker Button
        view.findViewById<View>(R.id.btnBookWorker).setOnClickListener {
            val bookingBundle = Bundle().apply {
                putString("workerId", workerId)
                putString("workerName", workerName)
                putString("workerPhone", workerPhone)
                putString("workerAddress", workerAddress)
                putString("workerCategory", workerCategory ?: workerSpecialty)
                putString("workerRate", workerRate)
                putDouble("workerHourlyRate", workerHourlyRate)
                putDouble("workerDailyRate", workerDailyRate)
            }

            findNavController().navigate(
                R.id.action_workerDetails_to_confirmBooking,
                bookingBundle
            )
        }
    }
}