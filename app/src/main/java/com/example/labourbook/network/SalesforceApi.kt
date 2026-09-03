package com.example.labourbook.network

import com.example.labourbook.PendingBookingResponse
import com.example.labourbook.model.BookingListResponse
import com.example.labourbook.model.BookingRequest
import com.example.labourbook.model.BookingResponse
import com.example.labourbook.model.CancelBookingRequest
import com.example.labourbook.model.CustomerQueryResponse
import com.example.labourbook.model.CustomerRequest
import com.example.labourbook.model.CustomerResponse
import com.example.labourbook.model.LabourCategoryResponse
import com.example.labourbook.model.ServiceAreaResponse
import com.example.labourbook.model.UpdateBookingStatusRequest
import com.example.labourbook.model.UpdateCustomerRequest
import com.example.labourbook.model.WorkerProfileResponse
import com.example.labourbook.model.WorkerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface SalesforceApi {

    @GET("services/data/v61.0/query")
    suspend fun getLabourCategories(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<LabourCategoryResponse>


    // Get Workers
    @GET("services/data/v61.0/query")
    suspend fun getWorkers(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<WorkerResponse>


    // Get Bookings
    @GET("services/data/v61.0/query")
    suspend fun getBookings(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<BookingListResponse>


    // Get Pending Bookings for Worker
    @GET("services/data/v61.0/query")
    suspend fun queryPendingBookings(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<PendingBookingResponse>


    // Create Customer
    @POST
    suspend fun createCustomer(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body customer: CustomerRequest
    ): Response<CustomerResponse>


    // Create Booking
    @POST
    suspend fun createBooking(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body booking: BookingRequest
    ): Response<BookingResponse>


    // Cancel Booking
    @PATCH("services/data/v61.0/sobjects/Booking__c/{bookingId}")
    suspend fun cancelBooking(
        @Header("Authorization") authorization: String,
        @Path("bookingId") bookingId: String,
        @Body request: CancelBookingRequest
    ): Response<Void>


    // Update Booking Status
    @PATCH("services/data/v61.0/sobjects/Booking__c/{bookingId}")
    suspend fun updateBookingStatus(
        @Header("Authorization") authorization: String,
        @Path("bookingId") bookingId: String,
        @Body request: UpdateBookingStatusRequest
    ): Response<Void>


    // Query Customer Details
    @GET("services/data/v61.0/query")
    suspend fun getCustomerDetails(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<CustomerQueryResponse>


    // Get Worker Profile
    @GET("services/data/v61.0/query")
    suspend fun getWorkerProfile(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<WorkerProfileResponse>


    // Update Customer
    @PATCH
    suspend fun updateCustomer(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body customer: UpdateCustomerRequest
    ): Response<Void>


    // Fetch active Service Areas
    @GET("services/data/v61.0/query")
    suspend fun getServiceAreas(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<ServiceAreaResponse>


    // Create Worker
    @POST
    suspend fun createWorker(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body worker: com.example.labourbook.model.WorkerRequest
    ): Response<com.example.labourbook.model.WorkerCreateResponse>


    // Update Worker
    @PATCH
    suspend fun updateWorker(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body worker: com.example.labourbook.model.UpdateWorkerRequest
    ): Response<Void>
}