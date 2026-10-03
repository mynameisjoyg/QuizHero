package com.joyg.quizhero

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

class CardAdapter(
    private val items: MutableList<String>,
    // 1. 新增 Callback 參數，傳出選中的科目名稱 (String)
    private val onSubjectSelected: (subject: String) -> Unit) :
    RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    // 紀錄當前選中的位置 (-1 表示無選中)
    var selectedPosition = -1
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {

        val item = items[position]
        holder.tvTitle.text = item

        val cardView = holder.itemView as com.google.android.material.card.MaterialCardView

        // 關鍵 2：根據 selectedPosition 判斷當前卡片是否為選中狀態
        val isSelected = (position == selectedPosition)
        cardView.isChecked = isSelected

        // 關鍵 3：依選中狀態明確設定背景顏色 (選中變白色，未選中顯示灰色)
        if (isSelected) {
            cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0")) // 選中：預設灰色
        } else {
            cardView.setCardBackgroundColor(Color.WHITE) // 未選中/點擊後：反白 (白色)
        }

// 點擊事件：切換反白與讀取題庫
        holder.itemView.setOnClickListener {
            val realPosition = holder.adapterPosition

            // 1. 確保 position 有效才進行後續處理
            if (realPosition != RecyclerView.NO_POSITION) {

                // 2. 記錄舊的選中位置
                val previousSelected = selectedPosition

                // 3. 更新 selectedPosition（支援點擊切換：若點擊已選中的則切換回 -1，否則切換為當前 position）
                selectedPosition = if (selectedPosition == realPosition) -1 else realPosition

                // 4. 先處理科目邏輯與觸發題庫讀取 (確保 callback 順利執行)
                val subject = when (realPosition % 3) {
                    0 -> "English"
                    1 -> "Chinese"
                    2 -> "Geography"
                    else -> "English"
                }
                onSubjectSelected(subject)

                // 5. 最後才更新 UI 畫面（加入安全邊界檢查，避免 -1 導致 crash）
                if (previousSelected != RecyclerView.NO_POSITION && previousSelected < itemCount) {
                    notifyItemChanged(previousSelected)
                }
                if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < itemCount) {
                    notifyItemChanged(selectedPosition)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    /**
     * 將指定位置的卡片移到列表最底部
     */
    fun moveToBottom(fromPosition: Int) {
        if (fromPosition < 0 || fromPosition >= items.size) return

        // 1. 取出並移出資料
        val movedItem = items.removeAt(fromPosition)

        // 2. 通知 RecyclerView「頂層卡片已移除」
        // 這會立刻觸發第二張卡片平滑「由下往上推升」補位的動畫！
        notifyItemRemoved(fromPosition)

        // 3. 將資料加到最末端
        items.add(movedItem)

        // 4. 通知 RecyclerView「末端新增了一張卡片」
        notifyItemInserted(items.size - 1)
    }
}