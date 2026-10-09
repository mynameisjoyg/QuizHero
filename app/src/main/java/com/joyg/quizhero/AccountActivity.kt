package com.joyg.quizhero

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds

import com.google.android.gms.ads.AdView
private lateinit var adView: AdView
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


        adView = findViewById<AdView>(R.id.adView)

        // 3. Banner廣告
        // 初始化 Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        // 綁定並載入廣告
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}
