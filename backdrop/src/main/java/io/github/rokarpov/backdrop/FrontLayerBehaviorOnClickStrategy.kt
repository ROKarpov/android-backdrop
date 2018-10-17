package io.github.rokarpov.backdrop

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import java.lang.ref.WeakReference

interface FrontLayerBehaviorOnClickStrategy {
    fun setBackLayer(backLayer: BackdropBackLayer)
    fun onInterceptTouchEvent(child: View, ev: MotionEvent): Boolean
    fun onTouchEvent(child: View, ev: MotionEvent): Boolean
}


class ConcealOnClickFrontLayerBehaviorOnClickStrategy : FrontLayerBehaviorOnClickStrategy {
    private lateinit var backLayer: BackdropBackLayer
    private val gestureDetectorCompat: GestureDetectorCompat = GestureDetectorCompat(null, FrontViewGestureListener(this))


    override fun setBackLayer(backLayer: BackdropBackLayer) {
        this.backLayer = backLayer
    }

    override fun onInterceptTouchEvent(child: View, ev: MotionEvent): Boolean {
        return (backLayer.state == BackdropBackLayerState.REVEALED) && isTouchInView(child, ev)
    }

    override fun onTouchEvent(child: View, ev: MotionEvent): Boolean {
        return gestureDetectorCompat.onTouchEvent(ev)
    }

    private fun isTouchInView(view: View, e: MotionEvent): Boolean {
        val top = view.top + view.translationY
        val left = view.left + view.translationX
        val bottom = view.bottom + view.translationY
        val right = view.right + view.translationX

        val x = e.x
        val y = e.y

        return ((y >= top) && (x >= left) && (y <= bottom) && (x <= right))
    }


    class FrontViewGestureListener(
            strategy: ConcealOnClickFrontLayerBehaviorOnClickStrategy
    ) : GestureDetector.SimpleOnGestureListener() {
        val owner = WeakReference<ConcealOnClickFrontLayerBehaviorOnClickStrategy>(strategy)

        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean = onTap()
        override fun onSingleTapUp(e: MotionEvent?): Boolean = onTap()

        private fun onTap(): Boolean {
            owner.get()?.let { it.backLayer.concealBackView() }
            return true
        }
    }
}

object NotConcealOnClickFrontLayerBehaviorOnClickStrategy : FrontLayerBehaviorOnClickStrategy {
    override fun setBackLayer(backLayer: BackdropBackLayer) {}
    override fun onInterceptTouchEvent(child: View, ev: MotionEvent): Boolean = false
    override fun onTouchEvent(child: View, ev: MotionEvent): Boolean = false
}