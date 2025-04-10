package com.example.expenseclassifierapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Expense(
    val amount: Double = 0.0,
    val category: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    @DocumentId val documentId: String = "",
    val merchant: String = "",
    val description: String = ""
)
