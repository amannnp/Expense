package com.example.expenseclassifierapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class MainActivity : AppCompatActivity() {

    private lateinit var amountInput: EditText
    private lateinit var merchantInput: MaterialAutoCompleteTextView
    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var predictButton: Button
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter

    private lateinit var classifier: Classifier
    private lateinit var auth: FirebaseAuth

    private val expenses = mutableListOf<Expense>()
    private lateinit var categories: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Bind views
        amountInput = findViewById(R.id.amountInput)
        merchantInput = findViewById(R.id.merchantInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        predictButton = findViewById(R.id.predictButton)
        saveButton = findViewById(R.id.saveButton)
        logoutButton = findViewById(R.id.logoutButton)
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)

        adapter = ExpenseAdapter(expenses)
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = adapter

        classifier = Classifier(this)
        categories = classifier.getLabels()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        setupMerchantDropdown()

        predictButton.setOnClickListener {
            hideKeyboard()
            handlePrediction()
        }

        saveButton.setOnClickListener {
            hideKeyboard()
            handleSave()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handlePrediction() {
        val amountText = amountInput.text.toString().trim()
        val merchantName = merchantInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (amountText.isBlank()) return showToast("Enter an amount")
        if (merchantName.isBlank()) return showToast("Enter or select a merchant")

        val amount = amountText.toFloatOrNull()
        if (amount == null) return showToast("Invalid amount format")

        val inputText = "$merchantName $description"
        val predictedCategory = classifier.classify(inputText, amount)
        val index = categories.indexOf(predictedCategory).takeIf { it >= 0 } ?: 0
        categorySpinner.setSelection(index)
        showToast("Predicted: $predictedCategory")
    }

    private fun handleSave() {
        val amountText = amountInput.text.toString().trim()
        val merchant = merchantInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "Other"

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            showToast("Please enter a valid amount")
            return
        }

        if (merchant.isEmpty()) {
            showToast("Merchant cannot be empty")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("User not signed in")
            return
        }

        val expense = hashMapOf(
            "merchant" to merchant,
            "description" to description,
            "amount" to amount,
            "category" to selectedCategory,
            "timestamp" to System.currentTimeMillis(),
            "userId" to userId
        )

        FirebaseFirestore.getInstance().collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                showToast("Expense saved to Firebase!")
                val newExpense = Expense(amount, selectedCategory)
                expenses.add(0, newExpense) // Add to top
                adapter.notifyItemInserted(0)
                expenseRecyclerView.scrollToPosition(0)

                clearInputs()
            }
            .addOnFailureListener { e ->
                showToast("Error saving expense: ${e.message}")
            }
    }

    private fun setupMerchantDropdown() {
        val merchants = loadMerchantsFromAssets()
        val merchantAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, merchants)
        merchantInput.setAdapter(merchantAdapter)
        merchantInput.threshold = 1
        merchantInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) merchantInput.showDropDown()
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

    private fun clearInputs() {
        amountInput.setText("")
        merchantInput.setText("")
        descriptionInput.setText("")
        categorySpinner.setSelection(0)
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
