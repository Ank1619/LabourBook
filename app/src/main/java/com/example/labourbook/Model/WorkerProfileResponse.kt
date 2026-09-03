package com.example.labourbook.model

data class WorkerProfileResponse(
    val totalSize: Int,
    val done: Boolean,
    val records: List<WorkerProfile>
)

data class WorkerProfile(
    val Id: String,
    val Name: String,

    val Address__c: String?,
    val Available__c: Boolean?,

    val Daily_Rate__c: Double?,
    val Email__c: String?,

    val Experience__c: Int?,
    val Hourly_Rate__c: Double?,

    val Labour_Category__c: String?,
    val Labour_Category__r: LabourCategoryLookup?,

    val Phone_Number__c: String?,
    val Profile_URL__c: String?,

    val Skills__c: String?
)

data class LabourCategoryLookup(
    val Name: String?
)