package com.example.labourbook.network

import com.example.labourbook.model.UpdateWorkerRequest
import com.example.labourbook.model.WorkerResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WorkerRepository {

    suspend fun getWorkers(
        instanceUrl: String,
        accessToken: String
    ): WorkerResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id,
                   Name,
                   Address__c,
                   Available__c,
                   Email__c,
                   Experience__c,
                   Labour_Category__c,
                   Phone_Number__c,
                   Profile_URL__c,
                   Skills__c,
                   Hourly_Rate__c,
                   Daily_Rate__c
            FROM Worker__c
        """.trimIndent()

        try {
            val response = api.getWorkers(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful) {
                return response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback query without rates if custom fields are missing
        val fallbackQuery = """
            SELECT Id,
                   Name,
                   Address__c,
                   Available__c,
                   Email__c,
                   Experience__c,
                   Labour_Category__c,
                   Phone_Number__c,
                   Profile_URL__c,
                   Skills__c
            FROM Worker__c
        """.trimIndent()

        return try {
            val response = api.getWorkers(
                authorization = "Bearer $accessToken",
                query = fallbackQuery
            )
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createWorker(
        instanceUrl: String,
        accessToken: String,
        worker: com.example.labourbook.model.WorkerRequest
    ): com.example.labourbook.model.WorkerCreateResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        return try {
            val response = api.createWorker(
                url = "services/data/v61.0/sobjects/Worker__c/",
                authorization = "Bearer $accessToken",
                worker = worker
            )
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getWorkerByEmail(
        instanceUrl: String,
        accessToken: String,
        email: String
    ): com.example.labourbook.model.Worker? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id,
                   Name,
                   Address__c,
                   Available__c,
                   Email__c,
                   Experience__c,
                   Labour_Category__c,
                   Phone_Number__c,
                   Profile_URL__c,
                   Skills__c,
                   Hourly_Rate__c,
                   Daily_Rate__c
            FROM Worker__c
            WHERE Email__c = '$email'
        """.trimIndent()

        return try {
            val response = api.getWorkers(
                authorization = "Bearer $accessToken",
                query = query
            )
            if (response.isSuccessful && response.body() != null && response.body()!!.records.isNotEmpty()) {
                response.body()!!.records[0]
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateWorkerAvailability(
        instanceUrl: String,
        accessToken: String,
        workerId: String,
        available: Boolean
    ): Boolean {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val updateReq = UpdateWorkerRequest(Available__c = available)

        return try {
            val response = api.updateWorker(
                url = "services/data/v61.0/sobjects/Worker__c/$workerId",
                authorization = "Bearer $accessToken",
                worker = updateReq
            )
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateWorkerProfile(
        instanceUrl: String,
        accessToken: String,
        workerId: String,
        updateReq: UpdateWorkerRequest
    ): Boolean {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        return try {
            val response = api.updateWorker(
                url = "services/data/v61.0/sobjects/Worker__c/$workerId",
                authorization = "Bearer $accessToken",
                worker = updateReq
            )
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
