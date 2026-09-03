package com.example.labourbook.model

data class LabourCategoryModel(
    val Id: String,
    val Name: String,
    val Active__c: Boolean? = true,
    val Category_ImageURL__c: String? = null,
    val Description__c: String? = null
)