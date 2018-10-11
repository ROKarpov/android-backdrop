package io.github.rokarpov.backdrop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.view.View
import java.lang.ref.WeakReference

internal enum class BackdropBackLayerState {
    CONCEALED {
        override fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int {
            return 0
        }

        override fun onPrepare(layout: BackdropBackLayer) {}

        override fun onConceal(
                backLayer: BackdropBackLayer,
                viewToConceal: View, interactionData: BackdropBackLayerInteractionData,
                withAnimation: Boolean): Boolean {
            return true
        }

        override fun onReveal(backLayer: BackdropBackLayer,
                              viewToReveal: View, interactionData: BackdropBackLayerInteractionData,
                              withAnimation: Boolean): Boolean {
            backLayer.revealedView = viewToReveal
            backLayer.revealedViewInteractionData = interactionData
            backLayer.currentAnimator?.cancel()
            backLayer.state = REVEALED

            if (withAnimation) {
                val animatorSet = AnimatorSet()
                val delay =
                        interactionData.addRevealHeaderAnimations(animatorSet, backLayer.headerView)
                val contentViewAnimationDuration =
                        interactionData.addRevealContentAnimations(animatorSet, viewToReveal, delay)
                backLayer.addCustomRevealAnimators(animatorSet, contentViewAnimationDuration, delay)

                val weakBackLayer = WeakReference(backLayer)
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val strongBackLayer = weakBackLayer.get() ?: return
                        strongBackLayer.notifyReveal(viewToReveal)
                        strongBackLayer.currentAnimator = null
                    }
                })
                backLayer.currentAnimator = animatorSet
                animatorSet.start()
            } else {
                interactionData.reveal(viewToReveal, backLayer.headerView)
                backLayer.notifyReveal(viewToReveal)
            }
            return true
        }
    },
    REVEALED {
        override fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int {
            return if ((contentView != null) && (interactionData != null)) {
                headerView.measuredHeight - interactionData.getLayoutRevealedHeight(contentView, headerView)
            } else {
                0
            }
        }

        override fun onPrepare(layout: BackdropBackLayer) {
            val view = layout.revealedView
            val interactionData = layout.revealedViewInteractionData
            if ((view == null) || (interactionData == null)) return

            interactionData.reveal(view, layout.headerView)
        }

        override fun onConceal(
                backLayer: BackdropBackLayer,
                viewToConceal: View, interactionData: BackdropBackLayerInteractionData,
                withAnimation: Boolean): Boolean {

            backLayer.revealedView = null
            backLayer.revealedViewInteractionData = null
            backLayer.state = BackdropBackLayerState.CONCEALED
            backLayer.currentAnimator?.cancel()

            if (withAnimation) {
                val animatorSet = AnimatorSet()
                val outAnimationDuration =
                        interactionData.addConcealContentAnimations(animatorSet, viewToConceal)
                val inAnimationDuration =
                        interactionData.addConcealHeaderAnimations(animatorSet, backLayer.headerView, outAnimationDuration)
                backLayer.addCustomConcealAnimators(animatorSet, inAnimationDuration, outAnimationDuration)

                val weakBackLayer = WeakReference(backLayer)
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val strongBackLayer = weakBackLayer.get() ?: return
                        strongBackLayer.notifyConceal(viewToConceal)
                        strongBackLayer.currentAnimator = null
                    }
                })
                backLayer.currentAnimator = animatorSet
                animatorSet.start()

            } else {
                interactionData.conceal(viewToConceal, backLayer.headerView)
                backLayer.notifyConceal(viewToConceal)
            }
            return true
        }

        override fun onReveal(
                backLayer: BackdropBackLayer,
                viewToReveal: View, interactionData: BackdropBackLayerInteractionData,
                withAnimation: Boolean): Boolean {
            val prevView = backLayer.revealedView ?: return false
            val prevInteractionData = backLayer.revealedViewInteractionData ?: return false

            backLayer.revealedView = viewToReveal
            backLayer.revealedViewInteractionData = interactionData
            backLayer.currentAnimator?.cancel()

            if (withAnimation) {
                val animatorSet = AnimatorSet()
                val prevContentViewAnimationDuration =
                        prevInteractionData.addConcealContentAnimations(animatorSet, prevView)
                interactionData.addRevealHeaderAnimations(animatorSet, prevInteractionData, backLayer.headerView, prevContentViewAnimationDuration)
                val contentViewAnimationDuration =
                        interactionData.addRevealContentAnimations(animatorSet, viewToReveal, prevContentViewAnimationDuration)
                backLayer.addCustomRevealAnimators(animatorSet, prevContentViewAnimationDuration, contentViewAnimationDuration)

                val weakBackLayer = WeakReference(backLayer)
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val strongBackLayer = weakBackLayer.get() ?: return
                        strongBackLayer.notifyReveal(viewToReveal)
                        strongBackLayer.currentAnimator = null
                    }
                })
                backLayer.currentAnimator = animatorSet
                animatorSet.start()
            } else {
                interactionData.reveal(viewToReveal, backLayer.headerView, prevView)
                backLayer.notifyReveal(viewToReveal)
            }
            return true
        }
    };

    internal abstract fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int
    internal abstract fun onPrepare(layout: BackdropBackLayer)

    internal abstract fun onConceal(
            backLayer: BackdropBackLayer,
            viewToConceal: View, interactionData: BackdropBackLayerInteractionData,
            withAnimation: Boolean): Boolean

    internal abstract fun onReveal(
            backLayer: BackdropBackLayer,
            viewToReveal: View, interactionData: BackdropBackLayerInteractionData,
            withAnimation: Boolean): Boolean
}