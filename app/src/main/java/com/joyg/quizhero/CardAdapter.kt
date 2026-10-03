package com.joyg.quizhero

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        // 紀錄當前選中的位置 (-1 表示無選中)
        var selectedPosition = -1
        val item = items[position]
        holder.tvTitle.text = item

        val cardView = holder.itemView as com.google.android.material.card.MaterialCardView

        // 設定卡片是否為選中 (反白) 狀態
        cardView.isChecked = (position == selectedPosition)

        // 點擊事件：切換反白
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition

            // 刷新舊項目與新項目以切換反白效果
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            val realPosition = holder.adapterPosition
            if (realPosition != RecyclerView.NO_POSITION) {

                // 根據 position 對應指定的科目名稱
                val subject = when (realPosition % 3) { // 使用 % 3 可支援循環卡片列表
                    0 -> "English"
                    1 -> "Chinese"
                    2 -> "Geography"
                    else -> "English"
                }

                // 將結果回傳給 Activity
                onSubjectSelected(subject)
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