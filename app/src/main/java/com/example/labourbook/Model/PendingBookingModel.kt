package com.example.labourbook

data class PendingBookingModel(

    val Id: String,

    val Name: String,

    val Booking_Type__c: String?,

    val Work_Description__c: String?,

    val Work_Address__c: String?,

    val Booking_Date__c: String?,

    val Working_Hour__c: Int?,

    val Amount__c: Double?,

    val Booking_Labour_Status__c: String?
)