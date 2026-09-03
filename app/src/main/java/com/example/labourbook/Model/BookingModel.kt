package com.example.labourbook.model

data class BookingModel(
    val Id: String,
    val Name: String,

    // Worker Lookup ID
    val Worker__c: String?,

    // Related Worker Object
    val Worker__r: WorkerReference?,

    // Customer Lookup ID and Object
    val L_Customer__c: String? = null,
    val L_Customer__r: CustomerReference? = null,

    val Booking_Date__c: String?,
    val Booking_Labour_Status__c: String?,
    val Amount__c: Double?,
    val Work_Address__c: String?,
    val Work_Description__c: String?,
    val Working_Hour__c: Double? = null,
    val Booking_Type__c: String? = null
)

data class WorkerReference(
    val Name: String?
)

data class CustomerReference(
    val Name: String?
)
