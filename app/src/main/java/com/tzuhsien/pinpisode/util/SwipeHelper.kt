package com.tzuhsien.pinpisode.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber
import java.util.*
import kotlin.math.abs


abstract class SwipeHelper(
    private val recyclerView: RecyclerView,
    private val swipeOutListener: OnSwipeOutListener,
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.ACTION_STATE_IDLE,
    ItemTouchHelper.LEFT
) {
    private var swipedPosition = -1
    private val buttonsBuffer: MutableMap<Int, List<UnderlayButton>> = mutableMapOf()
    private val recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
        if (swipedPosition < 0) return@OnTouchListener false
        buttonsBuffer[swipedPosition]?.forEach { it.handle(event) }
        recoverQueue.add(swipedPosition)
        swipedPosition = -1
        recoverSwipedItem()
        true
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    private fun recoverSwipedItem() {
        Timber.d("recoverSwipedItem")
        while (!recoverQueue.isEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
            Timber.d("recoverSwipedItem, recoverQueue = $position")
        }
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<UnderlayButton>,
        itemView: View,
        dX: Float,
    ) {
        var right = itemView.right
        Timber.d("itemView.right = ${itemView.right}")
        buttons.forEach { button ->
            Timber.d("width = ${button.intrinsicWidth / buttons.intrinsicWidth() * abs(dX)}")
            Timber.d("button.intrinsicWidth: ${button.intrinsicWidth}/buttons.intrinsicWidth(): ${buttons.intrinsicWidth()}")
            val width = button.intrinsicWidth / buttons.intrinsicWidth() * abs(dX)
            val left = right - width
            Timber.d("left = $left")
            button.draw(
                canvas,
                RectF(left, itemView.top.toFloat(), right.toFloat(), itemView.bottom.toFloat())
            )

            right = left.toInt()
            Timber.d("right = $right")
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val position = viewHolder.adapterPosition
        var maxDX = dX
        val itemView = viewHolder.itemView

        Timber.w("actionState = $actionState")

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            Timber.d("dX = $dX ")
            if (dX < 0) {
                if (!buttonsBuffer.containsKey(position)) {

                    Timber.d("!buttonsBuffer.containsKey(position): ${
                        instantiateUnderlayButton(position)
                    }")
                    buttonsBuffer[position] = instantiateUnderlayButton(position)
                }

                val buttons = buttonsBuffer[position] ?: return
                if (buttons.isEmpty()) return
//                maxDX = max(-buttons.intrinsicWidth(), dX)

                drawButtons(c, buttons, itemView, dX)

                Timber.d("position: $position,swipedPosition:  $swipedPosition, viewHolder.adapterPosition: ${viewHolder.adapterPosition} ")

                if (dX == -1080F && !isCurrentlyActive && swipedPosition != -1) {
                    Timber.d("dX == -1080F && !isCurrentlyActive> position: $position,swipedPosition:  $swipedPosition")
                    swipeOutListener.onSwipeOut(swipedPosition)
                    swipedPosition = -1
                    Timber.d("swipeOutListener.onSwipeOut(swipedPosition)")
                }
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            maxDX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        Timber.d("onMove")
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position

        recoverSwipedItem()
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    class OnSwipeOutListener(val swipeOutListener: (position: Int) -> Unit) {
        fun onSwipeOut(position: Int) = swipeOutListener(position)
    }

    abstract fun instantiateUnderlayButton(position: Int): List<UnderlayButton>

    //region UnderlayButton
    interface UnderlayButtonClickListener {
        fun onClick()
    }

    class UnderlayButton(
        private val context: Context,
        private val title: String,
        textSize: Float,
        @ColorRes private val colorRes: Int,
        private val clickListener: UnderlayButtonClickListener,
    ) {
        private var clickableRegion: RectF? = null
        private val textSizeInPixel: Float =
            textSize * context.resources.displayMetrics.density // dp to px
        private val horizontalPadding = 100.0f
        val intrinsicWidth: Float

        init {
            val paint = Paint()
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            val titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)
            intrinsicWidth = titleBounds.width() + 2 * horizontalPadding
        }

        fun draw(canvas: Canvas, rect: RectF) {
            val paint = Paint()

            Timber.d("class UnderlayButton draw")
            // Draw background
            paint.color = ContextCompat.getColor(context, colorRes)
            canvas.drawRect(rect, paint)

            // Draw title
            paint.color = ContextCompat.getColor(context, android.R.color.white)
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER

            val titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)

            val y = rect.height() / 2 + titleBounds.height() / 2 - titleBounds.bottom
            canvas.drawText(title, rect.left + horizontalPadding, rect.top + y, paint)

            clickableRegion = rect
        }

        fun handle(event: MotionEvent) {
//            clickableRegion?.let {
//                if (it.contains(event.x, event.y)) {
//                    clickListener.onClick()
//                }
//            }
        }
    }
}

private fun List<SwipeHelper.UnderlayButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return map { it.intrinsicWidth }.reduce { acc, fl -> acc + fl }
}