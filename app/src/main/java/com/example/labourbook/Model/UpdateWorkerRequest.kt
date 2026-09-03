package com.example.labourbook.model

data class UpdateWorkerRequest(
    val Name: String? = null,
    val Phone_Number__c: String? = null,
    val Address__c: String? = null,
    val Email__c: String? = null,
    val Experience__c: Int? = null,
    val Hourly_Rate__c: Double? = null,
    val Daily_Rate__c: Double? = null,
    val Skills__c: String? = null,
    val Profile_URL__c: String? = null,
    val Available__c: Boolean? = null
)
