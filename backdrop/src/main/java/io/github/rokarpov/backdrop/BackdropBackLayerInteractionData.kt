package io.github.rokarpov.backdrop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import java.lang.ref.WeakReference

class BackdropBackLayerInteractionData {
    companion object {
        private val ALPHA_VISIBLE = 1.0f
        private val ALPHA_HIDDEN = 0.0f

        fun showView(contentView: View) {
            contentView.alpha = BackdropBackLayerInteractionData.ALPHA_VISIBLE
            contentView.visibility = View.VISIBLE
        }
        fun hideView(contentView: View) {
            contentView.alpha = BackdropBackLayerInteractionData.ALPHA_HIDDEN
            contentView.visibility = View.INVISIBLE
        }

        fun addShowAnimator(
                animatorSet: AnimatorSet,
                view: View,
                delay: Long,
                duration: Long) {
            view.visibility = View.VISIBLE
            val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, ALPHA_VISIBLE)
            animator.startDelay = delay
            animator.duration = duration
            animator.interpolator = DecelerateInterpolator()
            animatorSet.play(animator)
        }
        fun addHideAnimator(
                animatorSet: AnimatorSet,
                view: View,
                delay: Long,
                duration: Long) {
            val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, ALPHA_HIDDEN)
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

        private val defaultContentAnimatorProvider = DefaultContentAnimatorProvider()
    }

    private val owner: WeakReference<BackdropBackLayer>
    private var backViewStrategy: BackdropBackLayerHeaderStrategy = BackdropBackLayerHeaderStrategy.DEFAULT
    private var actualContentProvider: ContentAnimatorProvider = defaultContentAnimatorProvider

    var hideHeader: Boolean
        get() {
            return backViewStrategy == BackdropBackLayerHeaderStrategy.HIDE_HEADER
        }
        set(value) {
            if (hideHeader == value) return
            field = value
            backViewStrategy =
                    if (value) {
                        BackdropBackLayerHeaderStrategy.HIDE_HEADER
                    } else {
                        BackdropBackLayerHeaderStrategy.DEFAULT
                    }
            owner.get()?.requestLayout()
        }
    var revealedFrontViewHeight: Int = 0
        set(value) {
            if (field == value) return
            field = value
            owner.get()?.requestLayout()
        }
    var inAnimationDuration: Long
    var outAnimationDuration: Long
    var contentAnimatorProvider: ContentAnimatorProvider? = null
        set(value) {
            if (field == value) return
            field = value
            actualContentProvider = if (value == null) defaultContentAnimatorProvider else value
        }

    internal constructor(owner: BackdropBackLayer, hideHeader: Boolean) {
        this.owner = WeakReference(owner)
        this.hideHeader = hideHeader
        this.revealedFrontViewHeight = revealedFrontViewHeight
        if (hideHeader) {
            inAnimationDuration = BackdropBackLayer.oneStepAnimationTime
            outAnimationDuration = BackdropBackLayer.oneStepAnimationTime
        } else {
            inAnimationDuration = BackdropBackLayer.fadeInTime
            outAnimationDuration = BackdropBackLayer.fadeOutTime
        }
    }

    internal fun onLayoutRevealedView(contentView:View, headerView: View, left: Int, top: Int, right: Int, bottom: Int) =
            this.backViewStrategy.onLayoutBackView(left, top, right, bottom, contentView, headerView)

    internal fun getContentViewVerticalOffset(headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView)
    }
    internal fun getLayoutRevealedHeight(contentView: View, headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView) + contentView.measuredHeight
    }


    internal fun addRevealHeaderAnimations(animatorSet: AnimatorSet, headerView: View): Long {
        backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, headerView, outAnimationDuration)
        return outAnimationDuration
    }
    internal fun addRevealHeaderAnimations(
            animatorSet: AnimatorSet,
            prevInteractionData: BackdropBackLayerInteractionData,
            view: View, prevDuration: Long
    ) {
        backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, view, prevDuration, inAnimationDuration, prevInteractionData.backViewStrategy)

    }
    internal fun addConcealHeaderAnimations(animatorSet: AnimatorSet, view: View, delay: Long): Long {
        backViewStrategy.addOnConcealHeaderViewAnimator(animatorSet, view, delay, inAnimationDuration)
        return inAnimationDuration
    }

    internal fun addRevealContentAnimations(animatorSet: AnimatorSet, view: View, delay: Long): Long {
        return actualContentProvider.addOnRevealAnimators(view, animatorSet, delay, inAnimationDuration)
    }
    internal fun addConcealContentAnimations(animatorSet: AnimatorSet, view: View): Long {
        return actualContentProvider.addOnConcealAnimators(view, animatorSet, 0, outAnimationDuration)
    }

    internal fun reveal(contentView: View, headerView: View) {
        backViewStrategy.updateHeaderOnReveal(headerView)
        showView(contentView)
    }
    internal fun reveal(contentView: View, headerView: View, prevContentView: View) {
        hideView(prevContentView)
        backViewStrategy.updateHeaderOnReveal(headerView)
        showView(contentView)
    }
    internal fun conceal(contentView: View, headerView: View) {
        backViewStrategy.updateHeaderOnConceal(headerView)
        hideView(contentView)
    }

    interface ContentAnimatorProvider {
        fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long
        fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long
    }

    private class DefaultContentAnimatorProvider: ContentAnimatorProvider {
        override fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            addShowAnimator(animatorSet, contentView, delay, duration)
            return duration
        }

        override fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            addHideAnimator(animatorSet, contentView, delay, duration)
            return duration
        }

    }
}