package com.example.expenseclassifierapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView

class BudgetTrackingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_tracking)

        val suggestionsView = findViewById<TextView>(R.id.suggestionsTextView)
        val recyclerView = findViewById<RecyclerView>(R.id.budgetRecyclerView)

        // Show "Coming Soon" message
        suggestionsView.text = "Coming Soon!"

        // Hide the RecyclerView since we have no data
        recyclerView.visibility = View.GONE
    }
}
