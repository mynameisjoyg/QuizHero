package com.joyg.quizhero

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class CardSwipeCallback(
    private val onSwipedAction: (position: Int, direction: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    // 當卡片滑出螢幕後觸發
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        onSwipedAction(position, direction)
    }

    // 自訂卡片滑動時的動態效果 (旋轉 + 透明度)
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val width = recyclerView.width.toFloat()

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // 計算滑動比例 (-1.0 到 1.0)
            val swipeRatio = dX / width

            // 1. 動態旋轉：根據滑動距離最多旋轉 15 度
            val maxRotation = 15f
            itemView.rotation = swipeRatio * maxRotation

            // 2. 動態漸變透明度：拉得越遠越透明 (最低 alpha 為 0.3)
            val alpha = 1f - (abs(dX) / width) * 0.7f
            itemView.alpha = alpha.coerceIn(0.3f, 1.0f)

            // 3. 預設水平位移
            itemView.translationX = dX
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    // 手放開/卡片復原時重置 View 的旋轉角度與透明度
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.rotation = 0f
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.translationX = 0f
    }
}