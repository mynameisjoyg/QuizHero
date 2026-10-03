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

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserId = findViewById<TextView>(R.id.tvUserId)

        tvUserName.text = "使用者名稱: $name"
        tvUserId.text = "User ID: $userId"
    }
}
