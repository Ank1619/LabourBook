package com.example.labourbook.model

data class WorkerResponse(
    val totalSize: Int,
    val done: Boolean,
    val records: List<Worker>
)

data class Worker(
    val Id: String,
    val Name: String,
    val Address__c: String?,
    val Available__c: Boolean?,
    val Email__c: String?,
    val Experience__c: Int?,
    val Labour_Category__c: String?,
    val Phone_Number__c: String?,
    val Profile_URL__c: String?,
    val Skills__c: String?,
    val Hourly_Rate__c: Double? = null,
    val Daily_Rate__c: Double? = null
)