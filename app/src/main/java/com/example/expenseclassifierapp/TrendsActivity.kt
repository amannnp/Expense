package com.example.expenseclassifierapp
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseclassifierapp.databinding.ActivityTrendsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TrendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrendsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        pieChart = binding.pieChart
        barChart = binding.barChart
        userId = FirebaseAuth.getInstance().currentUser?.uid

        fetchAndDisplayTrends()
    }

    private fun fetchAndDisplayTrends() {
        if (userId == null) return

        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { doc ->
                    val category = doc.getString("category")
                    val amount = doc.getDouble("amount")
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()

                    if (category != null && amount != null && timestamp != null)
                        Triple(category, amount, timestamp)
                    else null
                }

                if (expenses.isEmpty()) {
                    pieChart.clear()
                    barChart.clear()
                    pieChart.setNoDataText("No chart data available")
                    barChart.setNoDataText("No chart data available")

                    binding.noDataText.visibility = View.VISIBLE
                    pieChart.visibility = View.GONE
                    barChart.visibility = View.GONE
                } else {
                    binding.noDataText.visibility = View.GONE
                    pieChart.visibility = View.VISIBLE
                    barChart.visibility = View.VISIBLE

                    showPieChart(expenses)
                    showBarChart(expenses)
                }
            }
            .addOnFailureListener {
                binding.noDataText.text = "Failed to load data"
                binding.noDataText.visibility = View.VISIBLE
            }
    }

    private fun showPieChart(expenses: List<Triple<String, Double, Date>>) {
        val categoryTotals = mutableMapOf<String, Double>()

        for ((category, amount, _) in expenses) {
            categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
        }

        val pieEntries = categoryTotals.map { PieEntry(it.value.toFloat(), it.key) }

        val pieDataSet = PieDataSet(pieEntries, "Spending by Category")
        pieDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        val pieData = PieData(pieDataSet)
        pieChart.data = pieData
        pieChart.description = Description().apply { text = "" }
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun showBarChart(expenses: List<Triple<String, Double, Date>>) {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val grouped = expenses.groupBy { sdf.format(it.third) }

        val entries = grouped.entries.mapIndexed { index, entry ->
            val total = entry.value.sumOf { it.second }
            BarEntry(index.toFloat(), total.toFloat())
        }

        val labels = grouped.keys.toList()
        val dataSet = BarDataSet(entries, "Spending over Time")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        val barData = BarData(dataSet)

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.isGranularityEnabled = true
        barChart.description = Description().apply { text = "" }
        barChart.animateY(1000)
        barChart.invalidate()
    }
}
