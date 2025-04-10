package com.example.expenseclassifierapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BudgetAdapter(
    private val categoryTotals: Map<String, Double>,
    private val limits: Map<String, Double>
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = categoryTotals.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = categoryTotals.entries.toList()[position]
        val category = entry.key
        val spent = entry.value
        val limit = limits[category] ?: 0.0

        holder.categoryText.text = category
        holder.progressText.text = "₹${spent.toInt()} / ₹${limit.toInt()}"
        holder.progressBar.max = limit.toInt()
        holder.progressBar.progress = spent.toInt().coerceAtMost(limit.toInt())
    }
}
