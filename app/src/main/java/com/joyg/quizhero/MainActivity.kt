package com.joyg.quizhero

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.joyg.quizhero.ui.theme.QuizHeroTheme

import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.alibaba.excel.EasyExcel
import java.io.InputStream


import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


private lateinit var firebaseAnalytics: FirebaseAnalytics

class MainActivity : ComponentActivity() {
    // 1. 宣告 FirebaseFirestore 變數
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // 2. 初始化 Firestore 實例
        db = Firebase.firestore

        val bt_add = findViewById<Button>(R.id.bt_add)
        val bt_delete = findViewById<Button>(R.id.bt_delete)

        bt_add.setOnClickListener {
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

                        //println("第 $rowIndex 行 - A欄: $colA, B欄: $colB")
                        saveDataToFirestore(colA, colB, colC)
                    },
                    onComplete = {
                        println("Excel 檔案全部讀取完成！")
                    }
                )
            }
            ////////////
        }

        bt_delete.setOnClickListener {
            deleteDataToFirestore()
        }

    }

    private fun deleteDataToFirestore(){
        db.collection("EnglishQuiz")
            //.whereEqualTo("name", "張小明")
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

    private fun saveDataToFirestore(ans: String, que: String, sol: String) {
        Log.d("FirestoreDemo", "Call saveDataToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val question = hashMapOf(
            "answer" to ans,
            "question" to que,
            "solution" to sol
        )

        // 4. 指定集合名稱 "users"，並自動產生文件 ID 新增資料 (.add)
        db.collection("EnglishQuiz")
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