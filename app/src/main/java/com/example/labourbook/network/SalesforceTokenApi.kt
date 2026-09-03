package com.example.labourbook.network

import com.example.labourbook.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SalesforceTokenApi {

    @FormUrlEncoded
    @POST("services/oauth2/token")
    suspend fun getAccessToken(

        @Field("grant_type")
        grantType: String = "authorization_code",

        @Field("code")
        code: String,

        @Field("client_id")
        clientId: String,

        @Field("redirect_uri")
        redirectUri: String,

        @Field("code_verifier")
        codeVerifier: String

    ): Response<TokenResponse>
}