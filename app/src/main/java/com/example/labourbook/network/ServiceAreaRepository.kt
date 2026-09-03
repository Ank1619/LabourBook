package com.example.labourbook.network

import com.example.labourbook.model.ServiceAreaResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceAreaRepository {

    suspend fun getServiceAreas(
        instanceUrl: String,
        accessToken: String
    ): ServiceAreaResponse? {

        val retrofit =
            Retrofit.Builder()
                .baseUrl("$instanceUrl/")
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()

        val api =
            retrofit.create(
                SalesforceApi::class.java
            )

        val query =
            """
            SELECT Id,
                   Name,
                   City_Area__c,
                   State__c,
                   Active__c
            FROM Service_Area__c
            WHERE Active__c = true
            ORDER BY City_Area__c ASC
            """.trimIndent()
                .replace("\n", " ")

        try {
            val response =
                api.getServiceAreas(
                    authorization = "Bearer $accessToken",
                    query = query
                )

            if (response.isSuccessful && response.body() != null) {
                return response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback Query without Active__c filter
        val fallbackQuery =
            """
            SELECT Id,
                   Name,
                   City_Area__c,
                   State__c
            FROM Service_Area__c
            """.trimIndent()
                .replace("\n", " ")

        return try {
            val response =
                api.getServiceAreas(
                    authorization = "Bearer $accessToken",
                    query = fallbackQuery
                )

            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}