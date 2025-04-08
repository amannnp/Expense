package com.example.expenseclassifierapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var amountInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter

    private val expenses = mutableListOf<Expense>()
    private val categories = listOf("Food", "Transport", "Entertainment", "Rent", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amountInput = findViewById(R.id.amountInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        saveButton = findViewById(R.id.saveButton)
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)

        adapter = ExpenseAdapter(expenses)
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = adapter

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        saveButton.setOnClickListener {
            val amountText = amountInput.text.toString()
            if (amountText.isBlank()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val predictedCategory = predictCategory(amount)
            val selectedCategory = categorySpinner.selectedItem.toString()

            val finalCategory = if (predictedCategory != selectedCategory) selectedCategory else predictedCategory
            val expense = Expense(amount, finalCategory)
            expenses.add(expense)
            adapter.notifyDataSetChanged()
            amountInput.text.clear()
            categorySpinner.setSelection(0)
        }
    }

    // Dummy prediction logic (replace with ML model integration later)
    private fun predictCategory(amount: Double): String {
        return when {
            amount < 100 -> "Food"
            amount in 100.0..300.0 -> "Transport"
            amount in 300.0..1000.0 -> "Entertainment"
            else -> "Rent"
        }
    }
}
