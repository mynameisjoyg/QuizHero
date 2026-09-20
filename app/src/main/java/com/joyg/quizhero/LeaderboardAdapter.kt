package com.joyg.quizhero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter : ListAdapter<LeaderboardUser, LeaderboardAdapter.LeaderboardViewHolder>(DiffCallback) {

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

        fun bind(user: LeaderboardUser, position: Int) {
            // 名次為目前位置 + 1
            tvRank.text = "${user.rank}" // 🌟 改用 user 物件內的 rank
            tvName.text = user.name
            tvScore.text = "答對 ${user.correct} 題"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_user, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    // 比對新舊排行榜資料，確保效能與動畫平滑
    companion object DiffCallback : DiffUtil.ItemCallback<LeaderboardUser>() {
        override fun areItemsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean {
            return oldItem == newItem
        }
    }
}