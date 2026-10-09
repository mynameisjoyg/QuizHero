package com.joyg.quizhero

import android.os.SystemClock
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class QuizActivityNavigationTest {
    // 啟動 QuizActivity
    @get:Rule
    val activityRule = ActivityScenarioRule(QuizActivity::class.java)

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
    fun testQuizActivityNavigation() {
        // 等待畫面載入
        SystemClock.sleep(2000)

        for(i in 0 until 2) {
            // 2. 模擬使用者點擊出發跳轉的按鈕
            onView(withId(R.id.btnOptionA)).perform(click())
            // 等下一題
            SystemClock.sleep(5000)
        }

        // 等待廣告
        SystemClock.sleep(10*1000)
    }
}