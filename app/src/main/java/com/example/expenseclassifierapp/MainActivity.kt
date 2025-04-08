package com.example.expenseclassifierapp

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var amountInput: EditText
    private lateinit var merchantInput: AutoCompleteTextView
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
        merchantInput = findViewById(R.id.merchantInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        saveButton = findViewById(R.id.saveButton)
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)

        adapter = ExpenseAdapter(expenses)
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = adapter

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        // Load merchants and set up autocomplete
        val merchants = loadMerchantsFromAssets()
        Log.d("MerchantDebug", "Loaded ${merchants.size} merchants") // for debugging

        val merchantAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, merchants)
        merchantInput.setAdapter(merchantAdapter)
        merchantInput.threshold = 1
        merchantInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) merchantInput.showDropDown()
        }

        saveButton.setOnClickListener {
            val amountText = amountInput.text.toString()
            val merchantName = merchantInput.text.toString()

            if (amountText.isBlank()) {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (merchantName.isBlank()) {
                Toast.makeText(this, "Enter or select a merchant", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val predictedCategory = predictCategory(amount, merchantName)
            val selectedCategory = categorySpinner.selectedItem.toString()

            val finalCategory = if (predictedCategory != selectedCategory) selectedCategory else predictedCategory
            val expense = Expense(amount, finalCategory)
            expenses.add(expense)
            adapter.notifyDataSetChanged()

            // Clear inputs
            amountInput.text.clear()
            merchantInput.text.clear()
            categorySpinner.setSelection(0)
        }
    }

    private fun loadMerchantsFromAssets(): List<String> {
        return try {
            val jsonString = assets.open("merchants.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            Log.e("MerchantLoadError", "Error loading merchants: ${e.message}")
            emptyList()
        }
    }

    // Dummy prediction logic (to be replaced by ML model later)
    private fun predictCategory(amount: Double, merchant: String): String {
        return when {
            merchant.contains("Zomato", true) || merchant.contains("Swiggy", true) -> "Food"
            merchant.contains("Uber", true) || merchant.contains("Ola", true) -> "Transport"
            amount < 100 -> "Food"
            amount in 100.0..300.0 -> "Transport"
            amount in 300.0..1000.0 -> "Entertainment"
            else -> "Rent"
        }
    }
}
