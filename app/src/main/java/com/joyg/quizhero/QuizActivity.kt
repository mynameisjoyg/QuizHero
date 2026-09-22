package com.joyg.quizhero


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.alibaba.excel.EasyExcel
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import com.facebook.CallbackManager
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import com.facebook.FacebookCallback
import com.facebook.GraphRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private lateinit var firebaseAnalytics: FirebaseAnalytics

private var id: Int = 0
private lateinit var userId: String

private var subject: String = ""
private var volume: String =""
private var chapter: String =""
private var answer: String = ""
private var question: String = ""
private var solution: String = ""
private var image1: String = ""
private var image2: String = ""
private var image3: String = ""
private var image4: String = ""
private var image5: String = ""
private var image6: String = ""
private var total: Int  = 0
private var correct: Int  = 0
private var wrong: Int =0
private var percent: Double =0.0

private lateinit var time_start: String
private lateinit var time_end: String


class QuizActivity : ComponentActivity() {
    private lateinit var tvQuestion: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvCorrect: TextView
    private lateinit var tv_wrong: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvFacebookUserName: TextView
    private lateinit var btSubmit: Button
    private lateinit var btNextQuestion: Button
    private lateinit var btExit: Button
    private lateinit var radioGroup: RadioGroup

    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_activity)

        //取得MainActivity傳送過來的subject
        // 1. 綁定 TextView 元件
        val tvSelectedSubject: TextView = findViewById(R.id.tv_selected_subject)

        // 2. 接收從 MainActivity 傳過來的字串，若沒有傳值則設定預設值
        userId = intent.getStringExtra("userId") ?: "未選擇"
        val subject = intent.getStringExtra("subject") ?: "未選擇"
        val volume = intent.getStringExtra("volume")?:"未選擇"
        val chapter = intent.getStringExtra("chapter")?:"未選擇"
        Log.v("JOYG", "JOYGSAY: getStringExtra, userId=$userId, subject=$subject, volume=$volume, chapter=$chapter")

        // 3. 將取得的資料顯示在 TextView 上
        tvSelectedSubject.text = "科目：${subject}\t\t冊目：${volume}\t\t章節：${chapter}"

        //記錄開始作答時間
        time_start = getCurrentTimeString()

        btSubmit = findViewById<Button>(R.id.bt_submit)
        btNextQuestion = findViewById<Button>(R.id.bt_next_question)
        btExit = findViewById<Button>(R.id.bt_exit)
        radioGroup = findViewById<RadioGroup>(R.id.rg_options)
        tvQuestion = findViewById<TextView>(R.id.tv_question)
        tvTotal = findViewById<TextView>(R.id.tv_total)
        tvCorrect = findViewById<TextView>(R.id.tv_correct)
        tv_wrong = findViewById<TextView>(R.id.tv_wrong)
        tvPercent = findViewById<TextView>(R.id.tv_percent)
        tvFacebookUserName = findViewById<TextView>(R.id.tv_facebook_user_name)

        total =0
        correct =0
        wrong=0
        percent=0.0

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore

        btExit.setOnClickListener {
            showExitDialog()
        }

        btSubmit.setOnClickListener {
            //設定可按下一題以及不可以按提交
            btNextQuestion.isClickable=true
            btSubmit.isClickable=false

            //取得資料庫內正確解答
            // 假設 RadioGroup 的 ID 是 rg_options
            val radioGroup = findViewById<RadioGroup>(R.id.rg_options) // 請確保 RadioGroup 在 XML 有設定 id

            // 1. 取得目前被選中的 RadioButton ID
            val selectedId = radioGroup.checkedRadioButtonId

            // 2. 判斷是否有選擇選項
            if (selectedId != -1) {
                // 依據 ID 判斷選了哪一個
                val selectedAnswer = when (selectedId) {
                    R.id.rb_A -> "A"
                    R.id.rb_B -> "B"
                    R.id.rb_C -> "C"
                    R.id.rb_D -> "D"
                    else -> ""
                }

                if (answer == selectedAnswer) {
                    Toast.makeText(this, "答對了", Toast.LENGTH_SHORT).show()
                    correct = correct+1
                    tvCorrect?.setText("正確數："+correct)

                } else {
                    Toast.makeText(this, "答錯了", Toast.LENGTH_SHORT).show()
                    wrong= wrong+1
                    tv_wrong?.setText("錯誤數："+wrong)
                }
                total= total+1
                tvTotal?.setText("已完成："+total)
                percent = calculatePercentage()
                Log.v("JOYG", "JOYG: percent = "+ percent)
                tvPercent?.setText("正確率："+String.format("%.1f%%", percent))
            } else {
                println("使用者還沒選擇任何選項！")
            }
        }

        btNextQuestion.setOnClickListener {
            queryQuestion(subject, volume, chapter)
        }

        queryQuestion(subject, volume, chapter)
    }

    override fun onStart() {
        super.onStart()

        val currentAccessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = currentAccessToken != null && !currentAccessToken.isExpired

        if (isLoggedIn) {
            // 使用者先前已登入，直接抓取資料顯示
            fetchUserInfoWithGraphApi(currentAccessToken, tvFacebookUserName)
        }
    }

    // 4. 將 Intent 結果傳遞給 Facebook SDK CallbackManager
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun fetchUserInfoWithGraphApi(accessToken: AccessToken, textView: TextView?) {
        // 建立 Graph API 請求，目標為 "me" (目前登入的使用者)
        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, response ->
            if (jsonObject != null) {
                try {
                    // 解析 JSON 回傳內容
                    val name = jsonObject.optString("name", "未知使用者")

                    // UI 異動必須在 Main Thread 執行（GraphRequest 回呼預設已在 UI 線程）
                    textView?.text = "歡迎， $name"

                } catch (e: Exception) {
                    Log.e("FBAuth", "解析使用者資料失敗: ${e.message}")
                }
            }
        }

        // 指定需要獲取的欄位
        val parameters = Bundle().apply {
            putString("fields", "id,name,email")
        }
        request.parameters = parameters

        // 非同步執行請求
        request.executeAsync()
    }

    private fun queryQuestion(sub:String, vol: String, chap: String){
        radioGroup.check(R.id.rb_A)
        Log.v("JOYG", "JOYGSAY: queryQuestion, sub=$sub, vol=$vol, chap=$chap")

        //設定不可按下一題以及可以按提交
        btNextQuestion.isClickable=false
        btSubmit.isClickable=true

        var totalCount = 0
        var randomNumber = 1
        db.collection(sub+"_Quiz")
            .whereEqualTo("volume", vol)
            .whereEqualTo("chapter", chap)
            .get()
            .addOnSuccessListener {
                    querySnapshot ->
                if(querySnapshot.isEmpty) {
                    Log.v("JOYG", "JOYGSAY: in queryQuestion, 找不到${sub}_Quiz")
                    Toast.makeText(this, "找不到${sub}_Quiz", Toast.LENGTH_SHORT).show()
                }
                //取得全部題目個數
                totalCount = querySnapshot.size()
                //取得隨機題目id
                randomNumber = (0..totalCount-1).random()
                Log.v("JOYG","JOYG: totalCount="+totalCount+", randomNumber="+ randomNumber)

                //利用隨機id來取得題目、解答和詳解
                Log.v("JOYG", "JOYG: randomNumber=${randomNumber}")
                db.collection("${sub}_Quiz")
                    .whereEqualTo("volume", vol)
                    .whereEqualTo("chapter", chap)
                    .get()
                    .addOnSuccessListener {
                            querySnapshot ->
                        if(!querySnapshot.isEmpty) {
                            //把題目顯示出來
                            if (!querySnapshot.isEmpty) {
                                // 1. 轉成 Quiz 物件
                                val quiz = querySnapshot.documents[randomNumber].toObject(Quiz::class.java)

                                // 2. 取出 question 欄位並設定給 TextView
                                quiz?.let {
                                    id=it.id
                                    subject=it.subject
                                    volume=it.volume
                                    chapter=it.chapter
                                    answer = it.answer
                                    question=it.question
                                    solution=it.solution
                                    image1=it.image1
                                    image2=it.image2
                                    image3=it.image3
                                    image4=it.image4
                                    image5=it.image5
                                    image6=it.image6

                                    tvQuestion?.text = it.question
                                    tvQuestion.text = tvQuestion.text.replace(Regex("\\(A\\)"), "\n(A)")
                                    tvQuestion.text = tvQuestion.text.replace(Regex("\\(B\\)"), "\n(B)")
                                    tvQuestion.text = tvQuestion.text.replace(Regex("\\(C\\)"), "\n(C)")
                                    tvQuestion.text = tvQuestion.text.replace(Regex("\\(D\\)"), "\n(D)")
                                    hintAnswer(answer)
                                }
                            } else {
                                tvQuestion?.text = "找不到題目"
                            }
                        }
                    }
            }
            .addOnFailureListener { e->
                Log.d("FirestoreDemo", "取得全部題目個數失敗", e)
                Toast.makeText(this, "取得全部題目個數失敗: ${e.message}", Toast.LENGTH_SHORT).show()

            }


    }

    /**
     * 顯示確認離開的 AlertDialog
     */
    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("離開測驗")
            .setMessage("是否要儲存此次的答題記錄？")
            // 按鈕 1：儲存並離開
            .setPositiveButton("儲存並離開") { dialog, _ ->
                saveQuizProgress()
                finish() // 關閉當前 Activity
            }
            // 按鈕 2：不儲存直接離開
            .setNegativeButton("不儲存") { dialog, _ ->
                Toast.makeText(this, "未儲存記錄", Toast.LENGTH_SHORT).show()
                finish() // 關閉當前 Activity
            }
            // 按鈕 3：取消（繼續留在測驗畫面）
            .setNeutralButton("取消") { dialog, _ ->
                dialog.dismiss() // 關閉對話框
            }
            // 防止點擊對話框外部背景隨意關閉（確保使用者明確做出選擇）
            .setCancelable(false)
            .show()
    }

    /**
     * 處理儲存邏輯的地方
     */
    private fun saveQuizProgress() {
        // TODO: 這裡寫寫入資料庫或 Call API 儲存分數/作答記錄的邏輯
        Toast.makeText(this, "記錄已成功儲存！", Toast.LENGTH_SHORT).show()
        saveScoreToFirestore()
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

    private fun hintAnswer(ans: String){
        tvTotal?.setText("已完成："+total)
        tvCorrect?.setText("正確數："+correct)
        tv_wrong?.setText("錯誤數："+wrong)
        tvPercent?.setText("答對率："+String.format("%.1f%%", percent))

        if(ans == "A"){
            tvTotal?.setText("已完成 ："+total)
        } else if(ans =="B"){
            tvCorrect?.setText("正確數 ："+correct)
        } else if(ans == "C"){
            tv_wrong?.setText("錯誤數 ："+wrong)
        } else{
            tvPercent?.setText("答對率 ："+percent)
        }

    }
    fun calculatePercentage(): Double {
        return if (total!! > 0) {
            (correct?.toDouble()?.div(total!!))?.times(100) ?: 0.0
        } else {
            0.0
        }
    }
    
}

// 讀取 Excel 的 Listener
class ExcelRowListener : ReadListener<Map<Int, String>> {
    override fun invoke(data: Map<Int, String>, context: AnalysisContext) {
        // data 是一個 Map，key 是第幾欄 (0, 1, 2...)，value 是儲存格數值/字串
        println("讀取到第 ${context.readRowHolder().rowIndex} 列數據: $data")
        val col0 = data[0] // 取得 A 欄
        val col1 = data[1] // 取得 B 欄
        val col2 = data[2] // 取得 C 欄
    }

    override fun doAfterAllAnalysed(context: AnalysisContext) {
        println("全部讀取完成！")
    }
}

// 執行讀取
fun readExcelWithEasyExcel(inputStream: InputStream) {
    EasyExcel.read(inputStream, ExcelRowListener()).sheet(0).doRead()
}

/**
 * 讀取 assets 資料夾內的 Excel 檔案
 * @param context Android Context
 * @param fileName assets 資料夾內的檔案名稱 (例: "data.xlsx")
 * @param onRowRead 每一行讀取到的回呼 (RowIndex, DataMap)
 * @param onComplete 讀取完成的回呼
 */
suspend fun readExcelFromAssets(
    context: Context,
    fileName: String,
    onRowRead: (rowIndex: Int, data: Map<Int, String>) -> Unit,
    onComplete: () -> Unit
) {
    // 切換至 IO 執行續處理檔案讀取
    withContext(Dispatchers.IO) {
        try {
            // 1. 開啟 assets 內的檔案 InputStream
            context.assets.open(fileName).use { inputStream ->

                // 2. 建立 EasyExcel 的 ReadListener
                val listener = object : ReadListener<Map<Int, String>> {
                    override fun invoke(data: Map<Int, String>, context: AnalysisContext) {
                        val rowIndex = context.readRowHolder().rowIndex
                        // 讀到一行資料
                        onRowRead(rowIndex, data)
                    }

                    override fun doAfterAllAnalysed(context: AnalysisContext) {
                        // 全部解析完畢
                        onComplete()
                    }
                }

                // 3. 執行讀取 (預設讀取第一個 Sheet)
                EasyExcel.read(inputStream, listener).sheet(0).doRead()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class Quiz(
    val id: Int = 0,
    val subject: String = "",
    val volume: String = "",
    val chapter: String = "",
    val question: String = "",
    val answer: String = "",
    val solution: String = "",
    val image1: String = "",
    val image2: String = "",
    val image3: String = "",
    val image4: String = "",
    val image5: String = "",
    val image6: String = ""
)

