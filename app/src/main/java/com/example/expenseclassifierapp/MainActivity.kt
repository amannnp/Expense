package com.example.expenseclassifierapp
import com.example.expenseclassifierapp.Expense
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

        adapter = ExpenseAdapter(expenses) { expense ->
            showDeleteConfirmation(expense)
        }

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

        loadExpenses()
    }

    private fun loadExpenses() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("expenses")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                expenses.clear()
                for (doc in documents) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: "Other"
                    val timestamp = when (val raw = doc["timestamp"]) {
                        is Timestamp -> raw
                        is Map<*, *> -> {
                            val seconds = (raw["_seconds"] as? Number)?.toLong() ?: 0L
                            val nanos = (raw["_nanoseconds"] as? Number)?.toInt() ?: 0
                            Timestamp(seconds, nanos)
                        }
                        else -> Timestamp.now()
                    }


                    val documentId = doc.id
                    expenses.add(Expense(amount, category, timestamp, documentId))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showToast("Error loading expenses: ${e.message}")
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
        if (amount == null) return showToast("Please enter a valid amount")
        if (merchant.isEmpty()) return showToast("Merchant cannot be empty")

        val userId = auth.currentUser?.uid ?: return showToast("User not signed in")
        val timestamp = Timestamp.now()

        val expenseMap = hashMapOf(
            "merchant" to merchant,
            "description" to description,
            "amount" to amount,
            "category" to selectedCategory,
            "timestamp" to timestamp,
            "userId" to userId
        )

        FirebaseFirestore.getInstance().collection("expenses")
            .add(expenseMap)
            .addOnSuccessListener { doc ->
                val newExpense = Expense(amount, selectedCategory, timestamp, doc.id)
                expenses.add(0, newExpense)
                adapter.notifyItemInserted(0)
                expenseRecyclerView.scrollToPosition(0)
                clearInputs()
                showToast("Expense saved!")
            }
            .addOnFailureListener {
                showToast("Error saving expense: ${it.message}")
            }
    }

    private fun deleteExpense(expense: Expense) {
        FirebaseFirestore.getInstance().collection("expenses").document(expense.documentId)
            .delete()
            .addOnSuccessListener {
                adapter.removeExpense(expense)
                showToast("Expense deleted")
            }
            .addOnFailureListener {
                showToast("Failed to delete expense: ${it.message}")
            }
    }

    private fun showDeleteConfirmation(expense: Expense) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Yes") { _, _ -> deleteExpense(expense) }
            .setNegativeButton("No", null)
            .create()
        dialog.show()
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
