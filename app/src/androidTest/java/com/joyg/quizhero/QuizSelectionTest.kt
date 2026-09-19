package com.joyg.quizhero // 請改成你專案實際的 package 名稱

import android.util.Log
import android.widget.ArrayAdapter
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private lateinit var db: FirebaseFirestore

@RunWith(AndroidJUnit4::class)
class QuizSelectionTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testSelectSpinnerItemsAndStartQuiz_DoesNotCrash() {

        // 1. 選擇 Subject (科目 Spinner)
        onView(withId(R.id.sp_subject)).perform(click())
        // 假設 Spinner 內容是 String，這裡模擬點擊第二個選項（或指定字串）
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(0).perform(click())

        // 2. 選擇 Volume (冊別 Spinner)
        onView(withId(R.id.sp_volume)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(0).perform(click())

        // 3. 選擇 Chapter (章節 Spinner)
        onView(withId(R.id.sp_chapter)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(0).perform(click())

        // 4. 點擊開始測驗按鈕 (請替換為你的按鈕 ID，例如 btn_start_quiz)
        onView(withId(R.id.bt_exam)).perform(click())

        // 5. 驗證：確認有發出跳轉至 QuizActivity 的 Intent 且沒有閃退
        intended(hasComponent(QuizActivity::class.java.name))
    }
}