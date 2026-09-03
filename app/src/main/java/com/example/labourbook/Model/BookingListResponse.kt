package com.example.labourbook.model

data class BookingListResponse(
    val totalSize: Int,
    val done: Boolean,
    val records: List<BookingModel>
)