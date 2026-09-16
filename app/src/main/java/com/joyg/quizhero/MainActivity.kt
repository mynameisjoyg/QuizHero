package com.joyg.quizhero


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

private lateinit var firebaseAnalytics: FirebaseAnalytics
private var tv_question: TextView? = null
private var tv_total: TextView? = null
private var tv_correct: TextView? = null
private var tv_wrong: TextView? = null
private var tv_percent: TextView? = null
private var tv_facebook_user_name: TextView?=null

private var id: Int = 0
private var answer: String = ""
private var question: String = ""
private var solution: String = ""
private var total: Int  = 0
private var correct: Int  = 0
private var wrong: Int =0
private var percent: Double =0.0

class MainActivity : ComponentActivity() {
    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val bt_add = findViewById<Button>(R.id.bt_add)
        val bt_delete = findViewById<Button>(R.id.bt_delete)
        val bt_submit = findViewById<Button>(R.id.bt_submit)
        val bt_exit = findViewById<Button>(R.id.bt_exit)
        val radioGroup = findViewById<RadioGroup>(R.id.rg_options)
        tv_question = findViewById<TextView>(R.id.tv_question)
        tv_total = findViewById<TextView>(R.id.tv_total)
        tv_correct = findViewById<TextView>(R.id.tv_correct)
        tv_wrong = findViewById<TextView>(R.id.tv_wrong)
        tv_percent = findViewById<TextView>(R.id.tv_percent)
        tv_facebook_user_name = findViewById<TextView>(R.id.tv_facebook_user_name)

        //登入臉書用
        // 1. 初始化 CallbackManager
        callbackManager = CallbackManager.Factory.create()
        val btnFacebookSignIn = findViewById<LoginButton>(R.id.btnFacebookSignIn)
        // 2. 設定向 Facebook 請求的權限（預設會取得 public_profile）
        btnFacebookSignIn.setPermissions("public_profile")
        // 3. 註冊 Login 回呼
        btnFacebookSignIn.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // 登入成功，取得 Access Token
                val accessToken = result.accessToken.token
                val userId = result.accessToken.userId
                Log.d("FBAuth", "登入成功！User ID: $userId, Token: $accessToken")

                // 呼叫 Graph API 取得姓名並顯示在 txtUserName (TextView)
                fetchUserInfoWithGraphApi(result.accessToken, findViewById<TextView>(R.id.tv_facebook_user_name))

                // TODO: 可將 accessToken 傳送至自家 Server 或 Firebase 進行認證
            }

            override fun onCancel() {
                Log.d("FBAuth", "使用者取消登入")
            }

            override fun onError(error: FacebookException) {
                Log.e("FBAuth", "登入失敗: ${error.message}")
            }
        })
        // 建立 AccessToken 監聽器
        accessTokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(
                oldAccessToken: AccessToken?,
                currentAccessToken: AccessToken?
            ) {
                // 當 currentAccessToken 變為 null 時，代表使用者已登出
                if (currentAccessToken == null) {
                    tv_facebook_user_name?.text = "未登入"
                }
            }
        }

        // 開始監聽 Token 狀態變化
        accessTokenTracker.startTracking()


        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore



        bt_exit.setOnClickListener {

        }

        bt_submit.setOnClickListener {
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
                    tv_correct?.setText("正確數："+correct)

                } else {
                    Toast.makeText(this, "答錯了", Toast.LENGTH_SHORT).show()
                    wrong= wrong+1
                    tv_wrong?.setText("錯誤數："+wrong)
                }
                total= total+1
                tv_total?.setText("已完成："+total)
                percent = calculatePercentage()
                Log.v("JOYG", "JOYG: percent = "+ percent)
                tv_percent?.setText("正確率："+String.format("%.1f%%", percent))
                //下一題
                queryQuestion()
            } else {
                println("使用者還沒選擇任何選項！")
            }
        }

        bt_add.setOnClickListener {
            readExcelByLifeCycleScope()
        }

        bt_delete.setOnClickListener {
            deleteDataToFirestore()
        }

        queryQuestion()
    }

    override fun onStart() {
        super.onStart()

        val currentAccessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = currentAccessToken != null && !currentAccessToken.isExpired

        if (isLoggedIn) {
            // 使用者先前已登入，直接抓取資料顯示
            fetchUserInfoWithGraphApi(currentAccessToken, tv_facebook_user_name)
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

    private fun queryQuestion(){
        var totalCount = 0
        var randomNumber = 1
        db.collection("English_Quiz")
            .get()
            .addOnSuccessListener {
                    querySnapshot ->
                if(querySnapshot.isEmpty) {
                    Toast.makeText(this, "找不到EnglishQuiz", Toast.LENGTH_SHORT).show()
                }
                //取得全部題目個數
                totalCount = querySnapshot.size()
                //取得隨機題目id
                randomNumber = (1..totalCount).random()
                Log.v("JOYG","JOYG: totalCount="+totalCount+", randomNumber="+ randomNumber)

                //利用隨機id來取得題目、解答和詳解
                Log.v("JOYG", "JOYG: randomNumber=${randomNumber}")
                db.collection("EnglishQuiz")
                    .whereEqualTo("id", randomNumber)
                    .get()
                    .addOnSuccessListener {
                            querySnapshot ->
                        if(!querySnapshot.isEmpty) {
                            //把題目顯示出來
                            if (!querySnapshot.isEmpty) {
                                // 1. 轉成 Quiz 物件
                                val quiz = querySnapshot.documents[0].toObject(Quiz::class.java)

                                // 2. 取出 question 欄位並設定給 TextView
                                quiz?.let {
                                    id=it.id
                                    answer = it.answer
                                    question=it.question
                                    solution=it.solution

                                    tv_question?.text = it.question
                                    hintAnswer(answer)
                                }
                            } else {
                                tv_question?.text = "找不到題目"
                            }
                        }
                    }
            }
            .addOnFailureListener { e->
                Log.d("FirestoreDemo", "取得全部題目個數失敗", e)
                Toast.makeText(this, "取得全部題目個數失敗: ${e.message}", Toast.LENGTH_SHORT).show()

            }


    }

    private fun readExcelByLifeCycleScope(){
        // 在 Activity 或 Fragment 中：
        lifecycleScope.launch {
            readExcelFromAssets(
                context = this@MainActivity,
                fileName = "EnglishQuiz.xlsx",
                onRowRead = { rowIndex, rowData ->
                    // rowData[0] 代表 A 欄，rowData[1] 代表 B 欄，依此類推
                    val colA = rowData[0] ?: ""
                    val colB = rowData[1] ?: ""
                    val colC = rowData[2] ?: ""
                    val colD = rowData[3] ?: ""
                    val colE = rowData[4] ?: ""
                    val colF = rowData[5] ?: ""
                    val colG = rowData[6] ?: ""

                    //println("第 $rowIndex 行 - A欄: $colA, B欄: $colB, C欄: $colC, D欄: $colD, E欄: $colD, F欄: $colF, G欄: $colG")
                    saveDataToFirestore(colA.toInt(), colB, colC, colD, colE, colF, colG)
                },
                onComplete = {
                    println("Excel 檔案全部讀取完成！")
                }
            )
        }
    }

    private fun deleteDataToFirestore(){
        db.collection("EnglishQuiz")
            .get()
            .addOnSuccessListener {
                querySnapshot ->
                if(querySnapshot.isEmpty) {
                    Toast.makeText(this, "找不到EnglishQuiz", Toast.LENGTH_SHORT).show()
                }
                val totalCount = querySnapshot.size()
                var deleteCount = 0
                querySnapshot.documents.forEach { document ->
                    document.reference.delete()
                        .addOnSuccessListener {
                            deleteCount ++
                            Log.d("FirestoreDemo", "成功刪除單筆文件ID: ${document.id}")

                            if(deleteCount == totalCount) {
                                Toast.makeText(this, "已成功刪除資料共${deleteCount}筆",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.d("FirestoreDemo", "刪除文件: ${document.id}失敗", e)
                        }

                }
            }
            .addOnFailureListener { e->
                Log.d("FirestoreDemo", "查詢失敗", e)
                Toast.makeText(this, "查詢失敗: ${e.message}", Toast.LENGTH_SHORT).show()

            }

    }
    private fun saveDataToFirestore(id: Int, sub: String, vol: String, cha: String, ans: String, que: String, sol: String) {
        Log.d("FirestoreDemo", "Call saveDataToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val question = hashMapOf(
            "id" to id,
            "subject" to sub,
            "volume" to vol,
            "chapter" to cha,
            "answer" to ans,
            "question" to que,
            "solution" to sol
        )

        // 4. 指定集合名稱 "EnglishQuiz"，並自動產生文件 ID 新增資料 (.add)
        db.collection("English_Quiz")
            .add(question)
            .addOnSuccessListener { documentReference ->
                // 新增成功時的回呼
                Log.d("FirestoreDemo", "資料新增成功.")
                //Toast.makeText(this, "新增成功！ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 新增失敗時的回呼
                Log.w("FirestoreDemo", "新增資料時發生錯誤", e)
                //Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

private fun hintAnswer(ans: String){
    tv_total?.setText("已完成："+total)
    tv_correct?.setText("正確數："+correct)
    tv_wrong?.setText("錯誤數："+wrong)
    tv_percent?.setText("答對率："+String.format("%.1f%%", percent))

    if(ans == "A"){
        tv_total?.setText("已完成 ："+total)
    } else if(ans =="B"){
        tv_correct?.setText("正確數 ："+correct)
    } else if(ans == "C"){
        tv_wrong?.setText("錯誤數 ："+wrong)
    } else{
        tv_percent?.setText("答對率 ："+percent)
    }

}
fun calculatePercentage(): Double {
    return if (total!! > 0) {
        (correct?.toDouble()?.div(total!!))?.times(100) ?: 0.0
    } else {
        0.0
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
    val question: String = "",
    val answer: String = "",
    val solution: String = ""
)

