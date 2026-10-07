package com.joyg.quizhero


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.CallbackManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback

private var tv_facebook_user_name: TextView?=null
private var userId : String = ""
private var userTotalCorrectCount: Int = 0
private var userTotalWrongCount: Int = 0
// 1. 宣告 FirebaseFirestore 變數
private lateinit var db: FirebaseFirestore

private lateinit var adapter: LeaderboardAdapter

class MainActivity : ComponentActivity() {

    //登入臉書用
    private lateinit var callbackManager: CallbackManager

    //Swipe Card
    private val itemList = mutableListOf("English", "Chinese", "Geography")
    private lateinit var cardAdapter: CardAdapter
    var selectedSubject = "English"
    var selectedVolume = "1"
    var selectedChapter = "1"

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "MainActivityAdMob"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // 1. 初始化 AdMob SDK
        MobileAds.initialize(this) {}

        // 2. 預先載入插頁式廣告
        loadInterstitialAd()



        val swipeCardRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        cardAdapter = CardAdapter(itemList) { subject ->
            // 這裡會接收到 "English", "Chinese", 或 "Geography"
            this.selectedSubject = subject
            this.selectedVolume ="2"
            this.selectedChapter ="1"

            // (選用) Toast 提示確認更新結果
            Toast.makeText(this, "選擇科目為: $selectedSubject", Toast.LENGTH_SHORT).show()
        }

        swipeCardRecyclerView.layoutManager = LinearLayoutManager(this)
        swipeCardRecyclerView.adapter = cardAdapter

        // 設定並附加 ItemTouchHelper
        val swipeCallback = CardSwipeCallback { position, direction ->
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    //Toast.makeText(this, "通過/保留: ${itemList[position]}", Toast.LENGTH_SHORT).show()
                }
                ItemTouchHelper.LEFT -> {
                    //Toast.makeText(this, "剔除/不喜歡: ${itemList[position]}", Toast.LENGTH_SHORT).show()
                }
            }
            //cardAdapter.notifyItemRemoved(position)
            cardAdapter.moveToBottom(position)
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(swipeCardRecyclerView)


        val bt_exam: Button = findViewById(R.id.bt_exam)
        val bt_battle: Button = findViewById(R.id.bt_battle)


        //Firestore
        // 2. 初始化 Firestore 實例
        db = Firebase.firestore


        var subjects = listOf("English","Chinese", "Geography")
        var Volume = listOf("1","2","3","4","5")
        var chapter = listOf("1","2","3","4")
        lateinit var spSubjectAdapter : ArrayAdapter<Any?>
        lateinit var spVolumeAdapter : ArrayAdapter<Any?>
        lateinit var spChapterAdapter : ArrayAdapter<Any?>

        //sp_subject
        spSubjectAdapter = ArrayAdapter(
            this,
            R.layout.my_spinner_item,
            subjects
        )


        bt_exam.setOnClickListener {

            showInterstitialAdAndProceed {

                // 這裡放您原本點擊按鈕後要執行的邏輯 (例如：切換到考試 Activity)
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("userId", "${userId}")
                    putExtra("subject", "${selectedSubject}")
                    putExtra("volume", "${selectedVolume}")
                    putExtra("chapter", "${selectedChapter}")
                    Log.v("JOYG", "JOYGSAY: putExtra, subject=$selectedSubject, volume=$selectedVolume, chapter=$selectedChapter")
                    setPackage(packageName)
                }
                startActivity(intent)

            }
        }

        bt_battle.setOnClickListener {
            val intent = Intent(this, BattleActivity::class.java).apply {
                putExtra("userId", "${userId}")
                putExtra("subject", "${selectedSubject}")
                putExtra("volume", "${selectedVolume}")
                putExtra("chapter", "${selectedChapter}")
                Log.v("JOYG", "JOYGSAY: putExtra, subject=$selectedSubject, volume=$selectedVolume, chapter=$selectedChapter")
                setPackage(packageName)
            }
            startActivity(intent)
        }
        //登入臉書用
        var name = intent.getStringExtra("name") ?: "未登入"
        userId = intent.getStringExtra("userId") ?: "未登入"
        tv_facebook_user_name = findViewById<TextView>(R.id.tv_facebook_user_name)
        tv_facebook_user_name?.text = "歡迎， $name"
        tv_facebook_user_name?.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("name", name)
                putExtra("totalCorrectCount", userTotalCorrectCount)
                putExtra("totalWrongCount", userTotalWrongCount)
                setPackage(packageName)
            }
            startActivity(intent)
        }

        //Leaderboard setting
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLeaderboard)
        adapter = LeaderboardAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 模擬從資料庫或伺服器獲取的排行榜資料
        fetchAndRefreshLeaderboard()
    }

    /**
     * 預先載入插頁廣告
     */
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        // 測試用插頁廣告 Unit ID: ca-app-pub-3940256099942544/1033173712
        // 正式上架請替換為您在 AdMob 後台建立的 Interstitial Ad Unit ID
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
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



    private fun fetchAndRefreshLeaderboard() {
        // 1. 使用 lifecycleScope 啟動協程
        lifecycleScope.launch {
            try {
                // 抓取所有 User 資料 (使用 await 等待結果)
                val userSnapshots = db.collection("User").get().await()
                val newLeaderboardList = mutableListOf<LeaderboardUser>()

                Log.v("JOYG", "JOYGSAY: user_count = ${userSnapshots.documents.size}")

                // 2. 逐一取出使用者
                for (userDoc in userSnapshots.documents) {
                    val id = userDoc.getString("id") ?: continue
                    val name = userDoc.getString("name") ?: "Unknown"
                    var totalCorrectCount = userDoc.getLong("totalCorrectCount")
                        ?: userDoc.getLong("totalCorrectCount")
                        ?: 0L
                    var totalWrongCount = userDoc.getLong("totalWrongCount")
                        ?: userDoc.getLong("totalWrongCount")
                        ?: 0L

                    if (id == userId) {
                        userTotalCorrectCount = totalCorrectCount.toInt()
                        userTotalWrongCount = totalWrongCount.toInt()
                    }

                    Log.v("JOYG", "JOYGSAY: id=$id, name=$name, totalCorrectCount=${totalCorrectCount}, totalWrongCount=\${totalWrongCount}")

                    Log.v("JOYG", "JOYGSAY: name=$name, totalCorrectCount=$totalCorrectCount")

                    // 5. 將該位使用者的總分加入暫存列表
                    newLeaderboardList.add(LeaderboardUser(id, name, totalCorrectCount.toString()))

                }

                // 6. 依照答對題數由高到低排序排行榜
                // 1. 先按分數降冪排序
                val sortedByScore = newLeaderboardList.sortedByDescending { it.correct }
                // 2. 🌟 加上名次 (index + 1) 並產生新的 LeaderboardUser 物件
                val finalLeaderboard = sortedByScore.mapIndexed { index, user ->
                    user.copy(rank = index + 1)
                }

                // 7. 更新 RecyclerView
                adapter.submitList(finalLeaderboard)

            } catch (e: Exception) {
                Log.e("JOYG", "Error fetching leaderboard", e)
                Toast.makeText(this@MainActivity, "獲取排行榜資料失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        // 每次畫面重新呈現時（包含從 QuizActivity 返回），自動刷新排行榜
        fetchAndRefreshLeaderboard()
    }

    // 4. 將 Intent 結果傳遞給 Facebook SDK CallbackManager
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
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
}

data class LeaderboardUser(
    var userId: String,
    var name: String,
    var correct: String,
    val rank: Int = 0 // 🌟 新增名次欄位
)

