package io.github.rokarpov.backdrop

import android.animation.*
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

internal enum class BackdropBackLayerStrategy {
    HIDE_HEADER{
        override fun getContentViewVerticalOffset(defaultBackView: View): Int {
            return 0;
        }
        override fun onLayoutBackView(
                left: Int, top: Int, right: Int, bottom: Int,
                contentView: View, headerView: View) {
            contentView.layout(left, top, left + contentView.measuredWidth, top + contentView.measuredHeight)
        }

        override fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, prevViewDuration: Long, duration: Long, prevStrategy: BackdropBackLayerStrategy) {
            if (this == prevStrategy) return
            addHideAnimator(animatorSet, headerView, 0, prevViewDuration)
        }

        override fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, duration: Long) {
            addHideAnimator(animatorSet, headerView, 0, duration)
        }

        override fun addOnConcealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, delay: Long, duration: Long) {
            addShowAnimator(animatorSet, headerView, delay, duration)
        }

        override fun updateHeaderOnReveal(headerView: View) {
            headerView.alpha = 0.0f
            headerView.visibility = View.INVISIBLE
        }

        override fun updateHeaderOnConceal(headerView: View) {
            headerView.visibility = View.VISIBLE
            headerView.alpha = 1.0f
        }
    },
    DEFAULT{
        override fun getContentViewVerticalOffset(defaultBackView: View): Int {
            return defaultBackView.measuredHeight
        }
        override fun onLayoutBackView(
                left: Int, top: Int, right: Int, bottom: Int,
                contentView: View, headerView: View) {
            val viewRight = left + contentView.measuredWidth
            val viewTop = top + headerView.measuredHeight
            val viewBottom = viewTop + contentView.measuredHeight
            contentView.layout(left, viewTop, viewRight, viewBottom)
        }

        override fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, prevViewDuration: Long, duration: Long, prevStrategy: BackdropBackLayerStrategy) {
            if (this == prevStrategy) return
            addShowAnimator(animatorSet, headerView, prevViewDuration, duration)
        }
        override fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, duration: Long) { }
        override fun addOnConcealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, delay: Long, duration: Long) { }
    };

    abstract fun getContentViewVerticalOffset(defaultBackView: View): Int
    abstract fun onLayoutBackView(
            left: Int, top: Int, right: Int, bottom: Int,
            contentView: View, headerView: View)


    abstract fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, prevViewDuration: Long, duration: Long, prevStrategy: BackdropBackLayerStrategy)
    abstract fun addOnRevealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, duration: Long)
    abstract fun addOnConcealHeaderViewAnimator(animatorSet: AnimatorSet, headerView: View, delay: Long, duration: Long)

    fun addOnRevealContentViewAnimator(animatorSet: AnimatorSet, contentView: View, delay: Long, duration: Long) {
        addShowAnimator(animatorSet, contentView, delay, duration)
    }
    fun addOnConcealContentViewAnimator(animatorSet: AnimatorSet, contentView: View, duration: Long) {
        addHideAnimator(animatorSet, contentView, 0, duration)
    }

    open fun updateHeaderOnReveal(headerView: View) { }
    open fun updateHeaderOnConceal(headerView: View) { }

    protected fun addShowAnimator(
            animatorSet: AnimatorSet,
            view: View,
            delay: Long,
            duration: Long) {
        view.visibility = View.VISIBLE
        // TODO: add constant
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 1.0f)
        animator.startDelay = delay
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        animatorSet.play(animator)
    }
    protected fun addHideAnimator(
            animatorSet: AnimatorSet,
            view: View,
            delay: Long,
            duration: Long) {
        // TODO: add constant
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0.0f)
        animator.startDelay = delay
        animator.duration = duration
        animator.interpolator = AccelerateInterpolator()
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                view.visibility = View.INVISIBLE
            }
        })
        animatorSet.play(animator)
    }
}