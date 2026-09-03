package com.example.labourbook

import android.app.Application
import com.salesforce.androidsdk.app.SalesforceSDKManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SalesforceSDKManager.initNative(this, MainActivity::class.java)
    }
}
