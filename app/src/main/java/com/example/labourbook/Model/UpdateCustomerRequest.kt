package com.example.labourbook.model

data class UpdateCustomerRequest(
    val Name: String? = null,
    val Email__c: String? = null,
    val Phone_Number__c: String? = null,
    val Address__c: String? = null
)
