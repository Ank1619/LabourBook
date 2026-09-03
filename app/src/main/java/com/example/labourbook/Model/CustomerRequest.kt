package com.example.labourbook.model

data class CustomerRequest(
    val Name: String,
    val Email__c: String,
    val Firebase_UID__c: String,
    val Phone_Number__c: String
)