package com.joyg.quizhero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(private val items: MutableList<String>) :
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
        holder.tvTitle.text = items[position]
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