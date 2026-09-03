package com.example.labourbook.model

data class WorkerCreateResponse(
    val id: String? = null,
    val success: Boolean = false,
    val errors: List<String>? = null
)
