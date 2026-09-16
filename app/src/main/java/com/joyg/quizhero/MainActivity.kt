package com.joyg.quizhero


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
private var tv_facebook_user_name: TextView?=null


class MainActivity : ComponentActivity() {
    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val bt_exam = findViewById<Button>(R.id.bt_exam)
        val sp_subject: Spinner = findViewById(R.id.sp_subject)
        val sp_volumn: Spinner = findViewById(R.id.sp_volumn)
        val sp_chapter: Spinner = findViewById(R.id.sp_chapter)

        //sp_subject
        val subjects = listOf("English","Chinese")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            subjects
        )
        sp_subject.adapter = adapter
        //sp_volumn
        val volumn = listOf("1","2","3","4","5")
        val volumn_adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            volumn
        )
        sp_volumn.adapter = volumn_adapter
        //sp_chapter
        val chapter = listOf("1","2","3","4")
        val chapter_adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            chapter
        )
        sp_chapter.adapter = chapter_adapter

        bt_exam.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                val selectedSubject = sp_subject.selectedItem.toString()
                val selectedVolumn = sp_subject.selectedItem.toString()
                val selectedChapter = sp_chapter.selectedItem.toString()
                putExtra("subject", "${selectedSubject}")
                putExtra("volumn", "${selectedVolumn}")
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


        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore

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

