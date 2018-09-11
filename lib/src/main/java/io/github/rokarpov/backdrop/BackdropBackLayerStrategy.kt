package io.github.rokarpov.backdrop

import android.animation.*
import android.view.View

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

        override fun calculateFrontViewOffset(contentView: View, headerView: View): Float =
                (contentView.bottom - headerView.bottom).toFloat()

        override fun addOnRevealedViewChangedHeaderAnimator(animatorSet: AnimatorSet, headerView: View, animationConfig: AnimationConfig) =
                addOnRevealHeaderAnimator(animatorSet, headerView, animationConfig)

        override fun addOnRevealHeaderAnimator(animatorSet: AnimatorSet, headerView: View, animationConfig: AnimationConfig) {
            animationConfig.updateForTwoStepAnimation()
            addHideAnimator(
                    animatorSet,
                    headerView,
                    animationConfig.fadeOutDelay,
                    animationConfig.fadeOutDuration,
                    animationConfig.fadeOutInterpolator)
        }

        override fun addOnConcealHeaderAnimator(animatorSet: AnimatorSet, headerView: View, animationConfig: AnimationConfig) {
            animationConfig.updateForTwoStepAnimation()
            addShowAnimator(
                    animatorSet,
                    headerView,
                    animationConfig.fadeInDelay,
                    animationConfig.fadeInDuration,
                    animationConfig.fadeInInterpolator)
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

        override fun calculateFrontViewOffset(contentView: View, headerView: View): Float =
                contentView.measuredHeight.toFloat()

        override fun addOnRevealedViewChangedHeaderAnimator(
                animatorSet: AnimatorSet,
                headerView: View,
                animationConfig: AnimationConfig) {
            addShowAnimator(
                    animatorSet,
                    headerView,
                    animationConfig.fadeInDelay,
                    animationConfig.fadeInDuration,
                    animationConfig.fadeInInterpolator)
        }

        override fun addOnRevealHeaderAnimator(animatorSet: AnimatorSet, headerView: View, animationConfig: AnimationConfig) { }
        override fun addOnConcealHeaderAnimator(animatorSet: AnimatorSet, headerView: View, animationConfig: AnimationConfig) { }
    };

    abstract fun getContentViewVerticalOffset(defaultBackView: View): Int
    abstract fun onLayoutBackView(
            left: Int, top: Int, right: Int, bottom: Int,
            contentView: View, headerView: View)

    abstract fun calculateFrontViewOffset(contentView: View, headerView: View): Float

    abstract fun addOnRevealedViewChangedHeaderAnimator(
            animatorSet: AnimatorSet, headerView: View,
            animationConfig: AnimationConfig)
    fun addOnRevealedViewChangedContentAnimator(
            animatorSet: AnimatorSet, contentView: View,
            animationConfig: AnimationConfig) {
        animationConfig.updateForTwoStepAnimation()
        addHideAnimator(animatorSet,
                contentView,
                animationConfig.fadeOutDelay,
                animationConfig.fadeOutDuration,
                animationConfig.fadeOutInterpolator)
    }

    abstract fun addOnRevealHeaderAnimator(
            animatorSet: AnimatorSet, headerView: View,
            animationConfig: AnimationConfig)
    abstract fun addOnConcealHeaderAnimator(
            animatorSet: AnimatorSet, headerView: View,
            animationConfig: AnimationConfig)

    fun addOnRevealLayoutAnimator(
            animatorSet: AnimatorSet, contentView: View,
            animationConfig: AnimationConfig) {
        addShowAnimator(
                animatorSet,
                contentView,
                animationConfig.fadeInDelay,
                animationConfig.fadeInDuration,
                animationConfig.fadeInInterpolator)
    }
    fun addOnConcealContentAnimator(
            animatorSet: AnimatorSet, contentView: View,
            animationConfig: AnimationConfig) {
        addHideAnimator(
                animatorSet,
                contentView,
                animationConfig.fadeOutDelay,
                animationConfig.fadeOutDuration,
                animationConfig.fadeOutInterpolator)
    }

    open fun updateHeaderOnReveal(headerView: View) { }
    open fun updateHeaderOnConceal(headerView: View) { }

    protected fun addShowAnimator(
            animatorSet: AnimatorSet,
            view: View,
            delay: Long,
            duration: Long,
            interpolator: TimeInterpolator) {
        view.visibility = View.VISIBLE
        // TODO: add constant
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 1.0f)
        animator.startDelay = delay
        animator.duration = duration
        animator.interpolator = interpolator
        animatorSet.play(animator)
    }
    protected fun addHideAnimator(
            animatorSet: AnimatorSet,
            view: View,
            delay: Long,
            duration: Long,
            interpolator: TimeInterpolator) {
        // TODO: add constant
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0.0f)
        animator.startDelay = delay
        animator.duration = duration
        animator.interpolator = interpolator
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                view.visibility = View.INVISIBLE
            }
        })
        animatorSet.play(animator)
    }
}