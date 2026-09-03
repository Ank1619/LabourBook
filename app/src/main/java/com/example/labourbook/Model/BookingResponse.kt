package com.example.labourbook.model

data class BookingResponse(
    val id: String,
    val success: Boolean,
    val errors: List<String>?
)