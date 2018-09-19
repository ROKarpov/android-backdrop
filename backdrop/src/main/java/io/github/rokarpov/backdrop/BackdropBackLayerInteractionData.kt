package io.github.rokarpov.backdrop

import android.animation.AnimatorSet
import android.view.View
import java.lang.ref.WeakReference

// TODO: Add capability to specify reveal/conceal duration.
class BackdropBackLayerInteractionData {
    companion object {
        private val ALPHA_VISIBLE = 1.0f
        private val ALPHA_HIDDEN = 0.0f

        internal fun showView(contentView: View) {
            contentView.alpha = BackdropBackLayerInteractionData.ALPHA_VISIBLE
            contentView.visibility = View.VISIBLE
        }
        internal fun hideView(contentView: View) {
            contentView.alpha = BackdropBackLayerInteractionData.ALPHA_HIDDEN
            contentView.visibility = View.INVISIBLE
        }
    }

    private val owner: WeakReference<BackdropBackLayer>
    private lateinit var backViewStrategy: BackdropBackLayerStrategy

    var hideHeader: Boolean
        get() {
            return backViewStrategy == BackdropBackLayerStrategy.HIDE_HEADER
        }
        set(value) {
//            if (field == value) return
//            field = value
            backViewStrategy =
                    if (value) {
                        BackdropBackLayerStrategy.HIDE_HEADER
                    } else {
                        BackdropBackLayerStrategy.DEFAULT
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

    fun addRevealHeaderAnimations(animatorSet: AnimatorSet, headerView: View): Long {
        //backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, headerView)
        backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, headerView, outAnimationDuration)
        return outAnimationDuration
    }
    fun addRevealHeaderAnimations(
            animatorSet: AnimatorSet,
            prevInteractionData: BackdropBackLayerInteractionData,
            view: View, prevDuration: Long
    ) {
        backViewStrategy.addOnRevealHeaderViewAnimator(animatorSet, view, prevDuration, inAnimationDuration, prevInteractionData.backViewStrategy)

    }
    // TODO: Move addHideAnimator/addShowAnimator to this class.
    fun addRevealContentAnimations(animatorSet: AnimatorSet, view: View, delay: Long): Long {
        backViewStrategy.addOnRevealContentViewAnimator(animatorSet, view, delay, inAnimationDuration)
        return inAnimationDuration
    }
    fun addConcealHeaderAnimations(animatorSet: AnimatorSet, view: View, delay: Long): Long {
        backViewStrategy.addOnConcealHeaderViewAnimator(animatorSet, view, delay, inAnimationDuration)
        return inAnimationDuration
    }
    fun addConcealContentAnimations(animatorSet: AnimatorSet, view: View): Long {
        backViewStrategy.addOnConcealContentViewAnimator(animatorSet, view, outAnimationDuration)
        return outAnimationDuration
    }

    internal fun getContentViewVerticalOffset(headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView)
    }
    internal fun getLayoutRevealedHeight(contentView: View, headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView) + contentView.measuredHeight
    }

    internal fun onLayoutRevealedView(contentView:View, headerView: View, left: Int, top: Int, right: Int, bottom: Int) =
            this.backViewStrategy.onLayoutBackView(left, top, right, bottom, contentView, headerView)

    internal fun reveal(contentView: View, headerView: View) {
        backViewStrategy.updateHeaderOnReveal(headerView)
        showView(contentView)
    }
    internal fun reveal(contentView: View, headerView: View, prevContentView: View, prevInteractionData: BackdropBackLayerInteractionData) {
        hideView(prevContentView)
        backViewStrategy.updateHeaderOnReveal(headerView)
        showView(contentView)
    }
    internal fun conceal(contentView: View, headerView: View) {
        backViewStrategy.updateHeaderOnConceal(headerView)
        hideView(contentView)
    }
}