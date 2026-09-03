package com.example.labourbook.model

data class LabourCategoryResponse(
    val totalSize: Int? = 0,
    val done: Boolean? = true,
    val records: List<LabourCategoryModel> = emptyList()
)