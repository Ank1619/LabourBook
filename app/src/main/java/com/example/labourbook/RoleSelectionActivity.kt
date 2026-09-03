package com.example.labourbook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val cardCustomer = findViewById<MaterialCardView>(R.id.cardCustomer)
        val cardWorker = findViewById<MaterialCardView>(R.id.cardWorker)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        cardCustomer.setOnClickListener {
            prefs.edit().putString("user_role", "customer").apply()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        cardWorker.setOnClickListener {
            prefs.edit().putString("user_role", "worker").apply()
            val intent = Intent(this, WorkerLogin::class.java)
            startActivity(intent)
        }
    }
}
