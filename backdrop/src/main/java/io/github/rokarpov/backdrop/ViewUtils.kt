package io.github.rokarpov.backdrop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

const val ALPHA_VISIBLE = 1.0f
const val ALPHA_HIDDEN = 0.0f

inline fun showView(contentView: View) {
    contentView.alpha = ALPHA_VISIBLE
    contentView.visibility = View.VISIBLE
}
inline fun hideView(contentView: View) {
    contentView.alpha = ALPHA_HIDDEN
    contentView.visibility = View.INVISIBLE
}

inline fun addShowAnimator(
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
inline fun addHideAnimator(
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