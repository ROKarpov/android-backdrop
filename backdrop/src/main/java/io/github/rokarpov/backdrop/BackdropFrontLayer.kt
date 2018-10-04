package io.github.rokarpov.backdrop

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class BackdropFrontLayer: FrameLayout/*NestedScrollView*/, CoordinatorLayout.AttachedBehavior {
    companion object {
        const val MANY_HEADERS_MSG = "The BackdropBackLayer must contain only one view with \"layout_type\" set to \"header\"."
        const val MANY_CONTENT_VIEWS_MSG = "The BackdropBackLayer must contain only one view with \"layout_type\" set to \"content\"."
        const val CONCEALED_ALPHA = 1.0f
        const val REVEALED_ALPHA = 0.5f
    }

    private var headerView: View? = null
    private var contentView: View? = null

    constructor(context: Context) : this (context, null)
    constructor(context: Context, attrs: AttributeSet?): this (context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.M)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val layoutLeft = paddingLeft
        val layoutTop = paddingTop
        val layoutRight = right - left - paddingRight

        val header = headerView
        if (header != null) {
            header.layout(layoutLeft, layoutTop, layoutRight, layoutTop + header.measuredHeight)
            contentView?.let {
                val viewTop = layoutTop + header.measuredHeight
                val viewBottom = viewTop + it.measuredHeight
                it.layout(layoutLeft, viewTop, layoutRight, viewBottom)
            }
        } else {
            contentView?.let {
                val viewTop = layoutTop
                val viewBottom = viewTop + it.measuredHeight
                it.layout(layoutLeft, viewTop, layoutRight, viewBottom)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO: MODIFY TO ALLOW ONLY TWO CHILDREN!
        val widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val measureMatchParentChildren = (widthMeasureMode != MeasureSpec.EXACTLY) || (heightMeasureMode != MeasureSpec.EXACTLY)

        val header = headerView
        var contentViewVerticalOffset = 0
        var maxWidth: Int = 0
        var maxHeight: Int = 0
        var childState: Int = 0

        if (header != null) {
            measureChild(headerView, widthMeasureSpec, heightMeasureSpec)
            maxWidth = header.measuredWidth
            maxHeight = header.measuredHeight
            childState = header.measuredState
            contentViewVerticalOffset = header.measuredHeight
        }

        var isMatchedParentContent = false

        contentView?.let {
            if (it.visibility != View.GONE) {
                measureContentView(it, widthMeasureMode, heightMeasureSpec, contentViewVerticalOffset)
                val contentParams = it.layoutParams
                maxWidth = Math.max(maxWidth,
                        it.measuredWidth)
                maxHeight = Math.max(maxHeight,
                        it.measuredHeight + contentViewVerticalOffset)
                childState = View.combineMeasuredStates(childState, it.measuredState)
                if (measureMatchParentChildren) {
                    if (contentParams.width == ViewGroup.LayoutParams.MATCH_PARENT || contentParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        isMatchedParentContent = true
                    }
                }
            }
        }
        // Account for padding too
        val horizontalPadding = paddingLeft + paddingRight
        maxWidth += horizontalPadding
        val verticalPadding = paddingTop + paddingBottom
        maxHeight += verticalPadding

        // Check against our minimum height and width
        maxWidth = Math.max(maxWidth, suggestedMinimumWidth)
        maxHeight = Math.max(maxHeight, suggestedMinimumHeight)

        setMeasuredDimension(View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                View.resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState shl View.MEASURED_HEIGHT_STATE_SHIFT))

        if (isMatchedParentContent)
            contentView?.let {
                val lp = it.layoutParams

                val childWidthMeasureSpec = if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    val width = Math.max(0, getMeasuredWidth() - horizontalPadding)
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width)
                }
                val childHeightMeasureSpec = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    val height = Math.max(0, getMeasuredHeight() - verticalPadding - contentViewVerticalOffset)
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height)
                }
                it.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
    }

    override fun addView(child: View) {
        this.addView(child, -1)
    }

    override fun addView(child: View, index: Int) {
        var params = child.layoutParams
        if (params == null) {
            params = generateDefaultLayoutParams()
        }
        this.addView(child, index, params)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        this.addView(child, -1, params)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        if (params is LayoutParams)
            when(params.type) {
                LayoutParams.CONTENT_TYPE -> {
                    if (contentView != null) {
                        throw IllegalStateException(MANY_CONTENT_VIEWS_MSG)
                    }
                    contentView = child
                }
                LayoutParams.SUBHEADER_TYPE -> {
                    if (headerView != null) {
                        throw IllegalStateException(MANY_HEADERS_MSG)
                    }
                    headerView = child
                    child.bringToFront()
                }
            }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        super.generateDefaultLayoutParams()
        return LayoutParams()
    }

    override fun onSaveInstanceState(): Parcelable {
        var state = SavedState(super.onSaveInstanceState())
        state.contentViewAlpha = contentView?.alpha ?: REVEALED_ALPHA
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        contentView?.alpha = state.contentViewAlpha
    }

    override fun getBehavior(): CoordinatorLayout.Behavior<*> {
        return Behavior()
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {

    }

    private fun measureContentView(view: View, widthMeasureSpec: Int, heightMeasureSpec: Int, offset: Int) {
        val lp = view.layoutParams;
        val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, lp.width)
        val childHeightHeightSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom + offset, lp.height)
        view.measure(childWidthMeasureSpec, childHeightHeightSpec)
    }
    private fun isContentView(view: View): Boolean {
        return headerView != view
    }

    class SavedState: BaseSavedState {
        companion object {
            @JvmField val CREATOR = parcelableClassLoaderCreator(::SavedState, ::SavedState)
        }
        @JvmField var contentViewAlpha: Float

        private constructor(source: Parcel) : super(source) {
            contentViewAlpha = source.readFloat()
        }
        @TargetApi(Build.VERSION_CODES.N)
        private constructor(source: Parcel, loader: ClassLoader) : super(source, loader) {
            contentViewAlpha = source.readFloat()
        }
        constructor(superState: Parcelable) : super(superState) {
            contentViewAlpha = CONCEALED_ALPHA
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(contentViewAlpha)
        }

    }

    class LayoutParams: FrameLayout.LayoutParams {
        companion object {
            val CONTENT_TYPE: Int = 0
            val SUBHEADER_TYPE: Int = 1

            val DEFAULT_TYPE = CONTENT_TYPE
            val DEFAULT_WIDHT = ViewGroup.LayoutParams.MATCH_PARENT
            val DEFAULT_HEIGHT = ViewGroup.LayoutParams.MATCH_PARENT
        }
        val type: Int


        constructor(width: Int = DEFAULT_WIDHT, height: Int = DEFAULT_HEIGHT, type: Int = DEFAULT_TYPE) : super(width, height) {
            this.type = type
        }
        constructor(c: Context, attrs: AttributeSet) : super(c, attrs) {
            val array = c.obtainStyledAttributes(attrs, R.styleable.BackdropFrontLayer_Layout)
            type = array.getInt(R.styleable.BackdropFrontLayer_Layout_layout_childType, DEFAULT_TYPE)
            array.recycle()
        }
        constructor(source: ViewGroup.LayoutParams) : super(source) {
            if (source is LayoutParams) {
                type = source.type
            } else {
                type = DEFAULT_TYPE
            }
        }
    }

    class Behavior: BackdropBackLayer.FrontLayerBehavior<BackdropFrontLayer> {
        override var backLayerListener: BackdropBackLayer.FrontLayerBehavior.AnimatorProvider<BackdropFrontLayer>
                = AnimatorProvider()

        constructor() : super()
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

        class AnimatorProvider: BackdropBackLayer.FrontLayerBehavior.AnimatorProvider<BackdropFrontLayer>() {
            override fun addRevealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long) {
                super.addRevealAnimator(backLayer, animatorSet, inAnimationDuration, outAnimationDuration)

                val contentView = frontLayer.contentView ?: return
                val translateAnimator = ObjectAnimator.ofFloat(contentView, View.ALPHA, contentView.alpha, REVEALED_ALPHA)
                translateAnimator.duration = inAnimationDuration + outAnimationDuration
                animatorSet.play(translateAnimator)
            }

            override fun addConcealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long) {
                super.addConcealAnimator(backLayer, animatorSet, inAnimationDuration, outAnimationDuration)

                val contentView = frontLayer.contentView ?: return
                val translateAnimator = ObjectAnimator.ofFloat(contentView, View.ALPHA, contentView.alpha, CONCEALED_ALPHA)
                translateAnimator.duration = inAnimationDuration + outAnimationDuration
                animatorSet.play(translateAnimator)
            }
        }
    }
}