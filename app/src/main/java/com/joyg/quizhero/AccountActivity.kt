package com.joyg.quizhero

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class AccountActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        val userId = intent.getStringExtra("userId") ?: "未登入"
        val name = intent.getStringExtra("name") ?: "未登入"
        val totalCorrectCount = intent.getIntExtra("totalCorrectCount", intent.getLongExtra("totalCorrectCount", 0L).toInt())
        val totalWrongCount = intent.getIntExtra("totalWrongCount", intent.getLongExtra("totalWrongCount", 0L).toInt())

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserId = findViewById<TextView>(R.id.tvUserId)
        val tvTotalCorrectCount = findViewById<TextView>(R.id.tvTotalCorrectCount)
        val tvTotalWrongCount = findViewById<TextView>(R.id.tvTotalWrongCount)

        tvUserName.text = "使用者名稱: $name"
        tvUserId.text = "User ID: $userId"
        tvTotalCorrectCount.text = "總答對題數: $totalCorrectCount"
        tvTotalWrongCount.text = "總答錯題數: $totalWrongCount"
    }
}
