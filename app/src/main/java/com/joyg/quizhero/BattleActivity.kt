package com.joyg.quizhero

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class BattleActivity : AppCompatActivity() {

    private lateinit var stompClient: QuizStompClient
    //private val myPlayerId = "Android_Player_01" // 可以是從 Firebase Auth 取得的玩家 ID
    // ✅ 每次進入 Activity 都會生成獨一無二的 ID，例如：Android_a829bb
    //private val myPlayerId = "Android_" + UUID.randomUUID().toString().substring(0, 6)
    private lateinit var myPlayerId : String
    private var currentRoomId: String? = null
    private var currentQuestionId: String? = null

    // UI 元件
    private lateinit var tvStatus: TextView
    private lateinit var tvQuestionTitle: TextView
    private lateinit var btnMatch: Button
    private lateinit var btnOptionA: Button
    private lateinit var btnOptionB: Button
    private lateinit var btnOptionC: Button
    private lateinit var btnOptionD: Button

    //Subject, volume, chapter
    private lateinit var subject : String
    private lateinit var volume : String
    private lateinit var chapter : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        tvStatus = findViewById(R.id.tvStatus)
        tvQuestionTitle = findViewById(R.id.tvQuestionTitle)
        btnMatch = findViewById(R.id.btnMatch)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)

        //接收從 MainActivity 傳過來的字串，若沒有傳值則設定預設值
        myPlayerId = intent.getStringExtra("userId") ?: "未選擇"
        subject = intent.getStringExtra("subject") ?: "未選擇"
        volume = intent.getStringExtra("volume") ?: "未選擇"
        chapter = intent.getStringExtra("chapter") ?: "未選擇"
        Log.v("BattleActivity", "JOYGSAY: getStringExtra, userId=$myPlayerId, subject=$subject, volume=$volume, chapter=$chapter")


        stompClient = QuizStompClient()

        // 步驟 1: 建立 WebSocket 連線
        tvStatus.text = "連線中..."
        stompClient.connect(
            playerId = myPlayerId,
            onConnected = {
                runOnUiThread {
                    tvStatus.text = "連線成功！請點擊配對"
                    btnMatch.isEnabled = true
                }
            },
            onMatched = { matchData ->
                currentRoomId = matchData.roomId
                runOnUiThread {
                    tvStatus.text = "🎉 配對成功！房間: ${matchData.roomId.take(8)}\n等待發題中..."
                    btnMatch.visibility = View.GONE
                }
            },
            onQuizReceived = { quiz ->
                currentQuestionId = quiz.questionId
                runOnUiThread {
                    tvStatus.text = "❓ 題目來了！請搶答！"
                    tvQuestionTitle.text = quiz.title

                    // 顯示選項並啟用搶答按鈕
                    btnOptionA.text = quiz.options.getOrNull(0) ?: ""
                    btnOptionB.text = quiz.options.getOrNull(1) ?: ""
                    btnOptionC.text = quiz.options.getOrNull(2) ?: ""
                    btnOptionD.text = quiz.options.getOrNull(3) ?: ""
                    setAnswerButtonsEnabled(true)
                }
            },
            onResultReceived = { result ->
                runOnUiThread {
                    setAnswerButtonsEnabled(false) // 搶答結束，鎖定按鈕
                    if (result.winnerId == myPlayerId && result.isCorrect) {
                        tvStatus.text = "🏆 恭喜你搶答成功！"
                    } else if (result.winnerId == myPlayerId && !result.isCorrect){
                        tvStatus.text = "❌ 答錯了！"
                    } else if (result.winnerId != myPlayerId){
                        tvStatus.text = "❌ 太慢了！對手已先搶答！"
                    }
                }
            }
        )

        // 步驟 2: 按下配對按鈕
        btnMatch.setOnClickListener {
            stompClient.sendMatchRequest(myPlayerId, subject, volume, chapter)
            tvStatus.text = "正在尋找對手..."
            btnMatch.isEnabled = false
        }

        // 步驟 3: 按下搶答按鈕
        val answerClickListener = View.OnClickListener { view ->
            val selectedOption = (view as Button).text.toString()
            val roomId = currentRoomId
            val qId = currentQuestionId

            if (roomId != null && qId != null) {
                // 送出答案並立刻停用按鈕，防止重複點擊
                Log.v("JOYG", "JOYG: roomId=${roomId}")
                stompClient.sendAnswer(roomId, qId, selectedOption)
                setAnswerButtonsEnabled(false)
                tvStatus.text = "已搶答，等待後端判定時間..."
            }
        }

        btnOptionA.setOnClickListener(answerClickListener)
        btnOptionB.setOnClickListener(answerClickListener)
        btnOptionC.setOnClickListener(answerClickListener)
        btnOptionD.setOnClickListener(answerClickListener)
    }

    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        btnOptionA.isEnabled = enabled
        btnOptionB.isEnabled = enabled
        btnOptionC.isEnabled = enabled
        btnOptionD.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient.disconnect() // 離開頁面時切斷連線
    }
}