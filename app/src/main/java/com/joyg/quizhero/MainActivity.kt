package com.joyg.quizhero


import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import com.facebook.CallbackManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private var tv_facebook_user_name: TextView?=null
private var userId : String = ""
// 1. 宣告 FirebaseFirestore 變數
private lateinit var db: FirebaseFirestore
private lateinit var sp_subject : Spinner
private lateinit var sp_volume : Spinner
private lateinit var sp_chapter : Spinner

private lateinit var adapter: LeaderboardAdapter

class MainActivity : ComponentActivity() {

    //登入臉書用
    private lateinit var callbackManager: CallbackManager

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


        var subjects = listOf("English","Chinese", "Geography")
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
            R.layout.my_spinner_item,
            subjects
        )
        // 設定下拉選單展開時的項目樣式（選填，同樣會套用置中效果）
        spSubjectAdapter.setDropDownViewResource(R.layout.my_spinner_item)
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
            R.layout.my_spinner_item,
            Volume
        )
        spVolumeAdapter.setDropDownViewResource(R.layout.my_spinner_item)
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
            R.layout.my_spinner_item,
            chapter
        )
        spChapterAdapter.setDropDownViewResource(R.layout.my_spinner_item)
        sp_chapter.adapter = spChapterAdapter


        bt_exam.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                val selectedSubject = sp_subject.selectedItem.toString()
                val selectedVolume = sp_volume.selectedItem.toString()
                val selectedChapter = sp_chapter.selectedItem.toString()
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
        fetchAndRefreshLeaderboard()
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
                    var totalCorrectCount = 0

                    Log.v("JOYG", "JOYGSAY: id=$id, name=$name")

                    // 3. 查詢該使用者的所有 Score 紀錄
                    val scoreSnapshots = db.collection("Score")
                        .whereEqualTo("user_id", id)
                        .get()
                        .await()

                    Log.v("JOYG", "JOYGSAY: score_count = ${scoreSnapshots.documents.size}")

                    // 4. 累加得分
                    for (scoreDoc in scoreSnapshots.documents) {
                        val correct = scoreDoc.getLong("correct")?.toInt() ?: 0
                        totalCorrectCount += correct
                    }

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
                    R.layout.my_spinner_item,
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
                    R.layout.my_spinner_item,
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
}

data class LeaderboardUser(
    var userId: String,
    var name: String,
    var correct: String,
    val rank: Int = 0 // 🌟 新增名次欄位
)

