package com.example.labourbook.network

import com.example.labourbook.model.CustomerModel
import com.example.labourbook.model.CustomerRequest
import com.example.labourbook.model.CustomerResponse
import com.example.labourbook.model.UpdateCustomerRequest
import com.google.firebase.auth.FirebaseUser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CustomerRepository {

    suspend fun createCustomer(
        instanceUrl: String,
        accessToken: String,
        customer: CustomerRequest
    ): CustomerResponse? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val response = api.createCustomer(
            url = "services/data/v61.0/sobjects/L_Customer__c/",
            authorization = "Bearer $accessToken",
            customer = customer
        )

        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getCustomerDetails(
        instanceUrl: String,
        accessToken: String,
        customerId: String
    ): CustomerModel? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id, Name, Email__c, Phone_Number__c, Address__c, Firebase_UID__c
            FROM L_Customer__c
            WHERE Id = '$customerId'
        """.trimIndent().replace("\n", " ")

        return try {
            val response = api.getCustomerDetails(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful && response.body() != null && response.body()!!.records.isNotEmpty()) {
                response.body()!!.records[0]
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCustomerByFirebaseUid(
        instanceUrl: String,
        accessToken: String,
        firebaseUid: String
    ): CustomerModel? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id, Name, Email__c, Phone_Number__c, Address__c, Firebase_UID__c
            FROM L_Customer__c
            WHERE Firebase_UID__c = '$firebaseUid'
        """.trimIndent().replace("\n", " ")

        return try {
            val response = api.getCustomerDetails(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful && response.body() != null && response.body()!!.records.isNotEmpty()) {
                response.body()!!.records[0]
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCustomerByEmail(
        instanceUrl: String,
        accessToken: String,
        email: String
    ): CustomerModel? {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        val query = """
            SELECT Id, Name, Email__c, Phone_Number__c, Address__c, Firebase_UID__c
            FROM L_Customer__c
            WHERE Email__c = '$email'
        """.trimIndent().replace("\n", " ")

        return try {
            val response = api.getCustomerDetails(
                authorization = "Bearer $accessToken",
                query = query
            )

            if (response.isSuccessful && response.body() != null && response.body()!!.records.isNotEmpty()) {
                response.body()!!.records[0]
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun ensureCustomerId(
        instanceUrl: String,
        accessToken: String,
        firebaseUser: FirebaseUser,
        phone: String = ""
    ): String? {
        // 1. Check by Firebase UID
        val existingCustomer = getCustomerByFirebaseUid(instanceUrl, accessToken, firebaseUser.uid)
        if (existingCustomer != null) {
            return existingCustomer.Id
        }

        // 2. Check by Email if email is present
        val email = firebaseUser.email
        if (!email.isNullOrBlank()) {
            val customerByEmail = getCustomerByEmail(instanceUrl, accessToken, email)
            if (customerByEmail != null) {
                return customerByEmail.Id
            }
        }

        // 3. Create new Customer record in Salesforce
        val newCustomerReq = CustomerRequest(
            Name = firebaseUser.displayName ?: "Customer",
            Email__c = email ?: "",
            Firebase_UID__c = firebaseUser.uid,
            Phone_Number__c = phone
        )

        val createResponse = createCustomer(instanceUrl, accessToken, newCustomerReq)
        return createResponse?.id
    }

    suspend fun updateCustomer(
        instanceUrl: String,
        accessToken: String,
        customerId: String,
        customer: UpdateCustomerRequest
    ): Boolean {

        val retrofit = Retrofit.Builder()
            .baseUrl("$instanceUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(SalesforceApi::class.java)

        return try {
            val response = api.updateCustomer(
                url = "services/data/v61.0/sobjects/L_Customer__c/$customerId",
                authorization = "Bearer $accessToken",
                customer = customer
            )
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
