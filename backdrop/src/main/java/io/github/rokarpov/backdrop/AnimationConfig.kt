package io.github.rokarpov.backdrop

import android.animation.TimeInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

class AnimationConfig {
    internal companion object {
        val DEFAULT_DELAY : Long = 0

        var DEFAULT_DURATION: Long = BackdropBackLayer.oneStepAnimationTime
        val FADE_OUT_DURATION : Long = BackdropBackLayer.fadeOutTime
        var FADE_IN_DURATION: Long = BackdropBackLayer.fadeInTime

        var DEFAULT_INTERPOLATOR: TimeInterpolator = AccelerateDecelerateInterpolator()
        val FADE_OUT_INTERPOLATOR : TimeInterpolator = AccelerateInterpolator()
        var FADE_IN_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()
    }

    val fadeOutDelay: Long = DEFAULT_DELAY
    var fadeOutDuration: Long = DEFAULT_DURATION
        private set
    var fadeOutInterpolator: TimeInterpolator = DEFAULT_INTERPOLATOR
        private set
    var fadeInDelay: Long = DEFAULT_DELAY
        private set
    var fadeInDuration: Long = DEFAULT_DURATION
        private set
    var fadeInInterpolator: TimeInterpolator = DEFAULT_INTERPOLATOR
        private set
    var totalDuration: Long = DEFAULT_DURATION
        private set

    internal fun reset() {
        fadeOutDuration = BackdropBackLayer.oneStepAnimationTime
        fadeInDuration = BackdropBackLayer.oneStepAnimationTime
        totalDuration = BackdropBackLayer.oneStepAnimationTime
        fadeInDelay = 0
    }

    internal fun updateForTwoStepAnimation() {
        fadeInDelay = FADE_OUT_DURATION
        fadeInDuration = FADE_IN_DURATION
        fadeInInterpolator = FADE_IN_INTERPOLATOR

        fadeOutDuration = FADE_OUT_DURATION
        fadeOutInterpolator = FADE_OUT_INTERPOLATOR

        totalDuration = FADE_OUT_DURATION + FADE_IN_DURATION
    }
}