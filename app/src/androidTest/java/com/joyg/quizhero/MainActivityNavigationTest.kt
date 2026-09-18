package com.joyg.quizhero // 請改成你專案實際的 package 名稱

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
        // 1. 模擬使用者點擊出發跳轉的按鈕 (請將 R.id.btn_start_quiz 替換成你 MainActivity 裡的按鈕 ID)
        onView(withId(R.id.bt_exam)).perform(click())

        // 2. 驗證系統是否有發出前往 QuizActivity 的 Intent
        intended(hasComponent(QuizActivity::class.java.name))
    }
}
