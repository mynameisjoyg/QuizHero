package com.joyg.quizhero

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds

import com.google.android.gms.ads.AdView
import kotlin.math.roundToInt

private lateinit var adView: AdView
class AccountActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        val name = intent.getStringExtra("name") ?: "未登入"
        val totalCorrectCount = intent.getIntExtra("totalCorrectCount", intent.getLongExtra("totalCorrectCount", 0L).toInt())
        val totalWrongCount = intent.getIntExtra("totalWrongCount", intent.getLongExtra("totalWrongCount", 0L).toInt())

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvTotalCorrectCount = findViewById<TextView>(R.id.tvTotalCorrectCount)
        val tvTotalWrongCount = findViewById<TextView>(R.id.tvTotalWrongCount)
        val tvPercent = findViewById<TextView>(R.id.tvPercent)

        tvUserName.text = "使用者名稱: $name"
        tvTotalCorrectCount.text = "總答對題數: $totalCorrectCount"
        tvTotalWrongCount.text = "總答錯題數: $totalWrongCount"
        val total = totalCorrectCount + totalWrongCount
        val totalPercent = if (total > 0) {
            // 1. 先把其中一個轉成 Float 或 Double 避免整數除法
            // 2. 乘以 100 算出百分比浮點數
            // 3. 用 roundToInt() 四捨五入成整數
            ((totalCorrectCount.toFloat() / total) * 100).roundToInt()
        } else {
            0 // 避免分母為 0 的防呆機制
        }
        // 最終顯示的字串
        val percentString = "$totalPercent%"
        tvPercent.text = "正確率：$totalPercent %"


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
