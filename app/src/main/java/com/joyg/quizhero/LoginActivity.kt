package com.joyg.quizhero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class LoginActivity : ComponentActivity() {
//    private var tv_facebook_user_name: TextView?=null
//    //登入臉書用
//    private lateinit var callbackManager: CallbackManager
//    private lateinit var accessTokenTracker: AccessTokenTracker
    private var userId : String = ""
    private var name : String = ""
    private var password : String = ""
//    private var email : String = ""

    private lateinit var etName: EditText
    private lateinit var etPassword: EditText

    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore

        etName = findViewById<EditText>(R.id.et_name)
        etPassword = findViewById<EditText>(R.id.et_password)

        val btLogin = findViewById<Button>(R.id.bt_loggin)
        btLogin.setOnClickListener {
            name = etName.text.toString()
            password = etPassword.text.toString()

            if (name=="" || password=="") {
                Toast.makeText(this, "請輸入完整帳號密碼！", Toast.LENGTH_LONG).show()
            } else {
                fetchUserInfoFromFirestore(name, password) { exists ->
                    if (exists) {
                        // 使用者存在的處理邏輯
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("userId", userId)
                            putExtra("name", name)
                            putExtra("password", password)
                            setPackage(packageName)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        // 使用者不存在的處理邏輯
                        Toast.makeText(this, "帳號：${name}不存在", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        var bt_registor = findViewById<Button>(R.id.bt_registor)
        bt_registor.setOnClickListener {
            name = etName.text.toString()
            password = etPassword.text.toString()

            if (name=="" || password=="") {
                Toast.makeText(this, "請輸入完整帳號密碼！", Toast.LENGTH_LONG).show()
            }else if (name!="" && password!="") {
                fetchUserInfoFromFirestore(name, password) { exists ->
                    if (exists) {
                        Toast.makeText(this, "該帳號：${name} 已存在！", Toast.LENGTH_LONG).show()
                    } else{
                        userId = UUID.randomUUID().toString()
                        saveUserInfoToFirestore(userId, etName.text.toString(), etPassword.text.toString())
                        Toast.makeText(this, "註冊帳號：${name} 完成！", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("name", name)
                            putExtra("password", password)
                            setPackage(packageName)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
        //


//        //登入臉書用
//        tv_facebook_user_name = findViewById<TextView>(R.id.tv_facebook_user_name)
//        // 1. 初始化 CallbackManager
//        callbackManager = CallbackManager.Factory.create()
//        val btnFacebookSignIn = findViewById<LoginButton>(R.id.btnFacebookSignIn)
//        // 2. 設定向 Facebook 請求的權限（預設會取得 public_profile）
//        btnFacebookSignIn.setPermissions("public_profile")
//        // 3. 註冊 Login 回呼
//        btnFacebookSignIn.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
//            override fun onSuccess(result: LoginResult) {
//                // 登入成功，取得 Access Token
//                val accessToken = result.accessToken.token
//                userId = result.accessToken.userId
//                Log.d("FBAuth", "登入成功！User ID: $userId, Token: $accessToken")
//
//                // 呼叫 Graph API 取得姓名並顯示在 txtUserName (TextView)
//                fetchUserInfoWithGraphApi(result.accessToken, findViewById<TextView>(R.id.tv_facebook_user_name))
//
//                // TODO: 可將 accessToken 傳送至自家 Server 或 Firebase 進行認證
//                handleFacebookAccessToken(result.accessToken)
//            }
//
//            override fun onCancel() {
//                Log.d("FBAuth", "使用者取消登入")
//            }
//
//            override fun onError(error: FacebookException) {
//                Log.e("FBAuth", "登入失敗: ${error.message}")
//            }
//        })
//        // 建立 AccessToken 監聽器
//        accessTokenTracker = object : AccessTokenTracker() {
//            override fun onCurrentAccessTokenChanged(
//                oldAccessToken: AccessToken?,
//                currentAccessToken: AccessToken?
//            ) {
//                // 當 currentAccessToken 變為 null 時，代表使用者已登出
//                if (currentAccessToken == null) {
//                    tv_facebook_user_name?.text = "未登入"
//                }
//            }
//        }
//
//        // 開始監聽 Token 狀態變化
//        accessTokenTracker.startTracking()
//
//        tv_facebook_user_name?.setOnClickListener {
//            if(tv_facebook_user_name?.text!="未登入"){
//                val intent = Intent(this, MainActivity::class.java).apply {
//                    putExtra("name", name)
//                    putExtra("userId", userId)
//                    setPackage(packageName)
//                }
//                startActivity(intent)
//                finish()
//            }
//        }
    }

    override fun onStart() {
        super.onStart()
        val currentAccessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = currentAccessToken != null && !currentAccessToken.isExpired

//        if (isLoggedIn) {
//            userId = currentAccessToken.userId
//            // 使用者先前已登入，直接抓取資料顯示
//            fetchUserInfoWithGraphApi(currentAccessToken, tv_facebook_user_name)
//        }

    }

    fun fetchUserInfoFromFirestore(name: String, password: String, onResult: (Boolean) -> Unit) {
        db.collection("User")
            .whereEqualTo("name", name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    //Toast.makeText(this, "$name 不存在，可以註冊。", Toast.LENGTH_LONG).show()
                    Log.v("JOYG", "JOYGSAY: name= $name 不存在")
                    onResult(false) // 透過 callback 回傳 false
                } else {
                    for (document in querySnapshot) {
                        // 假設 userId 欄位是字串型態
                        userId = document.getString("id").toString()
                        Log.v("JOYG", "找到使用者，userId = $userId")
                    }
                    onResult(true)  // 透過 callback 回傳 true
                }
            }
            .addOnFailureListener { e ->
                Log.v("JOYG", "Error: ${e.message}")
                onResult(false)
            }
    }

    private fun saveUserInfoToFirestore(userId:String, name: String, password: String) {
        Log.d("FirestoreDemo", "JOYGSAY: Call saveUserInfoToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val userInformation = hashMapOf(
            "id" to userId,
            "name" to name,
            "email" to "",
            "photoUrl" to "",
            "locale" to "",
            "heart" to 3,
            "heartContainer" to 3,
            "level" to 1,
            "password" to password
        )

        // 4. 指定集合名稱 "EnglishQuiz"，並自動產生文件 ID 新增資料 (.add)
        db.collection("User")
            .add(userInformation)
            .addOnSuccessListener { documentReference ->
                // 新增成功時的回呼
                Log.d("FirestoreDemo", "JOYGSAY: user Information 資料新增成功.")
                //Toast.makeText(this, "新增成功！ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 新增失敗時的回呼
                Log.w("FirestoreDemo", "JOYGSAY: user Information 新增資料時發生錯誤", e)
                //Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}