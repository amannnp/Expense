package com.example.expenseclassifierapp

import java.io.File
import androidx.core.content.FileProvider

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var amountInput: EditText
    private lateinit var merchantInput: MaterialAutoCompleteTextView
    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var predictButton: Button
    private lateinit var saveButton: Button
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter

    private lateinit var classifier: Classifier
    private lateinit var auth: FirebaseAuth

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private val expenses = mutableListOf<Expense>()
    private lateinit var categories: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

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

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val toDelete = expenses[pos]

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setPositiveButton("Yes") { _, _ ->
                        FirebaseFirestore.getInstance().collection("expenses")
                            .document(toDelete.documentId)
                            .delete()
                            .addOnSuccessListener {
                                expenses.removeAt(pos)
                                adapter.notifyItemRemoved(pos)
                                showToast("Expense deleted")
                            }
                            .addOnFailureListener {
                                showToast("Failed to delete: ${it.message}")
                                adapter.notifyItemChanged(pos)
                            }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(pos)
                    }
                    .setCancelable(false)
                    .show()
            }
        })
        itemTouchHelper.attachToRecyclerView(expenseRecyclerView)

        classifier = Classifier(this)
        categories = classifier.getLabels()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.setSelection(categories.indexOf("Others").takeIf { it >= 0 } ?: 0)

        setupMerchantDropdown()

        predictButton.setOnClickListener {
            hideKeyboard()
            handlePrediction()
        }

        saveButton.setOnClickListener {
            hideKeyboard()
            handleSave()
        }

        loadExpenses()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_trends -> startActivity(Intent(this, TrendsActivity::class.java))
            R.id.nav_export -> exportExpensesToCSV()
            R.id.nav_about -> showAboutDialog()
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawers()
        return true
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
                    val timestamp = (doc["timestamp"] as? Timestamp) ?: Timestamp.now()
                    val id = doc.id
                    expenses.add(0, Expense(amount, category, timestamp, id))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e -> showToast("Failed to load: ${e.message}") }
    }

    private fun handlePrediction() {
        val amount = amountInput.text.toString().toFloatOrNull()
        val merchant = merchantInput.text.toString().trim()
        val desc = descriptionInput.text.toString().trim()

        if (amount == null) return showToast("Enter valid amount")
        if (merchant.isBlank()) return showToast("Enter merchant")

        val predicted = classifier.classify("$merchant $desc", amount)
        categorySpinner.setSelection(categories.indexOf(predicted).takeIf { it >= 0 } ?: 0)
        showToast("Predicted: $predicted")
    }

    private fun handleSave() {
        val amount = amountInput.text.toString().toDoubleOrNull()
        val merchant = merchantInput.text.toString().trim()
        val desc = descriptionInput.text.toString().trim()
        val category = categorySpinner.selectedItem?.toString() ?: "Other"

        if (amount == null) return showToast("Enter a valid amount")
        if (merchant.isEmpty()) return showToast("Merchant required")

        val userId = auth.currentUser?.uid ?: return showToast("Not signed in")
        val data = hashMapOf(
            "merchant" to merchant,
            "description" to desc,
            "amount" to amount,
            "category" to category,
            "timestamp" to Timestamp.now(),
            "userId" to userId
        )

        FirebaseFirestore.getInstance().collection("expenses")
            .add(data)
            .addOnSuccessListener {
                expenses.add(0, Expense(amount, category, Timestamp.now(), it.id))
                adapter.notifyItemInserted(0)
                expenseRecyclerView.scrollToPosition(0)
                clearInputs()
                showToast("Saved successfully")
            }
            .addOnFailureListener { showToast("Save failed: ${it.message}") }
    }

    private fun setupMerchantDropdown() {
        val merchants = loadMerchantsFromAssets()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, merchants)
        merchantInput.setAdapter(adapter)
        merchantInput.threshold = 1
        merchantInput.setOnFocusChangeListener { _, focus -> if (focus) merchantInput.showDropDown() }
    }

    private fun loadMerchantsFromAssets(): List<String> {
        return try {
            val json = assets.open("merchants.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e("MerchantLoad", "Error: ${e.message}")
            emptyList()
        }
    }

    private fun clearInputs() {
        amountInput.setText("")
        merchantInput.setText("")
        descriptionInput.setText("")
        categorySpinner.setSelection(0)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun exportExpensesToCSV() {
        val userId = auth.currentUser?.uid ?: return showToast("Not signed in")
        if (expenses.isEmpty()) return showToast("No expenses to export")

        val content = buildString {
            append("amount,category,timestamp\n")
            expenses.forEach {
                append("${it.amount},${it.category},${it.timestamp.toDate()}\n")
            }
        }

        try {
            val file = File(cacheDir, "expenses_export.csv")
            file.writeText(content)

            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Exported Expenses")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share CSV via"))
        } catch (e: Exception) {
            showToast("Export failed: ${e.message}")
            Log.e("ExportCSV", "Error: ${e.message}", e)
        }
    }

    private fun showAboutDialog() {
        val view = layoutInflater.inflate(R.layout.about_dialog, null)
        AlertDialog.Builder(this)
            .setTitle("About This App")
            .setView(view)
            .setPositiveButton("OK", null)
            .show()
    }
}
