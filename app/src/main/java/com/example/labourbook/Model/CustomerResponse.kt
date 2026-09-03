package com.example.labourbook.model

data class CustomerResponse(
    val id: String,
    val success: Boolean,
    val errors: List<String>?
)