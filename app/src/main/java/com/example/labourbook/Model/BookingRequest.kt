package com.example.labourbook.model

data class BookingRequest(
    val Worker__c: String,
    val L_Customer__c: String,
    val Booking_Date__c: String,
    val Booking_Labour_Status__c: String,
    val Amount__c: Double,
    val Work_Address__c: String,
    val Work_Description__c: String,
    val Working_Hour__c: Double? = null,
    val Booking_Type__c: String? = null
)