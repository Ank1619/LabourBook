package com.example.labourbook

data class PendingBookingResponse(

    val totalSize: Int,

    val done: Boolean,

    val records: List<PendingBookingModel>
)