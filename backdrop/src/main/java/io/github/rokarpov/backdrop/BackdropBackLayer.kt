package io.github.rokarpov.backdrop

import android.animation.*
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.AttributeSet
import android.view.*
import android.view.MotionEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout

class BackdropBackLayer : ViewGroup {
    companion object {
        internal const val NO_HEADER_MSG = "The BackdropBackLayer must contain the Header view."
        internal const val MANY_HEADERS_MSG = "The BackdropBackLayer must contain only one Header view."
        internal const val NO_INTERACTION_DATA_FOR_VIEW_MSG = "The Layer does not contain an InteractionData object for the specified view. This is possible due to the fact that the passed view is a header or is not a child of the layer."

        internal val DEFAULT_STATE = BackdropBackLayerState.CONCEALED

        @JvmField
        var fadeOutTime: Long = 100
        @JvmField
        var fadeInTime: Long = 200
        @JvmField
        var oneStepAnimationTime: Long = 263
    }

    private var isInLayoutState: Boolean = false
    private var hasHeaderView: Boolean = false
    private val matchedParentChildren: MutableMap<View, BackdropBackLayerInteractionData> = mutableMapOf()
    private val interactionData: MutableMap<View, BackdropBackLayerInteractionData> = mutableMapOf()

    @JvmField
    internal var state: BackdropBackLayerState = DEFAULT_STATE

    internal lateinit var headerView: View
    @JvmField
    internal var revealedView: View? = null
    @JvmField
    internal var revealedViewInteractionData: BackdropBackLayerInteractionData? = null
    @JvmField
    internal var currentAnimator: Animator? = null

    private val listeners: MutableList<Listener> = mutableListOf()
    private val animatorProviders: MutableList<AnimatorProvider> = mutableListOf()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    fun reveal(contentViewId: Int): Boolean {
        return revealBackView(contentViewId, true)
    }

    //fun revealWithoutAnimation(contentViewId: Int): Boolean { }
    fun revealBackView(id: Int, withAnimation: Boolean): Boolean {
        val viewToReveal = findViewById<View>(id)
        return revealBackView(viewToReveal, withAnimation)
    }

    fun revealBackView(viewToReveal: View): Boolean {
        return revealBackView(viewToReveal, true)
    }

    fun revealBackView(viewToReveal: View, withAnimation: Boolean): Boolean {
        if (revealedView == viewToReveal) return false
        val interactionData = interactionData[viewToReveal] ?: return false
        return state.onReveal(this, viewToReveal, interactionData, withAnimation)
    }

    fun concealBackView(): Boolean {
        return concealBackView(true)
    }

    fun concealBackView(withAnimation: Boolean): Boolean {
        val viewToConceal = revealedView ?: return false
        val interactionData = revealedViewInteractionData ?: return false
        return state.onConceal(this, viewToConceal, interactionData, withAnimation)
    }

    fun addBackdropListener(listener: Listener): Boolean = listeners.add(listener)
    fun removeBackdropListener(listener: Listener): Boolean = listeners.remove(listener)

    fun addAnimatorProvider(provider: AnimatorProvider): Boolean = animatorProviders.add(provider)
    fun removeAnimatorProvider(provider: AnimatorProvider): Boolean = animatorProviders.remove(provider)

    fun getInteractionData(id: Int): BackdropBackLayerInteractionData = getInteractionData(findViewById<View>(id))
    fun getInteractionData(view: View): BackdropBackLayerInteractionData
            = interactionData[view]
                ?: throw IllegalArgumentException(NO_INTERACTION_DATA_FOR_VIEW_MSG)

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        // To use the navigation view correctly:
        ViewCompat.setZ(child, 0.0f)
        if (params is LayoutParams)
            when (params.childType) {
                LayoutParams.CONTENT_CHILD_TYPE -> {
                    val data = BackdropBackLayerInteractionData(this, params.shouldHideHeader)
                    interactionData[child] = data
                }
                LayoutParams.HEADER_CHILD_TYPE -> {
                    if (hasHeaderView) {
                        throw IllegalStateException(MANY_HEADERS_MSG)
                    }
                    headerView = child
                    hasHeaderView = true
                }
            }
        if (child is Listener) {
            addBackdropListener(child)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!hasHeaderView) {
            throw IllegalStateException(NO_HEADER_MSG)
        }

        val widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        val heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        val measureMatchParentChildren = (widthMeasureMode != MeasureSpec.EXACTLY) || (heightMeasureMode != MeasureSpec.EXACTLY)

        matchedParentChildren.clear()
        measureChild(headerView, widthMeasureSpec, heightMeasureSpec)
        var maxWidth: Int = headerView.measuredWidth
        var maxHeight: Int = headerView.measuredHeight
        var childState: Int = headerView.measuredState
        for ((child, data) in interactionData) {
            if (child.visibility != View.GONE) {
                measureContentView(child, widthMeasureMode, heightMeasureSpec, data.getContentViewVerticalOffset(headerView))

                val lp = child.layoutParams as LayoutParams
                maxWidth = Math.max(maxWidth,
                        child.measuredWidth)
                maxHeight = Math.max(maxHeight,
                        child.measuredHeight + data.getContentViewVerticalOffset(headerView))
                childState = View.combineMeasuredStates(childState, child.measuredState)
                if (measureMatchParentChildren) {
                    if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT || lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        matchedParentChildren.put(child, data)
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

        for ((view, data) in matchedParentChildren) {
            val lp = view.layoutParams

            val childWidthMeasureSpec = if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                val width = Math.max(0, measuredWidth - horizontalPadding)
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width)
            }
            val childHeightMeasureSpec = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                val height = Math.max(0, measuredHeight - verticalPadding - data.getContentViewVerticalOffset(headerView))
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height)
            }
            view.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }

    fun onPrepare() {
        for ((child, data) in interactionData)
            data.onPrepare(child)
        state.onPrepare(this)
    }

    override fun requestLayout() {
        if (!isInLayoutState) {
            super.requestLayout()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        this.isInLayoutState = true
        val layoutLeft = paddingLeft
        val layoutTop = paddingTop
        val layoutRight = right - left - paddingRight
        val layoutBottom = bottom - top - paddingBottom

        headerView.layout(
                layoutLeft,
                layoutTop,
                layoutLeft + headerView.measuredWidth,
                layoutTop + headerView.measuredHeight)
        for ((view, data) in interactionData) {
            data.onLayoutRevealedView(view, headerView, layoutLeft, layoutTop, layoutRight, layoutBottom)
        }
        this.isInLayoutState = false
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams()
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(lp)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = SavedState(super.onSaveInstanceState())
        state.revealedViewId = revealedView?.id ?: EMPTY_ID
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.revealedViewId == EMPTY_ID) return
        revealBackView(state.revealedViewId, false)
    }

    internal val currentExtraHeight: Int
        get() = state.getContentHeight(revealedViewInteractionData, revealedView, headerView)

    internal fun getConcealedHeight(): Int {
        return paddingTop + paddingBottom + headerView.measuredHeight
    }

    private fun measureContentView(
            view: View,
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
            contentVerticalOffset: Int
    ) {
        val lp = view.layoutParams as LayoutParams
        val horizontalPadding = paddingLeft + paddingRight
        val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width)
        val verticalPadding = paddingTop + paddingBottom + contentVerticalOffset
        val childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, verticalPadding + lp.minRevealedFrontViewHeight, lp.height)
        view.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    internal fun notifyReveal(revealedView: View) {
        for (listener in listeners) {
            listener.onReveal(this, revealedView)
        }
    }

    internal fun notifyBeforeReveal(revealedView: View) : Boolean {
        for (listener in listeners) {
            return listener.onBeforeReveal(this, revealedView)
        }
        return true
    }

    internal fun notifyConceal(revealedView: View) {
        for (listener in listeners) {
            listener.onConceal(this, revealedView)
        }
    }

    internal fun notifyBeforeConceal(revealedView: View) : Boolean {
        for (listener in listeners) {
            listener.onBeforeConceal(this, revealedView)
        }
        return true
    }

    internal fun addCustomRevealAnimators(animatorSet: AnimatorSet,
                                          inAnimationDuration: Long,
                                          outAnimationDuration: Long) {
        for (provider in animatorProviders) {
            provider.addRevealAnimator(this, animatorSet, inAnimationDuration, outAnimationDuration)
        }
    }

    internal fun addCustomConcealAnimators(animatorSet: AnimatorSet,
                                           inAnimationDuration: Long,
                                           outAnimationDuration: Long) {
        for (provider in animatorProviders) {
            provider.addConcealAnimator(this, animatorSet, inAnimationDuration, outAnimationDuration)
        }
    }

    interface Listener {
        fun onBeforeReveal(backLayer: BackdropBackLayer, revealedView: View): Boolean
        fun onReveal(backLayer: BackdropBackLayer, revealedView: View)

        fun onBeforeConceal(backLayer: BackdropBackLayer, revealedView: View): Boolean
        fun onConceal(backLayer: BackdropBackLayer, revealedView: View)
    }

    interface AnimatorProvider {
        fun addRevealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long)
        fun addConcealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long)
    }

    class LayoutParams : ViewGroup.LayoutParams {
        companion object {
            const val CONTENT_CHILD_TYPE = 0x0
            const val HEADER_CHILD_TYPE = 0x1

            const val DEFAULT_WIDTH = ViewGroup.LayoutParams.MATCH_PARENT
            const val DEFAULT_HEIGHT = ViewGroup.LayoutParams.MATCH_PARENT
            const val DEFAULT_CHILD_TYPE = CONTENT_CHILD_TYPE
            const val DEFAULT_HIDE_CONCEALED = true
            const val DEFAULT_MIN_REVEALED_FRONT_VIEW_HEIGHT = 0
        }

        val childType: Int
        val shouldHideHeader: Boolean
        val minRevealedFrontViewHeight: Int

        constructor() : this(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        constructor(width: Int, height: Int) : super(width, height) {
            this.childType = DEFAULT_CHILD_TYPE
            this.shouldHideHeader = DEFAULT_HIDE_CONCEALED
            this.minRevealedFrontViewHeight = 0
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {
            this.childType = DEFAULT_CHILD_TYPE
            this.shouldHideHeader = DEFAULT_HIDE_CONCEALED
            this.minRevealedFrontViewHeight = 0
        }

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            if (c == null || attrs == null) {
                this.childType = DEFAULT_CHILD_TYPE
                this.shouldHideHeader = DEFAULT_HIDE_CONCEALED
                this.minRevealedFrontViewHeight = 0
            } else {
                val typedArray = c.obtainStyledAttributes(attrs, R.styleable.BackdropBackLayer_Layout)
                this.childType = typedArray.getInt(R.styleable.BackdropBackLayer_Layout_layout_childType, DEFAULT_CHILD_TYPE)
                this.shouldHideHeader = typedArray.getBoolean(R.styleable.BackdropBackLayer_Layout_layout_hideHeaderOnReveal, DEFAULT_HIDE_CONCEALED)
                this.minRevealedFrontViewHeight = 0
                typedArray.recycle()
            }
        }
    }

    class SavedState : BaseSavedState {
        companion object {
            @JvmField
            val CREATOR = parcelableClassLoaderCreator(::SavedState, ::SavedState)
        }

        @JvmField
        var revealedViewId: Int

        private constructor(source: Parcel) : super(source) {
            revealedViewId = source.readInt()
        }

        @TargetApi(Build.VERSION_CODES.N)
        private constructor(source: Parcel, loader: ClassLoader) : super(source, loader) {
            revealedViewId = source.readInt()
        }

        constructor(superState: Parcelable) : super(superState) {
            revealedViewId = EMPTY_ID
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(revealedViewId)
        }
    }

    open class FrontLayerBehavior<T : View> : CoordinatorLayout.Behavior<T>, FrontLayerClickListener {
        private var indent: Int = 0
        private var lastInsets: WindowInsetsCompat? = null

        private var backLayer: BackdropBackLayer? = null
        protected open var animatorProvider = AnimatorProvider<T>()
        private lateinit var frontViewOnClickStrategy: FrontLayerBehaviorOnClickStrategy

        override val concealOnClick: Boolean
            get() = frontViewOnClickStrategy is ConcealOnClickFrontLayerBehaviorOnClickStrategy

        constructor() : super() {
            allowConcealOnClick()
        }

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BackdropBackLayer_FrontLayerBehavior)
            var concealOnClick = typedArray.getBoolean(R.styleable.BackdropBackLayer_FrontLayerBehavior_behavior_concealOnClick, true)
            if (concealOnClick) {
                allowConcealOnClick()
            } else {
                disallowConcealOnClick()
            }
            typedArray.recycle()
        }

        override fun layoutDependsOn(parent: CoordinatorLayout, child: T, dependency: View): Boolean {
            if (dependency is BackdropBackLayer) {
                backLayer?.removeAnimatorProvider(animatorProvider)

                animatorProvider.frontLayer = child
                frontViewOnClickStrategy.setBackLayer(dependency)
                dependency.addAnimatorProvider(animatorProvider)
                backLayer = dependency
                return true
            }
            return false
        }

        override fun onDependentViewRemoved(parent: CoordinatorLayout, child: T, dependency: View) {
            super.onDependentViewRemoved(parent, child, dependency)
            if (dependency is BackdropBackLayer) {
                if (dependency == backLayer) {
                    dependency.removeAnimatorProvider(animatorProvider)
                    frontViewOnClickStrategy.setBackLayer(dependency)
                    backLayer = null
                }
            }
        }

//        override fun onApplyWindowInsets(
//                coordinatorLayout: CoordinatorLayout, child: View, insets: WindowInsetsCompat
//        ): WindowInsetsCompat {
//            lastInsets = insets
//            return insets.consumeSystemWindowInsets()
//        }

        override fun onDependentViewChanged(parent: CoordinatorLayout, child: T, dependency: View): Boolean {
            super.onDependentViewChanged(parent, child, dependency)
            if (dependency is BackdropBackLayer) {
                child.top = dependency.getConcealedHeight() + indent
                child.translationY = -dependency.currentExtraHeight.toFloat()
            }
            return true
        }

        override fun onMeasureChild(
                parent: CoordinatorLayout, child: T,
                parentWidthMeasureSpec: Int, widthUsed: Int,
                parentHeightMeasureSpec: Int, heightUsed: Int
        ): Boolean {
            val lp = child.layoutParams as? CoordinatorLayout.LayoutParams ?: return false
            indent = lp.topMargin
            val headerBottom = backLayer?.getConcealedHeight() ?: 0
            val verticalInsets = lastInsets?.let { it.systemWindowInsetTop + it.systemWindowInsetBottom }
                    ?: 0
            parent.onMeasureChild(
                    child,
                    parentWidthMeasureSpec, widthUsed,
                    parentHeightMeasureSpec, heightUsed + headerBottom + verticalInsets)
            return true
        }

        override fun onLayoutChild(parent: CoordinatorLayout, child: T, layoutDirection: Int): Boolean {
            parent.onLayoutChild(child, layoutDirection)
            val headerBottom = backLayer?.getConcealedHeight() ?: 0
            val verticalInsets = lastInsets?.systemWindowInsetTop ?: 0
            ViewCompat.offsetTopAndBottom(child, verticalInsets + headerBottom)
            return true
        }

        override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: T, ev: MotionEvent): Boolean {
            return frontViewOnClickStrategy.onInterceptTouchEvent(child, ev)
        }

        override fun onTouchEvent(parent: CoordinatorLayout, child: T, ev: MotionEvent): Boolean {
            return frontViewOnClickStrategy.onTouchEvent(child, ev)
        }

        override fun allowConcealOnClick() {
            allowConcealOnClick(EmptyFrontLayerClickCallback)
        }

        override fun allowConcealOnClick(callback: FrontLayerClickCallback) {
            frontViewOnClickStrategy = ConcealOnClickFrontLayerBehaviorOnClickStrategy(callback)
            frontViewOnClickStrategy.setBackLayer(backLayer)
        }

        override fun disallowConcealOnClick() {
            frontViewOnClickStrategy = NotConcealOnClickFrontLayerBehaviorOnClickStrategy
        }

        open class AnimatorProvider<T : View> : BackdropBackLayer.AnimatorProvider {
            lateinit var frontLayer: T

            override fun addRevealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long) {
                val offset = -backLayer.currentExtraHeight.toFloat()
                val translateAnimator = ObjectAnimator.ofFloat(frontLayer, View.TRANSLATION_Y, frontLayer.translationY, offset)
                translateAnimator.duration = inAnimationDuration + outAnimationDuration
                animatorSet.play(translateAnimator)
            }

            override fun addConcealAnimator(backLayer: BackdropBackLayer, animatorSet: AnimatorSet, inAnimationDuration: Long, outAnimationDuration: Long) {
                val offset = -backLayer.currentExtraHeight.toFloat()
                val translateAnimator = ObjectAnimator.ofFloat(frontLayer, View.TRANSLATION_Y, frontLayer.translationY, offset)
                translateAnimator.duration = inAnimationDuration + outAnimationDuration
                animatorSet.play(translateAnimator)
            }
        }
    }

    class DefaultFrontLayerBehavior : FrontLayerBehavior<View> {
        constructor() : super()
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    }

    open class SimpleBackdropListener : Listener {
        override fun onBeforeReveal(backLayer: BackdropBackLayer, revealedView: View): Boolean = false
        override fun onReveal(backLayer: BackdropBackLayer, revealedView: View) {}

        override fun onBeforeConceal(backLayer: BackdropBackLayer, revealedView: View): Boolean = false
        override fun onConceal(backLayer: BackdropBackLayer, revealedView: View) {}
    }
}