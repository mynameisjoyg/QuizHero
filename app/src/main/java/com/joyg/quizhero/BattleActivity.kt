package com.joyg.quizhero

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.joyg.quizhero.databinding.ActivityBattleBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BattleActivity : AppCompatActivity() {

    private lateinit var stompClient: QuizStompClient
    //private val myPlayerId = "Android_Player_01" // 可以是從 Firebase Auth 取得的玩家 ID
    // ✅ 每次進入 Activity 都會生成獨一無二的 ID，例如：Android_a829bb
    //private val myPlayerId = "Android_" + UUID.randomUUID().toString().substring(0, 6)
    private lateinit var userId : String
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

    //Score.
    private var total: Int  = 0
    private var correct: Int  = 0
    private var wrong: Int =0
    private var percent: Double =0.0
    private lateinit var time_start: String
    private lateinit var time_end: String

    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    // 2. 宣告 binding 變數
    private lateinit var binding: ActivityBattleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        // 3. 初始化 binding (將 layout XML 膨脹/載入成視圖物件)
        binding = ActivityBattleBinding.inflate(layoutInflater)

        // 4. 設定內容視圖為 binding.root (代替原本的 R.layout.activity_battle)
        setContentView(binding.root)

        // 5. 這時候就可以順利使用 binding.root 了！
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // 為底部的選項區塊加上導覽列高度 Padding，避免被切掉
            binding.layoutAnswers.setPadding(0, 0, 0, navigationBars.bottom)
            insets
        }


        //記錄開始作答時間
        time_start = getCurrentTimeString()

        total =0
        correct =0
        wrong =0
        percent =0.0

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore


        tvStatus = findViewById(R.id.tvStatus)
        tvQuestionTitle = findViewById(R.id.tvQuestionTitle)
        btnMatch = findViewById(R.id.btnMatch)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)

        //接收從 MainActivity 傳過來的字串，若沒有傳值則設定預設值
        userId = intent.getStringExtra("userId") ?: "未選擇"
        subject = intent.getStringExtra("subject") ?: "未選擇"
        volume = intent.getStringExtra("volume") ?: "未選擇"
        chapter = intent.getStringExtra("chapter") ?: "未選擇"
        Log.v("BattleActivity", "JOYGSAY: getStringExtra, userId=$userId, subject=$subject, volume=$volume, chapter=$chapter")


        stompClient = QuizStompClient()

        // 步驟 1: 建立 WebSocket 連線
        tvStatus.text = "連線中..."
        stompClient.connect(
            playerId = userId,
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
                // 配對成功後呼叫 join 告知後端綁定 Session
                stompClient.sendJoinRoom(matchData.roomId)
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
                    if (result.winnerId == userId && result.isCorrect) {
                        tvStatus.text = "🏆 恭喜你搶答成功！"
                        correct = correct +1;
                    } else if (result.winnerId == userId && !result.isCorrect){
                        tvStatus.text = "❌ 答錯了！"
                    } else if (result.winnerId != userId){
                        tvStatus.text = "❌ 太慢了！對手已先搶答！"
                    }
                }
            },
            onPlayerLeft = { leftPlayerId ->
                Log.v("JOYG", "JOYG: onPlayerLeft, leftPlayerId=$leftPlayerId, userId=$userId")
                // 如果離開的人不是自己，代表對方離線/退出了
                if (leftPlayerId != userId) {
                    runOnUiThread {
                        Toast.makeText(this, "對手已離開對戰，你獲得了勝利！", Toast.LENGTH_LONG).show()
                        finish() // 關閉 Activity 回主畫面
                    }
                }
            }
        )

        // 步驟 2: 按下配對按鈕
        btnMatch.setOnClickListener {
            stompClient.sendMatchRequest(userId, subject, volume, chapter)
            tvStatus.text = "正在尋找對手..."
            btnMatch.isEnabled = false
        }

        // 步驟 3: 按下搶答按鈕
        val answerClickListener = View.OnClickListener { view ->
            val selectedOption = (view as Button).text.toString()
            val roomId = currentRoomId
            val qId = currentQuestionId

            // 匹配括號中的單一英文字母
            val regex = Regex("""\(([A-Za-z])\)""")
            val selectedAnswer = regex.find(selectedOption)?.groupValues?.get(1) ?: ""

            if (roomId != null && qId != null) {
                // 送出答案並立刻停用按鈕，防止重複點擊
                Log.v("JOYG", "JOYG: roomId=${roomId}, selectedAnswer=${selectedAnswer}")
                stompClient.sendAnswer(roomId, qId, selectedAnswer)
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

    private fun saveScoreToFirestore(){
        //記錄結束時間
        time_end = getCurrentTimeString()

        // 生成一個像是 "550e8400-e29b-41d4-a716-446655440000" 的獨一無二字串
        val uniqueId: String = UUID.randomUUID().toString()

        Log.d("FirestoreDemo", "JOYGSAY: Call saveScoreToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val score = hashMapOf(
            "id" to uniqueId,
            "user_id" to userId,
            "subject" to subject,
            "volume" to volume,
            "chapter" to chapter,
            "correct" to correct,
            "wrong" to wrong,
            "time_start" to time_start,
            "time_end" to time_end,
        )

        // 4. 指定集合名稱 "Score"，並自動產生文件 ID 新增資料 (.add)
        db.collection("Score")
            .add(score)
            .addOnSuccessListener { documentReference ->
                // 新增成功時的回呼
                Log.d("FirestoreDemo", "JOYGSAY: 記錄新增成功.")
                //Toast.makeText(this, "新增成功！ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 新增失敗時的回呼
                Log.w("FirestoreDemo", "JOYGSAY: 新增資料時發生錯誤", e)
                //Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    fun getCurrentTimeString(): String {
        // 1. 取得系統當前時間
        val current = LocalDateTime.now()

        // 2. 定義時間格式（例如：2026-09-20 11:09:52）
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // 3. 轉成字串傳回
        return current.format(formatter)
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient.disconnect() // 離開頁面時切斷連線
        saveScoreToFirestore()
    }
}