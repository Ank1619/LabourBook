package com.example.labourbook.model

data class WorkerModel(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: String,
    val rate: String,
    val avatarEmoji: String,

    // Salesforce details
    val address: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val experience: Int? = null,
    val available: Boolean? = null,
    val skills: String? = null,
    val profileUrl: String? = null,
    val labourCategory: String? = null,
    val hourlyRate: Double? = null,
    val dailyRate: Double? = null
)