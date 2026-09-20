package com.joyg.quizhero


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

private var tv_facebook_user_name: TextView?=null
// 1. 宣告 FirebaseFirestore 變數
private lateinit var db: FirebaseFirestore
private lateinit var sp_subject : Spinner
private lateinit var sp_volume : Spinner
private lateinit var sp_chapter : Spinner

private lateinit var adapter: LeaderboardAdapter

class MainActivity : ComponentActivity() {

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val bt_add = findViewById<Button>(R.id.bt_add)
        val bt_delete = findViewById<Button>(R.id.bt_delete)

        val bt_exam = findViewById<Button>(R.id.bt_exam)
        sp_subject = findViewById(R.id.sp_subject)
        sp_volume = findViewById(R.id.sp_volume)
        sp_chapter = findViewById(R.id.sp_chapter)

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore


        var subjects = listOf("English","Chinese")
        var Volume = listOf("1","2","3","4","5")
        var chapter = listOf("1","2","3","4")
        lateinit var spSubjectAdapter : ArrayAdapter<Any?>
        lateinit var spVolumeAdapter : ArrayAdapter<Any?>
        lateinit var spChapterAdapter : ArrayAdapter<Any?>
        var selectedSubject = ""
        var selectedVolume = ""
        var selectedChapter = ""

        //sp_subject
        spSubjectAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            subjects
        )
        sp_subject.adapter = spSubjectAdapter
        sp_subject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 取得目前選中的項目字串
                selectedSubject = parent?.getItemAtPosition(position).toString()

                // 在這裡處理選中後的邏輯（例如去查詢 MetaData）
                Log.d("Spinner", "JOYGSAY: 目前選中科目：$selectedSubject")
                setVolumeCount(selectedSubject)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 未選擇任何項目時的處理（通常維持空白即可）
            }
        }

        //sp_Volume
        spVolumeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Volume
        )
        sp_volume.adapter = spVolumeAdapter
        sp_volume.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 取得目前選中的項目字串
                val selectedVolume = parent?.getItemAtPosition(position).toString()

                // 在這裡處理選中後的邏輯（例如去查詢 MetaData）
                Log.d("Spinner", "JOYGSAY: 目前選中冊目：$selectedVolume")
                setChapterCount(selectedSubject, "Volume${selectedVolume}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 未選擇任何項目時的處理（通常維持空白即可）
            }
        }
        //sp_chapter
        spChapterAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            chapter
        )
        sp_chapter.adapter = spChapterAdapter


        bt_exam.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                val selectedSubject = sp_subject.selectedItem.toString()
                val selectedVolume = sp_volume.selectedItem.toString()
                val selectedChapter = sp_chapter.selectedItem.toString()
                putExtra("subject", "${selectedSubject}")
                putExtra("volume", "${selectedVolume}")
                putExtra("chapter", "${selectedChapter}")
                setPackage(packageName)
            }
            startActivity(intent)
        }

        //登入臉書用
        tv_facebook_user_name = findViewById<TextView>(R.id.tv_facebook_user_name)
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

        bt_add.setOnClickListener {
            val fileList = assets.list("")?.filter { it.endsWith(".xlsx") } ?: emptyList()
            val listView = ListView(this)
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileList)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Assets Files")
                .setView(listView)
                .setPositiveButton("OK", null)
                .create()

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedFileName = fileList[position]
                Log.d("MainActivity", "Selected file: $selectedFileName")
                readExcelByLifeCycleScope(selectedFileName)
                dialog.dismiss()
            }
            dialog.show()
        }

        bt_delete.setOnClickListener {
            val fileList = assets.list("")?.filter { it.endsWith(".xlsx") } ?: emptyList()
            val listView = ListView(this)
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileList)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Assets Files")
                .setView(listView)
                .setPositiveButton("OK", null)
                .create()

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedFileName = fileList[position]
                Log.d("MainActivity", "Selected file: $selectedFileName")
                deleteDataToFirestore(selectedFileName)
                dialog.dismiss()
            }
            dialog.show()
        }

        //Leaderboard setting
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLeaderboard)
        adapter = LeaderboardAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 模擬從資料庫或伺服器獲取的排行榜資料
        val leaderboardList = listOf(
            LeaderboardUser("1", "Alice", 120),
            LeaderboardUser("2", "Bob", 98),
            LeaderboardUser("3", "Charlie", 85),
            LeaderboardUser("4", "David", 60),
        )

        // 提交資料給 Adapter
        adapter.submitList(leaderboardList)
    }

    private fun setVolumeCount(sub: String){
        Log.d("JOYG", "JOYGSAY: getVolumeCount, sub="+sub)
        db.collection("MetaData").document(sub)
            .get()
            .addOnSuccessListener { document ->
                var VolumeCount: Int
                lateinit var volumeList : List<String>
                lateinit var newSpVolumeAdapter : ArrayAdapter<Any?>
                if (document != null && document.exists()) {
                    VolumeCount = document.get("VolumeCount").toString().toInt()
                    Log.d("Firestore", "JOYGSAY: ${sub}共有 $VolumeCount 冊")
                    volumeList = (1..VolumeCount).map { "$it" }
                } else {
                    Log.d("Firestore", "JOYGSAY: 找不到 ${sub} 的 MetaData 文件")
                    volumeList = listOf("0")
                }
                newSpVolumeAdapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    volumeList
                )
                sp_volume.adapter = newSpVolumeAdapter
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "JOYGSAY: 讀取 MetaData 失敗", exception)
            }
    }

    private fun setChapterCount(sub: String, vol: String) {
        Log.d("JOYG", "JOYGSAY: setChapterCount")
        db.collection("MetaData").document(sub)
            .get()
            .addOnSuccessListener { document ->
                var chapterCount: Int
                lateinit var chapterList : List<String>
                lateinit var newSpChapterAdapter : ArrayAdapter<Any?>
                if (document != null && document.exists()) {
                    Log.v("JOYG", "JOYGSAY: vol="+vol)
                    chapterCount = document.get("${vol}ChapterCount").toString().toInt()
                    Log.d("Firestore", "JOYGSAY: ${sub} 第 ${vol} 冊共有 $chapterCount 個章節")
                    chapterList = (1..chapterCount).map { "$it" }
                } else {
                    Log.d("Firestore", "JOYGSAY: 找不到 ${sub} 的 MetaData 文件")
                    chapterList = listOf("0")
                }
                newSpChapterAdapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    chapterList
                )
                sp_chapter.adapter = newSpChapterAdapter
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "JOYGSAY: 讀取 MetaData 失敗", exception)
            }
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
                    val userId = jsonObject.optString("id")
                    val name = jsonObject.optString("name", "未知使用者")
                    val email = jsonObject.optString("email", "未提供 Email")
                    //val pictureObj = jsonObject.optJSONObject("picture")
                    //val dataObj = pictureObj?.optJSONObject("data")
                    //val photoUrl = dataObj?.optString("url") // 可直接用 Glide / Coil 載入此 URL
                    //val locale = jsonObject.optString("locale")
                    //Log.d("FBData", "JOYGSAY: name: $name, ID: $userId, Email: $email, Photo: $photoUrl")
                    Log.d("FBData", "JOYGSAY: name: $name, ID: $userId, Email: $email")

                    // UI 異動必須在 Main Thread 執行（GraphRequest 回呼預設已在 UI 線程）
                    textView?.text = "歡迎， $name"

                    //以id檢查firestore內是否存在該user，沒有才新增使用者資訊。
                    db.collection("User")
                        .whereEqualTo("id", userId)
                        .get()
                        .addOnSuccessListener {
                                querySnapshot ->
                            if(querySnapshot.isEmpty) {
                                Log.v("JOYG", "JOYGSAY: name= $name 不存在，準備將$name 存到Firestore.")
                                //save facebook info to firestore here.
                                saveUserInfoToFirestore(userId, name, email, "", "")
                            }
                        }

                } catch (e: Exception) {
                    Log.e("FBAuth", "JOYGSAY: 解析使用者資料失敗: ${e.message}")
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

    private fun readExcelByLifeCycleScope(execelFileName: String){
        // 在 Activity 或 Fragment 中：
        lifecycleScope.launch {
            readExcelFromAssets(
                context = this@MainActivity,
                fileName = execelFileName,
                onRowRead = { rowIndex, rowData ->
                    // rowData[0] 代表 A 欄，rowData[1] 代表 B 欄，依此類推
                    val colA = rowData[0] ?: ""
                    val colB = rowData[1] ?: ""
                    val colC = rowData[2] ?: ""
                    val colD = rowData[3] ?: ""
                    val colE = rowData[4] ?: ""
                    val colF = rowData[5] ?: ""
                    val colG = rowData[6] ?: ""
                    val colH = rowData[7] ?: ""
                    val colI = rowData[8] ?: ""
                    val colJ = rowData[9] ?: ""
                    val colK = rowData[10] ?: ""
                    val colL = rowData[11] ?: ""
                    val colM = rowData[12] ?: ""

                    //println("第 $rowIndex 行 - A欄: $colA, B欄: $colB, C欄: $colC, D欄: $colD, E欄: $colD, F欄: $colF, G欄: $colG")
                    saveDataToFirestore(execelFileName, colA.toInt(), colB, colC, colD, colE, colF, colG, colH, colI, colJ, colK, colL, colM)
                },
                onComplete = {
                    println("Excel 檔案全部讀取完成！")
                }
            )
        }
    }

    private fun deleteDataToFirestore(execelFileName: String){
        db.collection(execelFileName.substringBeforeLast("."))
            .whereEqualTo("volume", "5")
            .whereEqualTo("chapter", "3")
            .get()
            .addOnSuccessListener {
                    querySnapshot ->
                if(querySnapshot.isEmpty) {
                    Toast.makeText(this, "找不到${execelFileName}", Toast.LENGTH_SHORT).show()
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
    private fun saveDataToFirestore(fileName: String, id: Int, sub: String, vol: String, cha: String, ans: String, que: String, sol: String, img1: String, img2: String, img3: String, img4: String, img5: String, img6: String) {
        Log.d("FirestoreDemo", "Call saveDataToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val question = hashMapOf(
            "id" to id,
            "subject" to sub,
            "volume" to vol,
            "chapter" to cha,
            "answer" to ans,
            "question" to que,
            "solution" to sol,
            "image1" to img1,
            "image2" to img2,
            "image3" to img3,
            "image4" to img4,
            "image5" to img5,
            "image6" to img6
        )

        // 4. 指定集合名稱 "EnglishQuiz"，並自動產生文件 ID 新增資料 (.add)
        //db.collection("English_Quiz")
        db.collection(fileName.substringBeforeLast("."))
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

    private fun saveUserInfoToFirestore(userId: String, name: String, email: String, photoUrl: String?, locale: String) {
        Log.d("FirestoreDemo", "JOYGSAY: Call saveUserInfoToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val facebookInformation = hashMapOf(
            "id" to userId,
            "name" to name,
            "email" to email,
            "photoUrl" to photoUrl,
            "locale" to locale,
            "heart" to 3,
            "heartContainer" to 3,
            "level" to 1,
        )

        // 4. 指定集合名稱 "EnglishQuiz"，並自動產生文件 ID 新增資料 (.add)
        db.collection("User")
            .add(facebookInformation)
            .addOnSuccessListener { documentReference ->
                // 新增成功時的回呼
                Log.d("FirestoreDemo", "JOYGSAY: facebook info 資料新增成功.")
                //Toast.makeText(this, "新增成功！ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 新增失敗時的回呼
                Log.w("FirestoreDemo", "JOYGSAY: facebook info 新增資料時發生錯誤", e)
                //Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


data class LeaderboardUser(
    val userId: String,
    val name: String,
    val score: Int,          // 答題數或總分
    val avatarUrl: String? = null
)