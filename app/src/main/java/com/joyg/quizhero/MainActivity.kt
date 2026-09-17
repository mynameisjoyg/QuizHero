package com.joyg.quizhero


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
import com.google.protobuf.LazyStringArrayList.emptyList

private var tv_facebook_user_name: TextView?=null
// 1. 宣告 FirebaseFirestore 變數
private lateinit var db: FirebaseFirestore
private lateinit var sp_subject : Spinner
private lateinit var sp_Volume : Spinner
private lateinit var sp_chapter : Spinner


class MainActivity : ComponentActivity() {

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val bt_exam = findViewById<Button>(R.id.bt_exam)
        sp_subject = findViewById(R.id.sp_subject)
        sp_Volume = findViewById(R.id.sp_volume)
        sp_chapter = findViewById(R.id.sp_chapter)

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore


        var subjects = listOf("選擇", "English","Chinese")
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
        sp_Volume.adapter = spVolumeAdapter
        sp_Volume.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                val selectedVolume = sp_subject.selectedItem.toString()
                val selectedChapter = sp_chapter.selectedItem.toString()
                putExtra("subject", "${selectedSubject}")
                putExtra("Volume", "${selectedVolume}")
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
                sp_Volume.adapter = newSpVolumeAdapter
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
}


