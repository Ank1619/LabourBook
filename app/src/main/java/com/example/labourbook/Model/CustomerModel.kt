package com.example.labourbook.model

data class CustomerModel(
    val Id: String,
    val Name: String?,
    val Email__c: String?,
    val Phone_Number__c: String?,
    val Address__c: String?,
    val Firebase_UID__c: String?
)

data class CustomerQueryResponse(
    val totalSize: Int? = 0,
    val done: Boolean? = true,
    val records: List<CustomerModel> = emptyList()
)
