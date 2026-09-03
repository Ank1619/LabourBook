package com.example.labourbook.network

import com.example.labourbook.model.LabourCategoryResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LabourCategoryRepository {

    suspend fun getLabourCategories(
        instanceUrl: String,
        accessToken: String
    ): LabourCategoryResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

        val api =
            retrofit.create(
                SalesforceApi::class.java
            )

        val query = """
            SELECT Id,
                   Name,
                   Active__c,
                   Category_ImageURL__c,
                   Description__c
            FROM Labour_Category__c
            WHERE Active__c = true
            ORDER BY Name ASC
        """.trimIndent()

        try {
            val response =
                api.getLabourCategories(
                    authorization = "Bearer $accessToken",
                    query = query
                )

            if (response.isSuccessful && response.body() != null && response.body()!!.records.isNotEmpty()) {
                return response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback query if Active__c or custom fields are not present or if WHERE clause fails
        val fallbackQuery = """
            SELECT Id,
                   Name
            FROM Labour_Category__c
            ORDER BY Name ASC
        """.trimIndent()

        return try {
            val response =
                api.getLabourCategories(
                    authorization = "Bearer $accessToken",
                    query = fallbackQuery
                )

            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}