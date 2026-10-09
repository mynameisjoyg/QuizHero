package com.joyg.quizhero


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.alibaba.excel.EasyExcel
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import com.facebook.CallbackManager
import com.facebook.GraphRequest
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.joyg.quizhero.databinding.ActivityQuizBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import androidx.activity.OnBackPressedCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.tasks.await

private lateinit var firebaseAnalytics: FirebaseAnalytics

private var id: Int = 0
private lateinit var userId: String

var subject: String = ""
var volume: String =""
var chapter: String =""
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
private var totalCorrectCount: Int = 0
private var wrong: Int =0
private var percent: Double =0.0

private lateinit var time_start: String
private lateinit var time_end: String

// 1. 定義顏色常數 (可以使用 ContextCompat 或 Color.parseColor)
private val COLOR_CORRECT = Color.parseColor("#4CAF50") // 綠色 (正確)
private val COLOR_WRONG = Color.parseColor("#F44336")   // 紅色 (選錯)
private val COLOR_DISABLED = Color.parseColor("#E0E0E0")// 灰色 (未選/停用)
private val COLOR_TEXT_DISABLED = Color.parseColor("#757575") // 灰色文字

class QuizActivity : ComponentActivity() {
    private lateinit var tvQuestionTitle: TextView
    private lateinit var tvStatus: TextView

    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    //
    private lateinit var btnOptionA: Button
    private lateinit var btnOptionB: Button
    private lateinit var btnOptionC: Button
    private lateinit var btnOptionD: Button

    // 2. 宣告 binding 變數
    private lateinit var binding: ActivityQuizBinding

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "MainActivityAdMob"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // 1. 初始化 AdMob SDK
        MobileAds.initialize(this) {}

        // 2. 預先載入插頁式廣告
        loadInterstitialAd()


        // 1. 初始化 binding (將 layout XML 膨脹/載入成視圖物件)
        binding = ActivityQuizBinding.inflate(layoutInflater)

        // 2. 設定內容視圖為 binding.root (代替原本的 R.layout.activity_battle)
        setContentView(binding.root)

        // 3. 這時候就可以順利使用 binding.root 了！
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // 為底部的選項區塊加上導覽列高度 Padding，避免被切掉
            binding.layoutAnswers.setPadding(0, 0, 0, navigationBars.bottom)
            insets
        }



        //取得MainActivity傳送過來的subject
        // 1. 綁定 TextView 元件
//        val tvSelectedSubject: TextView = findViewById(R.id.tv_selected_subject)

        // 2. 接收從 MainActivity 傳過來的字串，若沒有傳值則設定預設值
        userId = intent.getStringExtra("userId") ?: "未選擇"
        val subject = intent.getStringExtra("subject") ?: "未選擇"
        val volume = intent.getStringExtra("volume")?:"未選擇"
        val chapter = intent.getStringExtra("chapter")?:"未選擇"
        Log.v("JOYG", "JOYGSAY: getStringExtra, userId=$userId, subject=$subject, volume=$volume, chapter=$chapter")

        // 3. 將取得的資料顯示在 TextView 上
  //      tvSelectedSubject.text = "科目：${subject}\t\t冊目：${volume}\t\t章節：${chapter}"

        //記錄開始作答時間
        time_start = getCurrentTimeString()

        tvQuestionTitle = findViewById<TextView>(R.id.tvQuestionTitle)
        tvStatus = findViewById(R.id.tvStatus)

        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)


        total =0
        correct =0
        wrong=0
        percent=0.0

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore

        queryQuestion(subject, volume, chapter)

        //按下四個選項之一
        val answerClickListener = View.OnClickListener { view ->
            val selectedOption = (view as Button).text.toString()

            // 匹配括號中的單一英文字母
            val regex = Regex("""\(\s*([A-Za-z])\s*\)""")
            val selectedAnswer = regex.find(selectedOption)?.groupValues?.get(1) ?: ""

            Log.v("JOYG", "JOYGSAY: selectedAnswer=${selectedAnswer}, selectedOption=${selectedOption}")

            if (answer == selectedAnswer) {
                //Toast.makeText(this, "答對了", Toast.LENGTH_SHORT).show()
                tvStatus.text = "🏆 恭喜你作答成功！"
                correct = correct+1

            } else {
                //Toast.makeText(this, "答錯了", Toast.LENGTH_SHORT).show()
                tvStatus.text = "❌ 答錯了！"
                wrong= wrong+1
            }

            //設定答案顏色
            if(answer == "A") {
                btnOptionA.setBackgroundColor(COLOR_CORRECT)
            } else if(answer == "B") {
                btnOptionB.setBackgroundColor(COLOR_CORRECT)
            } else if (answer == "C") {
                btnOptionC.setBackgroundColor(COLOR_CORRECT)
            } else{
                btnOptionD.setBackgroundColor(COLOR_CORRECT)
            }

            total= total+1
            percent = calculatePercentage()
            Log.v("JOYG", "JOYG: percent = "+ percent)

            setAnswerButtonsEnabled(false)

            // 啟動協程並延遲 5 秒
            lifecycleScope.launch {
                delay(5000) // 延遲 5000 毫秒（非阻塞）

                // 5 秒後要執行的程式碼（依然在主執行緒）
                //答對五題顯示一次廣告
                if(correct%5==0) {
                    //呼叫廣告
                    showInterstitialAdAndProceed{
                        queryQuestion(subject, volume, chapter)
                    }
                } else {
                    queryQuestion(subject, volume, chapter)
                }
                

                setAnswerButtonsEnabled(true)
                tvStatus.text = "❓ 題目來了！請作答！"
                // 設定選項顏色
                btnOptionA.setBackgroundColor(COLOR_DISABLED)
                btnOptionB.setBackgroundColor(COLOR_DISABLED)
                btnOptionC.setBackgroundColor(COLOR_DISABLED)
                btnOptionD.setBackgroundColor(COLOR_DISABLED)

            }
        }

        btnOptionA.setOnClickListener(answerClickListener)
        btnOptionB.setOnClickListener(answerClickListener)
        btnOptionC.setOnClickListener(answerClickListener)
        btnOptionD.setOnClickListener(answerClickListener)
        setAnswerButtonsEnabled(true)

        // 設定返回鍵監聽器
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 當使用者按下返回鍵（實體按鍵或滑動手勢）時會執行這裡
                showExitDialog()
            }
        })
    }

    override fun onStart() {
        super.onStart()

        val currentAccessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = currentAccessToken != null && !currentAccessToken.isExpired

        if (isLoggedIn) {
            // 使用者先前已登入，直接抓取資料顯示
            //fetchUserInfoWithGraphApi(currentAccessToken, tvFacebookUserName)
        }
    }

    // 4. 將 Intent 結果傳遞給 Facebook SDK CallbackManager
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    ////Google Ads

    /**
     * 預先載入插頁廣告
     */
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        // 測試用插頁廣告 Unit ID: ca-app-pub-3940256099942544/1033173712
        // 正式上架請替換為您在 AdMob 後台建立的 Interstitial Ad Unit ID
        InterstitialAd.load(
            this,
            getString(R.string.interstitialAdUnitId),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "插頁廣告載入失敗: ${adError.message}")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "插頁廣告載入成功！")
                    mInterstitialAd = interstitialAd
                    setupAdCallbacks() // 設定廣告關閉與展示狀態監聽
                }
            }
        )
    }


    /**
     * 設定廣告展示與關閉的回呼 (Callbacks)
     */
    private fun setupAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "使用者關閉了插頁廣告")
                mInterstitialAd = null
                // 廣告關閉後重新載入下一檔廣告，備供下次使用
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "廣告展示失敗: ${adError.message}")
                mInterstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "插頁廣告成功在螢幕展示")
            }
        }
    }


    /**
     * 顯示廣告並執行後續流程
     */
    private fun showInterstitialAdAndProceed(onComplete: () -> Unit) {
        if (mInterstitialAd != null) {
            // 在廣告關閉時自動觸發 onComplete 動作
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd() // 預載下一檔
                    onComplete() // 執行主要業務邏輯 (如頁面跳轉)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onComplete() // 顯示失敗時依然讓使用者繼續操作
                }
            }
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "廣告尚未載入完成，直接執行下一步驟")
            onComplete()
        }
    }


    ////End of Google Ads

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
        Log.v("JOYG", "JOYGSAY: queryQuestion, sub=$sub, vol=$vol, chap=$chap")

        var totalCount = 0
        var randomNumber = 1
        db.collection("MetaData").document(sub)
            .get()
            .addOnSuccessListener { document ->
                var QuestionFirstId: Int
                if (document != null && document.exists()) {
                    //取得全部題目個數
                    totalCount = document.get("Volume"+vol + "Chapter" + chap + "QuestionCount").toString().toInt()
                    //取得該冊目、該章節的第一題的ID
                    QuestionFirstId = document.get("Volume"+vol + "Chapter" + chap + "QuestionFirstId").toString().toInt()

                    //取得隨機題目id
                    randomNumber = (0..totalCount - 1).random()
                    Log.v("JOYG", "JOYGSAY: totalCount=" + totalCount + ", randomNumber=" + randomNumber)

                    //利用第一題的ID+隨機數字，組成隨機ID，來取得題目、解答和詳解
                    val randomId = QuestionFirstId + randomNumber
                    Log.v("JOYG", "JOYGSAY: randomId=${randomId}")
                    db.collection("${sub}_Quiz")
                        .whereEqualTo("volume", vol)
                        .whereEqualTo("chapter", chap)
                        .whereEqualTo("id", randomId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                //把題目顯示出來
                                if (!querySnapshot.isEmpty) {
                                    // 1. 轉成 Quiz 物件
                                    val quiz = querySnapshot.documents[0].toObject(Quiz::class.java)

                                    // 2. 取出 question 欄位並設定給 TextView
                                    quiz?.let {
                                        id = it.id
                                        subject = it.subject
                                        volume = it.volume
                                        chapter = it.chapter
                                        answer = it.answer
                                        question = it.question
                                        solution = it.solution
                                        image1 = it.image1
                                        image2 = it.image2
                                        image3 = it.image3
                                        image4 = it.image4
                                        image5 = it.image5
                                        image6 = it.image6

                                        tvQuestionTitle?.text = it.question
                                        tvQuestionTitle.text = tvQuestionTitle.text.replace(
                                            Regex("\\(A\\)"),
                                            "\n(A)"
                                        )
                                        tvQuestionTitle.text = tvQuestionTitle.text.replace(
                                            Regex("\\(B\\)"),
                                            "\n(B)"
                                        )
                                        tvQuestionTitle.text =
                                            tvQuestionTitle.text.replace(Regex("\\(C"), "\n(C")
                                        tvQuestionTitle.text = tvQuestionTitle.text.replace(
                                            Regex("\\(D\\)"),
                                            "\n(D)"
                                        )

                                        // 1. 擷取選項
                                        //Pattern optionPattern = Pattern.compile("\\([A-D]\\)\\s*[^\\(\\)]+");
                                        val optionPattern: Pattern =
                                            Pattern.compile("\\([A-D][^\\(]*")
                                        val matcher: Matcher =
                                            optionPattern.matcher(tvQuestionTitle.text)

                                        val options: MutableList<String?> = ArrayList<String?>()
                                        var firstOptionIndex = -1

                                        while (matcher.find()) {
                                            if (firstOptionIndex == -1) {
                                                firstOptionIndex =
                                                    matcher.start() // 記錄第一個選項 (A) 開始的位置
                                            }
                                            options.add(matcher.group().trim())
                                        }

                                        // 2. 擷取不含選項的題目主幹
                                        val stem: String =
                                            (if (firstOptionIndex != -1) tvQuestionTitle.text.substring(
                                                0,
                                                firstOptionIndex
                                            ).trim() else tvQuestionTitle.text.trim()) as String

                                        var optionA = options.get(0)
                                        var optionB = options.get(1)
                                        var optionC = options.get(2)
                                        var optionD = options.get(3)

                                        if(answer == "A"){
                                            optionA = optionA + "."
                                        } else if(answer =="B"){
                                            optionB = optionB  + "."
                                        } else if(answer == "C"){
                                            optionC = optionC  + "."
                                        } else{
                                            optionD = optionD + "."
                                        }

                                        btnOptionA.setText(optionA)
                                        btnOptionB.setText(optionB)
                                        btnOptionC.setText(optionC)
                                        btnOptionD.setText(optionD)

                                        tvQuestionTitle.setText(stem)

                                    }
                                } else {
                                    tvQuestionTitle?.text = "找不到題目"
                                }
                            }
                        }

                } else{
                    Log.v("JOYG", "JOYGSAY: in queryQuestion, MetaData中找不到${sub}")
                    Toast.makeText(this, "MetaData中找不到${sub}", Toast.LENGTH_SHORT).show()
                }
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

    private fun setAnswerButtonsEnabled(enabled: Boolean) {
        btnOptionA.isEnabled = enabled
        btnOptionB.isEnabled = enabled
        btnOptionC.isEnabled = enabled
        btnOptionD.isEnabled = enabled

    }

    private fun saveScoreToFirestore(){

        //先取得最新的totalCorrectCount
        Log.v("JOYG", "JOYGSAY: userId=${userId}")
        val db = FirebaseFirestore.getInstance()
        db.collection("User").whereEqualTo("id", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // 取得 totalCorrectCount 欄位，若為 null 或不存在則預設為 0
                    totalCorrectCount = querySnapshot.documents[0].getLong("totalCorrectCount")?.toInt() ?: 0
                    var totalWrongCount = querySnapshot.documents[0].getLong("totalWrongCount")?.toInt() ?: 0
                    totalCorrectCount = totalCorrectCount + correct
                    totalWrongCount = totalWrongCount + wrong

                    //把總正確答對數與總答錯數更新到User資料表當中
                    db.collection("User").document(querySnapshot.documents[0].id)
                        .update(
                            "totalCorrectCount", totalCorrectCount,
                            "totalWrongCount", totalWrongCount
                        )
                        .addOnSuccessListener {
                            Log.d("Firestore", "JOYGSAY: 更新成功！")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "JOYGSAY: 更新失敗: ${e.message}")
                        }
                } else {
                    Log.d("Firestore", "JOYGSAY: 找不到該使用者的資料")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "JOYGSAY: 讀取資料失敗", exception)
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

