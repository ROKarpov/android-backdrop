package io.github.rokarpov.backdrop

import android.animation.AnimatorSet
import android.view.View
import java.lang.ref.WeakReference

class BackdropBackLayerInteractionData {
    companion object {
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
            inAnimationDuration = BackdropBackLayer.fadeInTime
            outAnimationDuration = BackdropBackLayer.fadeOutTime
        } else {
            inAnimationDuration = BackdropBackLayer.oneStepAnimationTime
            outAnimationDuration = BackdropBackLayer.oneStepAnimationTime
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
    internal fun onPrepare(contentView: View) = actualContentProvider.onPrepare(contentView)


    internal fun addRevealHeaderAnimations(animatorSet: AnimatorSet, headerView: View): Long {
        return backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, headerView, outAnimationDuration)
    }
    internal fun addRevealHeaderAnimations(
            animatorSet: AnimatorSet,
            prevInteractionData: BackdropBackLayerInteractionData,
            view: View, prevDuration: Long
    ): Long {
        return backViewStrategy.addOnRevealHeaderViewAnimator(
                animatorSet, view,
                prevDuration, inAnimationDuration,
                prevInteractionData.backViewStrategy)
    }
    internal fun addConcealHeaderAnimations(animatorSet: AnimatorSet, view: View, delay: Long): Long {
        return backViewStrategy.addOnConcealHeaderViewAnimator(animatorSet, view, delay, inAnimationDuration)
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
        fun onPrepare(contentView: View)
        fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long
        fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long
    }

    private class DefaultContentAnimatorProvider: ContentAnimatorProvider {
        override fun onPrepare(contentView: View) {
            hideView(contentView)
        }

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