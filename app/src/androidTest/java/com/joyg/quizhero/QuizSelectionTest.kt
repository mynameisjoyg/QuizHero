package com.joyg.quizhero // 請改成你專案實際的 package 名稱

import android.os.SystemClock
import android.util.Log
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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
        testSpinnerBySubjectVolumeChapter(0,1, 1)
        testSpinnerBySubjectVolumeChapter(1,1, 1)
        testSpinnerBySubjectVolumeChapter(2,1, 1)
    }

    private fun testSpinnerBySubjectVolumeChapter(sub: Int, vol: Int, cha: Int){
        // 定義你想點擊的科目卡片位置 (例如：0 是英文, 1 是國文, 2 是地理)
        val targetPosition = sub

        // 1. 點擊 RecyclerView 中指定位置 (position) 的卡片
        onView(withId(R.id.recyclerView))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    targetPosition,
                    click()
                )
            )

        // 4. 點擊開始測驗按鈕 (請替換為你的按鈕 ID，例如 btn_start_quiz)
        onView(withId(R.id.bt_exam)).perform(click())

        // 5. 驗證：確認有發出跳轉至 QuizActivity 的 Intent 且沒有閃退
        intended(hasComponent(QuizActivity::class.java.name))

        // 等待 5 秒
        SystemClock.sleep(3000)
        onView(withId(R.id.tvQuestionTitle)).check(matches(isDisplayed()))

        // 6. 重要：返回 MainActivity，以便進行下一個迴圈測試
        Espresso.pressBack()

        // 等待對話框彈出並點擊「不儲存/取消」按鈕 (android.R.id.button2)
        onView(withId(android.R.id.button2)).perform(click())

        // 清除過往的 Intent 紀錄，讓下一次迴圈重新計算
        Intents.release()
        Intents.init()
    }
}