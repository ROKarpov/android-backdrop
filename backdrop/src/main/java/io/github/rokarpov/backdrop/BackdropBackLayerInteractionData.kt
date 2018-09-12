package io.github.rokarpov.backdrop

import android.animation.AnimatorSet
import android.view.View

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

    private val animConfig = AnimationConfig()
    private lateinit var backViewStrategy: BackdropBackLayerStrategy

    var hideHeader: Boolean
        get() {
            return backViewStrategy == BackdropBackLayerStrategy.HIDE_HEADER
        }
        set(value) {
            backViewStrategy =
                    if (value) {
                        BackdropBackLayerStrategy.HIDE_HEADER
                    } else {
                        BackdropBackLayerStrategy.DEFAULT
                    }
        }

    internal constructor(hideHeader: Boolean) {
        this.hideHeader = hideHeader
    }

    internal fun getContentViewVerticalOffset(headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView)
    }
    internal fun getLayoutRevealedHeight(contentView: View, headerView: View): Int {
        return backViewStrategy.getContentViewVerticalOffset(headerView) + contentView.measuredHeight
    }

    internal fun onLayoutRevealedView(contentView:View, headerView: View, left: Int, top: Int, right: Int, bottom: Int) =
            this.backViewStrategy.onLayoutBackView(left, top, right, bottom, contentView, headerView)

    //TODO: Integrate with states.
    internal fun addOnRevealAnimators(
            animatorSet: AnimatorSet, contentView: View, headerView: View,
            prevContentView: View?, prevInteractionData:BackdropBackLayerInteractionData?
    ): AnimationConfig {
        animConfig.reset()

        if (prevContentView != null) {
            backViewStrategy.addOnRevealedViewChangedContentAnimator(animatorSet, prevContentView, animConfig)
            if (prevInteractionData?.backViewStrategy != this.backViewStrategy) {
                this.backViewStrategy.addOnRevealedViewChangedHeaderAnimator(animatorSet, headerView, animConfig)
            }
        } else {
            backViewStrategy.addOnRevealHeaderAnimator(animatorSet, headerView, animConfig)
        }
        backViewStrategy.addOnRevealLayoutAnimator(animatorSet, contentView, animConfig)
        return animConfig
    }
    internal fun addOnConcealAnimators(animatorSet: AnimatorSet, contentView: View, headerView: View): AnimationConfig {
        animConfig.reset()

        backViewStrategy.addOnConcealHeaderAnimator(animatorSet, headerView, animConfig)
        backViewStrategy.addOnConcealContentAnimator(animatorSet, contentView, animConfig)

        return animConfig
    }

    internal fun reveal(contentView: View, headerView: View, prevContentView: View?) {
        if (prevContentView != null) {
            hideView(prevContentView)
        }
        backViewStrategy.updateHeaderOnReveal(headerView)
        showView(contentView)
    }
    internal fun conceal(contentView: View, headerView: View) {
        backViewStrategy.updateHeaderOnConceal(headerView)
        hideView(contentView)
    }
}