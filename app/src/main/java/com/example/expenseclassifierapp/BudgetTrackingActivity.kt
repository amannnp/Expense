package com.example.expenseclassifierapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BudgetTrackingActivity : AppCompatActivity() {

    private lateinit var suggestionsView: TextView
    private lateinit var recyclerView: RecyclerView
    private val categoryTotals = mutableMapOf<String, Double>()
    private val budgetLimits = mapOf(
        "Food" to 3000.0,
        "Shopping" to 2000.0,
        "Entertainment" to 1500.0,
        "Travel" to 2500.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_tracking)

        suggestionsView = findViewById(R.id.suggestionsTextView)
        recyclerView = findViewById(R.id.budgetRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchExpenses()
    }

    private fun fetchExpenses() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val category = doc.getString("category") ?: continue
                    val amount = doc.getDouble("amount") ?: 0.0
                    categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
                }

                val suggestions = StringBuilder()
                categoryTotals.forEach { (category, total) ->
                    val limit = budgetLimits[category]
                    if (limit != null && total > limit) {
                        suggestions.append("⚠ You spent ₹${total.toInt()} on $category. Try reducing it below ₹${limit.toInt()}.\n\n")
                    }
                }

                if (suggestions.isEmpty()) {
                    suggestionsView.text = "🎉 You're within budget in all categories!"
                } else {
                    suggestionsView.text = suggestions.toString().trim()
                }

                recyclerView.adapter = BudgetAdapter(categoryTotals, budgetLimits)
            }
    }

}
