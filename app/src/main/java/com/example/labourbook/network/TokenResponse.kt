package com.example.labourbook.model

data class TokenResponse(

    val access_token: String,

    val instance_url: String,

    val token_type: String,

    val issued_at: String? = null,

    val signature: String? = null

)