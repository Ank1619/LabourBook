package com.example.labourbook.model

data class WorkerRequest(
    val Name: String,
    val Email__c: String,
    val Phone_Number__c: String,
    val Address__c: String,
    val Available__c: Boolean = true,
    val Daily_Rate__c: Double? = null,
    val Hourly_Rate__c: Double? = null,
    val Experience__c: Int? = null,
    val Labour_Category__c: String? = null,
    val Skills__c: String? = null,
    val Profile_URL__c: String? = null
)
