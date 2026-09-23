package com.joyg.quizhero

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BattleActivity : AppCompatActivity() {

    private lateinit var stompClient: QuizStompClient
    private val myPlayerId = "Android_Player_01" // 可以是從 Firebase Auth 取得的玩家 ID
    private var currentRoomId: String? = null
    private var currentQuestionId: String? = null

    // UI 元件
    private lateinit var tvStatus: TextView
    private lateinit var tvQuestionTitle: TextView
    private lateinit var btnMatch: Button
    private lateinit var btnOptionA: Button
    private lateinit var btnOptionB: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        tvStatus = findViewById(R.id.tvStatus)
        tvQuestionTitle = findViewById(R.id.tvQuestionTitle)
        btnMatch = findViewById(R.id.btnMatch)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)

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
                    setAnswerButtonsEnabled(true)
                }
            },
            onResultReceived = { result ->
                runOnUiThread {
                    setAnswerButtonsEnabled(false) // 搶答結束，鎖定按鈕
                    if (result.winnerId == myPlayerId) {
                        tvStatus.text = "🏆 恭喜你搶答成功！\n反應時間: ${result.reactionTimeMs} ms"
                    } else {
                        tvStatus.text = "❌ 太慢了！對手 ${result.winnerId} 贏得了這題"
                    }
                }
            }
        )

        // 步驟 2: 按下配對按鈕
        btnMatch.setOnClickListener {
            stompClient.sendMatchRequest(myPlayerId)
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
                stompClient.sendAnswer(roomId, qId, selectedOption)
                setAnswerButtonsEnabled(false)
                tvStatus.text = "已搶答，等待後端判定時間..."
            }
        }

        btnOptionA.setOnClickListener(answerClickListener)
        btnOptionB.setOnClickListener(answerClickListener)
    }

    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        btnOptionA.isEnabled = enabled
        btnOptionB.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient.disconnect() // 離開頁面時切斷連線
    }
}