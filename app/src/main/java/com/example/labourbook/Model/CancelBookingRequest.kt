package com.example.labourbook.model

data class CancelBookingRequest(
    val Booking_Labour_Status__c: String = "Cancelled"
)