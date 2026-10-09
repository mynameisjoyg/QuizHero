package com.joyg.quizhero

import android.os.SystemClock
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountActivityNavigationTest {
    // 啟動 MainActivity
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // 初始化 Intent 監控
        Intents.init()
    }

    @After
    fun tearDown() {
        // 測試結束後釋放 Intent 資源
        Intents.release()
    }
    @Test
    fun testNavigateToAccountActivity() {
        val totalAttempts = 5

        for (i in 1..totalAttempts) {
            // 1. 模擬使用者點擊使用者資訊的按鈕
            onView(withId(R.id.tv_facebook_user_name)).perform(click())

            // 3. 驗證系統是否有發出前往 AccountActivity 的 Intent
            intended(hasComponent(AccountActivity::class.java.name))

            if (i < totalAttempts) {
                // 等待畫面載入
                SystemClock.sleep(3000)

                // 返回 MainActivity
                Espresso.pressBack()

                // 處理可能出現的返回確認對話框
                try {
                    onView(withId(android.R.id.button2)).perform(click())
                } catch (_: Exception) {
                    // 若沒有對話框則忽略
                }

                // 清除 Intent 記錄，讓下一次迴圈重新計算
                Intents.release()
                Intents.init()
            }
        }
    }
}