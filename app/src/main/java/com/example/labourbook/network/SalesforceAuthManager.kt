package com.example.labourbook.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom

object SalesforceAuthManager {

    const val CLIENT_ID = "3MVG9rZjd7MXFdLgMPU3pTgCBiOaxo1dlvE854g4YgIBguXOcQ404p5rnU768QJJVegvkULyzm2yRLl.sz3gn"

    const val REDIRECT_URI = "labourbook://oauth/callback"

    const val LOGIN_URL =
        "https://orgfarm-6e68dba351-dev-ed.develop.my.salesforce.com"

    fun login(context: Context) {

        // Step 1: Create a secret temporary PKCE code verifier
        val codeVerifier = generateCodeVerifier()

        // Step 2: Create code challenge from the verifier
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // Save verifier temporarily.
        // We need it later to exchange the authorization code for an access token.
        val sharedPreferences =
            context.getSharedPreferences("salesforce_auth", Context.MODE_PRIVATE)

        sharedPreferences.edit()
            .putString("code_verifier", codeVerifier)
            .apply()

        // Step 3: Build Salesforce login URL
        val authUrl = Uri.parse("$LOGIN_URL/services/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)

            // PKCE parameters
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")

            .build()

        // Step 4: Open Salesforce login page
        val customTabsIntent = CustomTabsIntent.Builder().build()

        customTabsIntent.launchUrl(context, authUrl)
    }


    // Generates a random secret code
    private fun generateCodeVerifier(): String {

        val randomBytes = ByteArray(32)

        SecureRandom().nextBytes(randomBytes)

        return Base64.encodeToString(
            randomBytes,
            Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
        )
    }


    // Converts verifier into SHA-256 challenge
    private fun generateCodeChallenge(
        codeVerifier: String
    ): String {

        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)

        val messageDigest =
            MessageDigest.getInstance("SHA-256")

        val digest =
            messageDigest.digest(bytes)

        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
        )
    }
}