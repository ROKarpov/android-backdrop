package io.github.rokarpov.backdrop

import android.animation.*
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Parcelable
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.WindowInsetsCompat
import android.util.AttributeSet
import android.view.*
import android.view.MotionEvent
import java.lang.ref.WeakReference

class BackdropBackLayer: ViewGroup {
    companion object {
        internal const val NO_HEADER_MSG = "The BackdropBackLayer must contain the Header view."
        internal const val MANY_HEADERS_MSG = "The BackdropBackLayer must contain only one Header view."

        internal val DEFAULT_STATE = BackdropBackLayerState.CONCEALED

        @JvmField var fadeOutTime: Long = 100 // First step
        @JvmField var fadeInTime: Long = 200 // Second step
        @JvmField var oneStepAnimationTime: Long = 263
    }
    private var isInLayoutState: Boolean = false
    private var isAnimated: Boolean = false
    private var hasHeaderView: Boolean = false
    private val matchedParentChildren: MutableMap<View, BackdropBackLayerInteractionData> = mutableMapOf()
    private val interactionData : MutableMap<View, BackdropBackLayerInteractionData> = mutableMapOf()

    @JvmField internal var state: BackdropBackLayerState = DEFAULT_STATE

    internal lateinit var headerView: View
    @JvmField internal var revealedView: View? = null
    @JvmField internal var revealedViewInteractionData: BackdropBackLayerInteractionData? = null
    @JvmField internal var currentAnimator: Animator? = null

    // TODO: Make listeners weak.
    private val listeners: MutableList<Listener> = mutableListOf()
    private val animatorProviders: MutableList<AnimatorProvider> = mutableListOf()

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
    }

    fun revealBackView(id: Int): Boolean {
        return revealBackView(id, true)
    }
    fun revealBackView(id: Int, withAnimation: Boolean): Boolean {
        val viewToReveal = findViewById<View>(id)
        return revealBackView(viewToReveal, withAnimation)
    }

    fun revealBackView(viewToReveal: View): Boolean {
        return revealBackView(viewToReveal, true)
    }
    fun revealBackView(viewToReveal: View, withAnimation: Boolean = true): Boolean {
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

    fun updateChildState() {
        for((view, _) in interactionData) {
            BackdropBackLayerInteractionData.hideView(view)
        }
        state.onLayout(this);
    }

    fun addBackdropListener(listener: Listener): Boolean {
        return listeners.add(listener)
    }
    fun removeBackdropListener(listener: Listener): Boolean {
        return listeners.remove(listener)
    }

    fun addAnimatorProvider(provider: AnimatorProvider): Boolean {
        return animatorProviders.add(provider)
    }
    fun removeAnimatorProvider(provider: AnimatorProvider): Boolean {
        return animatorProviders.remove(provider)
    }

    // TODO: better return data through indexer.
    fun getInteractionData(view: View): BackdropBackLayerInteractionData {
        val data = interactionData[view];
        if (data == null)
            // TODO: Error message!
            throw IllegalArgumentException("")
        return data
    }
    fun getInteractionData(id: Int): BackdropBackLayerInteractionData {
        val view: View = findViewById(id)
        return getInteractionData(view)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        // To use the navigation view correctly:
        ViewCompat.setZ(child, 0.0f)
        if (params is LayoutParams)
            when(params.childType) {
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
                //measureChild(child, widthMeasureSpec, heightMeasureSpec)
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
                val width = Math.max(0, getMeasuredWidth() - horizontalPadding)
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                getChildMeasureSpec(widthMeasureSpec, horizontalPadding, lp.width)
            }
            val childHeightMeasureSpec = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                val height = Math.max(0, getMeasuredHeight() - verticalPadding - data.getContentViewVerticalOffset(headerView))
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height)
            }
            view.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }
    override fun requestLayout() {
        if (!isInLayoutState) {
            super.requestLayout()
        }
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        //if (!changed) return
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
        for((view, data) in interactionData) {
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

    // TODO: IMPLEMENT!
    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
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
    internal fun notifyConceal(revealedView: View) {
        for (listener in listeners) {
            listener.onConceal(this, revealedView)
        }
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
        fun onRevealStart(backLayer: BackdropBackLayer, revealedView: View, animationDuration: Long)
        fun onReveal(backLayer: BackdropBackLayer, revealedView: View)

        fun onConcealStart(backLayer: BackdropBackLayer, revealedView: View, animationDuration: Long)
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

        val childType : Int
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

    open class FrontLayerBehavior<T: View>: CoordinatorLayout.Behavior<T> {
        private var indent: Int = 0
        private var lastInsets: WindowInsetsCompat? = null

        private var backLayer: BackdropBackLayer? = null
        protected open var backLayerListener = AnimatorProvider<T>()

        private val gestureDetectorCompat: GestureDetectorCompat
        private var internalRevealedFrontClickCallback: WeakReference<RevealedFrontClickCallback>
                = WeakReference<RevealedFrontClickCallback>(null)

        var revealedFrontClickCallback: RevealedFrontClickCallback?
            get() {
                return internalRevealedFrontClickCallback.get()
            }
            set(value) {
                internalRevealedFrontClickCallback  = if (value != null)
                    WeakReference(value)
                else
                    WeakReference<RevealedFrontClickCallback>(null)
            }

        constructor() : super() {
            gestureDetectorCompat = GestureDetectorCompat(null, FrontViewGestureListener(WeakReference(this)))

        }
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            gestureDetectorCompat = GestureDetectorCompat(context, FrontViewGestureListener(WeakReference(this)))
        }

        override fun layoutDependsOn(parent: CoordinatorLayout, child: T, dependency: View): Boolean {
            if (dependency is BackdropBackLayer) {
                backLayerListener.frontLayer = child
                backLayer?.removeAnimatorProvider(backLayerListener)
                backLayer = dependency
                dependency.addAnimatorProvider(backLayerListener)
                return true
            }
            return false
        }

        override fun onDependentViewRemoved(parent: CoordinatorLayout, child: T, dependency: View) {
            super.onDependentViewRemoved(parent, child, dependency)
            if (dependency is BackdropBackLayer) {
                dependency.removeAnimatorProvider(backLayerListener)
                if (dependency == backLayer) {
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
            val verticalInsets = lastInsets?.let{ it.systemWindowInsetTop + it.systemWindowInsetBottom } ?: 0
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
            return ((backLayer?.state == BackdropBackLayerState.REVEALED)
                    && isTouchInView(child, ev))
        }

        override fun onTouchEvent(parent: CoordinatorLayout, child: T, ev: MotionEvent): Boolean {
            val result = gestureDetectorCompat.onTouchEvent(ev)
            return result
        }

        private fun isTouchInView(view: T, e: MotionEvent): Boolean {
            val top = view.top + view.translationY
            val left = view.left + view.translationX
            val bottom = view.bottom + view.translationY
            val right = view.right + view.translationX

            val x = e.x
            val y = e.y

            return ((y >= top) && (x >= left) && (y <= bottom) && (x <= right))
        }


        interface RevealedFrontClickCallback {
            fun onRevealedFrontViewClick()
        }

        open class AnimatorProvider<T: View>: BackdropBackLayer.AnimatorProvider {
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

        class FrontViewGestureListener<T: View>(
                private var owner: WeakReference<FrontLayerBehavior<T>>
        ): GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                val ownerInstance = owner.get() ?: return false
                ownerInstance.backLayer?.concealBackView()

                ownerInstance.internalRevealedFrontClickCallback
                        .get()?.onRevealedFrontViewClick()
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                val ownerInstance = owner.get() ?: return false
                ownerInstance.backLayer?.concealBackView()
                ownerInstance.internalRevealedFrontClickCallback
                        .get()?.onRevealedFrontViewClick()
                return true
            }
        }
    }
    class DefaultFrontLayerBehavior: FrontLayerBehavior<View> {
        constructor() : super()
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    }

    open class SimpleBackdropListener: Listener {
        override fun onRevealStart(backLayer: BackdropBackLayer, revealedView: View, animationDuration: Long) { }
        override fun onReveal(backLayer: BackdropBackLayer, revealedView: View) { }

        override fun onConcealStart(backLayer: BackdropBackLayer, revealedView: View, animationDuration: Long) { }
        override fun onConceal(backLayer: BackdropBackLayer, revealedView: View) { }
    }
}