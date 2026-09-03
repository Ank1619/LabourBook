package com.example.labourbook.network

import com.example.labourbook.model.WorkerProfile

object WorkerProfileRepository {

    suspend fun getWorkerProfile(
        instanceUrl: String,
        accessToken: String,
        workerId: String
    ): WorkerProfile? {

        return try {

            val query = """
                SELECT
                    Id,
                    Name,
                    Address__c,
                    Available__c,
                    Daily_Rate__c,
                    Email__c,
                    Experience__c,
                    Hourly_Rate__c,
                    Labour_Category__c,
                    Labour_Category__r.Name,
                    Phone_Number__c,
                    Profile_URL__c,
                    Skills__c
                FROM Worker__c
                WHERE Id = '$workerId'
            """.trimIndent().replace("\n", " ")

            val retrofit =
                RetrofitClient.getClient(instanceUrl)

            val api =
                retrofit.create(SalesforceApi::class.java)

            val response =
                api.getWorkerProfile(
                    authorization = "Bearer $accessToken",
                    query = query
                )

            if (
                response.isSuccessful &&
                response.body() != null
            ) {

                response.body()?.records?.firstOrNull()

            } else {

                null
            }

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }
}
