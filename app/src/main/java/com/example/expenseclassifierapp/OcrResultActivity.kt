package com.example.expenseclassifierapp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream

class OcrResultActivity : AppCompatActivity() {

    private lateinit var rawTextView: TextView
    private lateinit var merchantEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_result)

        // Initialize the text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Initialize UI components
        rawTextView = findViewById(R.id.ocrRawText)
        merchantEditText = findViewById(R.id.merchantEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        amountEditText = findViewById(R.id.amountEditText)
        saveButton = findViewById(R.id.saveOcrExpenseButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("image_uri")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        // Process the image if available
        if (imageUri != null) {
            runTextRecognition(imageUri)
        } else {
            rawTextView.text = "No image provided"
        }

        // Set up save button click listener
        saveButton.setOnClickListener {
            saveExpense()
        }

        // Set up cancel button click listener
        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveExpense() {
        // Validate inputs
        val merchant = merchantEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val amountText = amountEditText.text.toString().trim()

        // Basic validation
        if (merchant.isEmpty()) {
            merchantEditText.error = "Merchant name is required"
            return
        }

        if (amountText.isEmpty()) {
            amountEditText.error = "Amount is required"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            amountEditText.error = "Invalid amount format"
            return
        }

        // Check if user is logged in
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to save expenses", Toast.LENGTH_LONG).show()
            return
        }

        // Disable save button to prevent multiple submissions
        saveButton.isEnabled = false

        // Create expense data
        val expense = hashMapOf(
            "merchant" to merchant,
            "description" to description,
            "amount" to amount,
            "timestamp" to System.currentTimeMillis(),
            "userId" to userId,
            "category" to detectCategory(merchant, description)
        )

        // Save to Firestore
        firestore.collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save expense: ${e.message}", Toast.LENGTH_LONG).show()
                saveButton.isEnabled = true
            }
    }

    private fun runTextRecognition(imageUri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            inputStream?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val extractedText = visionText.text
                            rawTextView.text = extractedText
                            autofillFields(extractedText)
                        }
                        .addOnFailureListener { e ->
                            rawTextView.text = "Failed to recognize text: ${e.message}"
                        }
                } else {
                    rawTextView.text = "Unable to decode image."
                }
            } ?: run {
                rawTextView.text = "Invalid image stream."
            }
        } catch (e: Exception) {
            rawTextView.text = "Error reading image: ${e.message}"
        }
    }

    private fun autofillFields(text: String) {
        // Extract amount using regex
        val amountRegex = Regex("""(?:Rs\.?\s?|₹\s?)?(\d+(?:[.,]\d{1,2})?)""")
        val match = amountRegex.find(text)
        match?.let {
            amountEditText.setText(it.groupValues[1])
        }

        // Extract merchant name and description from text
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isNotEmpty()) {
            merchantEditText.setText(lines[0])
        }
        if (lines.size > 1) {
            descriptionEditText.setText(lines.drop(1).joinToString(" "))
        }
    }

    /**
     * Simple category detection based on merchant name and description
     * This could be enhanced with ML models in the future
     */
    private fun detectCategory(merchant: String, description: String): String {
        val text = "$merchant $description".lowercase()

        return when {
            text.contains("restaurant") || text.contains("cafe") ||
                    text.contains("food") || text.contains("dinner") ||
                    text.contains("lunch") || text.contains("breakfast") -> "Food & Dining"

            text.contains("grocery") || text.contains("market") ||
                    text.contains("supermarket") -> "Groceries"

            text.contains("uber") || text.contains("ola") ||
                    text.contains("lyft") || text.contains("taxi") ||
                    text.contains("metro") || text.contains("train") ||
                    text.contains("bus") -> "Transportation"

            text.contains("movie") || text.contains("cinema") ||
                    text.contains("theatre") || text.contains("concert") -> "Entertainment"

            text.contains("amazon") || text.contains("flipkart") ||
                    text.contains("myntra") || text.contains("shopping") -> "Shopping"

            text.contains("bill") || text.contains("utility") ||
                    text.contains("electricity") || text.contains("water") ||
                    text.contains("internet") || text.contains("phone") -> "Bills & Utilities"

            else -> "Miscellaneous"
        }
    }

    companion object {
        fun newIntent(context: Context, imageUri: Uri): Intent {
            return Intent(context, OcrResultActivity::class.java).apply {
                putExtra("image_uri", imageUri.toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the text recognizer when activity is destroyed
        textRecognizer.close()
    }
}