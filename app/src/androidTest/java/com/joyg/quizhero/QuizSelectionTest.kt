package com.joyg.quizhero // 請改成你專案實際的 package 名稱

import android.os.SystemClock
import android.util.Log
import android.widget.ArrayAdapter
import androidx.test.espresso.Espresso
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
        //Test English, volume1, chapter 1~6.
        for(chapter in 0 until 6){
            testSpinnerBySubjectVolumeChapter(0,0,chapter)
        }

        //Test English, volume2, chapter 1~4.
        for(chapter in 0 until 4){
            testSpinnerBySubjectVolumeChapter(0,1,chapter)
        }

        //Test English, volume3~5, chapter 1~6.
        for(volume in 2 until 5){
            for(chapter in 0 until 6){
                testSpinnerBySubjectVolumeChapter(0,volume, chapter)
            }
        }
    }

    private fun testSpinnerBySubjectVolumeChapter(sub: Int, vol: Int, cha: Int){
        // 1. 選擇 Subject (科目 Spinner)
        onView(withId(R.id.sp_subject)).perform(click())
        // 假設 Spinner 內容是 String，這裡模擬點擊第二個選項（或指定字串）
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(sub).perform(click())

        // 2. 選擇 Volume (冊別 Spinner)
        onView(withId(R.id.sp_volume)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(vol).perform(click())

        // 3. 選擇 Chapter (章節 Spinner)
        onView(withId(R.id.sp_chapter)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)))).atPosition(cha).perform(click())

        // 4. 點擊開始測驗按鈕 (請替換為你的按鈕 ID，例如 btn_start_quiz)
        onView(withId(R.id.bt_exam)).perform(click())

        // 5. 驗證：確認有發出跳轉至 QuizActivity 的 Intent 且沒有閃退
        intended(hasComponent(QuizActivity::class.java.name))

        // 等待 5 秒
        //SystemClock.sleep(3000)
        onView(withId(R.id.tv_question)).check(matches(isDisplayed()))

        // 6. 重要：返回 MainActivity，以便進行下一個迴圈測試
        Espresso.pressBack()

        // 清除過往的 Intent 紀錄，讓下一次迴圈重新計算
        Intents.release()
        Intents.init()
    }
}