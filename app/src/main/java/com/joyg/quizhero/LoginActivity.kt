package com.joyg.quizhero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class LoginActivity : ComponentActivity() {
    private var tv_facebook_user_name: TextView?=null
    //登入臉書用
    private lateinit var callbackManager: CallbackManager
    private lateinit var accessTokenTracker: AccessTokenTracker
    private var userId : String = ""
    private var name : String = ""
    private var email : String = ""

    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore


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
                userId = result.accessToken.userId
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

        tv_facebook_user_name?.setOnClickListener {
            if(tv_facebook_user_name?.text!="未登入"){
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("name", name)
                    putExtra("userId", userId)
                    setPackage(packageName)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentAccessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = currentAccessToken != null && !currentAccessToken.isExpired

        if (isLoggedIn) {
            userId = currentAccessToken.userId
            // 使用者先前已登入，直接抓取資料顯示
            fetchUserInfoWithGraphApi(currentAccessToken, tv_facebook_user_name)
        }

    }

    fun fetchUserInfoWithGraphApi(accessToken: AccessToken, textView: TextView?) {
        // 建立 Graph API 請求，目標為 "me" (目前登入的使用者)
        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, response ->
            if (jsonObject != null) {
                try {
                    // 解析 JSON 回傳內容
                    userId = jsonObject.optString("id")
                    name = jsonObject.optString("name", "未知使用者")
                    email = jsonObject.optString("email", "未提供 Email")
                    Log.d("FBData", "JOYGSAY: name: $name, ID: $userId, Email: $email")

                    // UI 異動必須在 Main Thread 執行（GraphRequest 回呼預設已在 UI 線程）
                    textView?.text = "歡迎， $name，點擊以登入。"

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