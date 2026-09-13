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
            ////呼叫新增資料的方法
            saveDataToFirestore()
        }

        bt_delete.setOnClickListener {
            deleteDataToFirestore()
        }

    }

    private fun deleteDataToFirestore(){
        db.collection("users")
            .whereEqualTo("name", "張小明")
            .get()
            .addOnSuccessListener {
                querySnapshot ->
                if(querySnapshot.isEmpty) {
                    Toast.makeText(this, "找不到張小明", Toast.LENGTH_SHORT).show()
                }
                val totalCount = querySnapshot.size()
                var deleteCount = 0
                querySnapshot.documents.forEach { document ->
                    document.reference.delete()
                        .addOnSuccessListener {
                            deleteCount ++
                            Log.d("FirestoreDemo", "成功刪除單筆文件ID: ${document.id}")

                            if(deleteCount == totalCount) {
                                Toast.makeText(this, "已成功刪除張小明資料共${deleteCount}筆",
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

    private fun saveDataToFirestore() {
        Log.d("FirestoreDemo", "Call saveDataToFirestore.")
        // 建立要傳入 Firestore 的資料 (HashMap 結構)
        val user = hashMapOf(
            "name" to "張小明",
            "age" to 25,
            "email" to "xiaoming@example.com"
        )

        // 4. 指定集合名稱 "users"，並自動產生文件 ID 新增資料 (.add)
        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                // 新增成功時的回呼
                Log.d("FirestoreDemo", "資料新增成功，ID: ${documentReference.id}")
                Toast.makeText(this, "新增成功！ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // 新增失敗時的回呼
                Log.w("FirestoreDemo", "新增資料時發生錯誤", e)
                Toast.makeText(this, "新增失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
