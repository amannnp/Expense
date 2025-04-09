import com.google.firebase.Timestamp

data class Expense(
    val amount: Double = 0.0,
    val category: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
