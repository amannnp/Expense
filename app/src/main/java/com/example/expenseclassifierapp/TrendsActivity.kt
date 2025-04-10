package com.example.expenseclassifierapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expenseclassifierapp.databinding.ActivityTrendsBinding

class TrendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Spending Trends"
    }
}
