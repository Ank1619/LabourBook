package com.example.labourbook.model

data class ServiceAreaResponse(

    val totalSize: Int,

    val done: Boolean,

    val records: List<ServiceAreaModel>
)