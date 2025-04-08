package com.example.expenseclassifierapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var amountInput: EditText
    private lateinit var merchantInput: AutoCompleteTextView
    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var predictButton: Button
    private lateinit var saveButton: Button
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter

    private lateinit var classifier: Classifier

    private val expenses = mutableListOf<Expense>()
    private lateinit var categories: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amountInput = findViewById(R.id.amountInput)
        merchantInput = findViewById(R.id.merchantInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        predictButton = findViewById(R.id.predictButton)
        saveButton = findViewById(R.id.saveButton)
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)

        adapter = ExpenseAdapter(expenses)
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = adapter

        // Initialize the classifier and load labels
        classifier = Classifier(this)
        categories = classifier.getLabels()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        val merchants = loadMerchantsFromAssets()
        val merchantAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, merchants)
        merchantInput.setAdapter(merchantAdapter)
        merchantInput.threshold = 1
        merchantInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) merchantInput.showDropDown()
        }

        predictButton.setOnClickListener {
            hideKeyboard()

            val amountText = amountInput.text.toString()
            val merchantName = merchantInput.text.toString()
            val description = descriptionInput.text.toString()

            if (amountText.isBlank()) {
                showToast("Enter an amount")
                return@setOnClickListener
            }

            if (merchantName.isBlank()) {
                showToast("Enter or select a merchant")
                return@setOnClickListener
            }

            val amount = amountText.toFloatOrNull()
            if (amount == null) {
                showToast("Invalid amount format")
                return@setOnClickListener
            }

            val inputText = "$merchantName $description"
            val predictedCategory = classifier.classify(inputText, amount)

            val index = categories.indexOf(predictedCategory).takeIf { it >= 0 } ?: 0
            categorySpinner.setSelection(index)

            showToast("Predicted: $predictedCategory")
        }

        saveButton.setOnClickListener {
            hideKeyboard()

            val amountText = amountInput.text.toString()
            val amount = amountText.toDoubleOrNull()

            if (amount == null) {
                showToast("Please enter a valid amount")
                return@setOnClickListener
            }

            val selectedCategory = categorySpinner.selectedItem.toString()
            val expense = Expense(amount, selectedCategory)

            expenses.add(expense)
            adapter.notifyDataSetChanged()

            amountInput.text.clear()
            merchantInput.text.clear()
            descriptionInput.text.clear()
            categorySpinner.setSelection(0)

            showToast("Expense saved!")
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
