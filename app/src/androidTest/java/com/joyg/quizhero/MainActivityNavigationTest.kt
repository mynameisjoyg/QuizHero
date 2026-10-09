package com.joyg.quizhero

import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
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
class MainActivityNavigationTest {

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
    fun testNavigateToQuizActivity() {
        val totalAttempts = 5

        for (i in 1..totalAttempts) {
            // 等待 5 秒
            SystemClock.sleep(3000)

            // 定義你想點擊的科目卡片位置 (例如：0 是英文, 1 是國文, 2 是地理)
            val targetPosition = (i - 1) % 3

            // 1. 點擊 RecyclerView 中指定位置 (position) 的卡片
            onView(withId(R.id.recyclerView))
                .perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                        targetPosition,
                        click()
                    )
                )

            // 2. 模擬使用者點擊出發跳轉的按鈕
            onView(withId(R.id.bt_exam)).perform(click())

            // 3. 驗證系統是否有發出前往 QuizActivity 的 Intent
            intended(hasComponent(QuizActivity::class.java.name))

            if (i < totalAttempts) {
                // 等待畫面載入
                SystemClock.sleep(2000)

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
