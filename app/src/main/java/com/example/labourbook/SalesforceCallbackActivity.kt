package com.example.labourbook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.labourbook.model.TokenResponse
import com.example.labourbook.network.SalesforceAuthManager
import com.example.labourbook.network.SalesforceTokenApi
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SalesforceCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data

        if (uri != null) {

            val code = uri.getQueryParameter("code")

            if (code != null) {

                exchangeCodeForToken(code)

            } else {

                val error = uri.getQueryParameter("error")

                Toast.makeText(
                    this,
                    "Salesforce Login failed: $error",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }

        } else {
            finish()
        }
    }

    private fun exchangeCodeForToken(code: String) {

        val sharedPreferences =
            getSharedPreferences(
                "salesforce_auth",
                Context.MODE_PRIVATE
            )

        // Get the PKCE verifier saved before login
        val codeVerifier =
            sharedPreferences.getString(
                "code_verifier",
                null
            )

        if (codeVerifier == null) {

            Toast.makeText(
                this,
                "PKCE verifier not found",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(SalesforceAuthManager.LOGIN_URL + "/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

        val tokenApi =
            retrofit.create(SalesforceTokenApi::class.java)

        lifecycleScope.launch {

            try {

                val response =
                    tokenApi.getAccessToken(
                        code = code,
                        clientId =
                            SalesforceAuthManager.CLIENT_ID,
                        redirectUri =
                            SalesforceAuthManager.REDIRECT_URI,
                        codeVerifier =
                            codeVerifier
                    )

                if (response.isSuccessful) {

                    val tokenResponse =
                        response.body()

                    if (tokenResponse != null) {

                        // Save Access Token
                        sharedPreferences.edit()
                            .putString(
                                "access_token",
                                tokenResponse.access_token
                            )
                            .putString(
                                "instance_url",
                                tokenResponse.instance_url
                            )
                            .apply()

                        Toast.makeText(
                            this@SalesforceCallbackActivity,
                            "Salesforce Connected Successfully!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Open MainActivity
                        val intent = Intent(
                            this@SalesforceCallbackActivity,
                            MainActivity::class.java
                        )

                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)

                        finish()
                    }

                } else {

                    Toast.makeText(
                        this@SalesforceCallbackActivity,
                        "Token Error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@SalesforceCallbackActivity,
                    "Connection Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }
}