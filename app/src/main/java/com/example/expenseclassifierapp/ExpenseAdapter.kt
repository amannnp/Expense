package com.example.expenseclassifierapp
import com.example.expenseclassifierapp.Expense
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val expenses: MutableList<Expense>,
    private val onDeleteConfirmed: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val expense = expenses[position]
                    AlertDialog.Builder(itemView.context)
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Yes") { _, _ ->
                            onDeleteConfirmed(expense)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.amountTextView.text = "₹${expense.amount}"
        holder.categoryTextView.text = expense.category

        val timestampText = expense.timestamp.toDate().let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
        }

        holder.timestampTextView.text = timestampText
    }

    override fun getItemCount(): Int = expenses.size

    fun removeExpense(expense: Expense) {
        val index = expenses.indexOfFirst { it.documentId == expense.documentId }
        if (index != -1) {
            expenses.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addExpense(expense: Expense) {
        expenses.add(0, expense)
        notifyItemInserted(0)
    }
}
