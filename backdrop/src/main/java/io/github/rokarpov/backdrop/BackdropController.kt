package io.github.rokarpov.backdrop

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.ActionBar as SupportActionBar
import androidx.appcompat.widget.Toolbar as SupportToolbar
import android.view.View
import android.widget.Toolbar
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

const val EMPTY_ID: Int = -1
const val DEFAULT_ANIMATION_DURATION: Long = -1

@DslMarker
annotation class DslBackdropControllerMarker

class BackdropController {
    companion object {
        fun build(
                backLayer: BackdropBackLayer,
                context: Context, initializer: Builder.() -> Unit
        ) = Builder(backLayer, context).apply(initializer).build()
    }

    private val backLayer: BackdropBackLayer
    private val concealedNavIcon: Drawable
    private val concealedTitle: CharSequence
    private var isBackdropRevealed: Boolean = false
    private val menuRevealData: Map<Int, ControllerData>
    private val navIconRevealData: ControllerData
    private val toolbarStrategy: ToolbarStrategy

    private constructor(
            backLayer: BackdropBackLayer,
            navIconRevealData: ControllerData,
            menuRevealData: Map<Int, ControllerData>,
            toolbarStrategy: ToolbarStrategy,
            concealedTitle: CharSequence,
            concealedNavIcon: Drawable) {
        this.backLayer = backLayer
        this.backLayer.addBackdropListener(BackdropListener(this))

        this.navIconRevealData = navIconRevealData
        this.menuRevealData = menuRevealData

        this.toolbarStrategy = toolbarStrategy
        this.toolbarStrategy.setOwner(this)
        this.toolbarStrategy.updateContent(concealedTitle, concealedNavIcon)

        this.concealedTitle = concealedTitle
        this.concealedNavIcon = concealedNavIcon
    }

    fun reveal(view: View): Boolean = backLayer.revealBackView(view)
    fun conceal(): Boolean = backLayer.concealBackView()

    fun syncState() {
        backLayer.onPrepare()
        when (backLayer.state) {
            BackdropBackLayerState.REVEALED -> findDataByView(backLayer.revealedView)?.onReveal(this)
            BackdropBackLayerState.CONCEALED -> updateControllerOnConceal()
        }
    }

    internal fun onNavigationIconSelected() {
        if (isBackdropRevealed) {
            conceal()
        } else {
            navIconRevealData.onMenuItemSelected(this)
        }
    }

    internal fun onOptionsItemSelected(menuItemId: Int): Boolean {
        if (menuItemId == R.id.home && isBackdropRevealed) {
            conceal()
        } else {
            menuRevealData[menuItemId]?.onMenuItemSelected(this) ?: return false
        }
        return true
    }

    internal fun updateControllerOnConceal() {
        toolbarStrategy.updateContent(concealedTitle, concealedNavIcon)
        isBackdropRevealed = false
    }

    internal fun updateControllerOnReveal(
            revealedTitle: CharSequence,
            revealedNavIcon: Drawable
    ) {
        toolbarStrategy.updateContent(revealedTitle, revealedNavIcon)
        isBackdropRevealed = true
    }

    private fun findDataByView(view: View?): ControllerData? {
        if (view == null) return null
        if ((navIconRevealData is RevealControllerData) && (navIconRevealData.view == view)) return navIconRevealData
        for ((_, data) in menuRevealData) {
            if ((data is RevealControllerData) && (data.view == view)) return data
        }
        return null
    }



    @DslBackdropControllerMarker
    class Builder(
            private val backLayer: BackdropBackLayer,
            private val context: Context) {
        companion object {
            private const val NO_TOOLBAR_MSG = "The builder should have the specified toolbar."
            private const val NO_CONCEALED_DRAWABLE_MSG = "The concealed drawable is not specified."
        }

        private var toolbarStrategy: ToolbarStrategy? = null

        private var navIconRevealSettings: RevealSettings? = null
        private val menuRevealSettings: MutableMap<Int, RevealSettings> = mutableMapOf()
        private val interactionSettingsField: MutableList<InteractionSettings> = mutableListOf()

        var toolbar: Toolbar?
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            get() {
                val strategy = toolbarStrategy
                return when (strategy) {
                    is PlatformToolbarStrategy -> strategy.toolbar
                    else -> null
                }
            }
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            set(value) {
                if (value == null) return
                toolbarStrategy = PlatformToolbarStrategy(value)
            }
        var supportToolbar: SupportToolbar?
            get() {
                val strategy = toolbarStrategy
                return when (strategy) {
                    is SupportToolbarStrategy -> strategy.toolbar
                    else -> null
                }
            }
            set(value) {
                if (value == null) return
                toolbarStrategy = SupportToolbarStrategy(value)
            }

        var concealedTitleId: Int = EMPTY_ID
            set(@StringRes value) {
                if (value != EMPTY_ID) {
                    concealedTitle = null
                }
                field = value
            }
        var concealedTitle: CharSequence? = null
            set(value) {
                if (value != null) {
                    concealedTitleId = EMPTY_ID
                }
                field = value
            }
        var revealedNavigationIconId: Int = EMPTY_ID
            set(@DrawableRes value) {
                if (value != EMPTY_ID) {
                    revealedNavigationIcon = null
                }
                field = value
            }
        var revealedNavigationIcon: Drawable? = null
            set(value) {
                if (value != null) {
                    revealedNavigationIconId = EMPTY_ID
                }
                field = value
            }
        var concealedNavigationIconId: Int = EMPTY_ID
            set(@DrawableRes value) {
                if (value != EMPTY_ID) {
                    concealedNavigationIcon = null
                }
                field = value
            }
        var concealedNavigationIcon: Drawable? = null
            set(value) {
                if (value != null) {
                    concealedNavigationIconId = EMPTY_ID
                }
                field = value
            }

        fun navigationIconSettings(view: View, init: RevealSettings.() -> Unit = { }): RevealSettings {
            val settings = RevealSettings(view)
            settings.init()
            this.navIconRevealSettings = settings
            return settings
        }

        fun menuItemRevealSettings(menuItemId: Int, view: View, init: RevealSettings.() -> Unit = { }): RevealSettings {
            val settings = RevealSettings(view)
            settings.init()
            menuRevealSettings[menuItemId] = settings
            return settings
        }

        fun interationSettings(view: View, init: InteractionSettings.() -> Unit = { }): InteractionSettings {
            val settings = InteractionSettings(view)
            settings.init()
            interactionSettingsField.add(settings)
            return settings
        }

        fun listener(init: ListenerBuilder.() -> Unit) {
            val builder = ListenerBuilder()
            builder.init()
            backLayer.addBackdropListener(builder.build())
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun withToolbar(toolbar: Toolbar): Builder {
            this.toolbar = toolbar
            return this
        }

        fun withToolbar(toolbar: SupportToolbar): Builder {
            this.supportToolbar = toolbar
            return this
        }

        fun withRevealedNavigationIcon(@DrawableRes id: Int): Builder {
            this.revealedNavigationIconId = id
            return this
        }

        fun withRevealedNavigationIcon(drawable: Drawable): Builder {
            this.revealedNavigationIcon = drawable
            return this
        }

        fun withConcealedNavigationIcon(@DrawableRes id: Int): Builder {
            this.concealedNavigationIconId = id
            return this
        }

        fun withConcealedNavigationIcon(drawable: Drawable): Builder {
            this.concealedNavigationIcon = drawable
            return this
        }

        fun withConcealedTitle(@StringRes id: Int): Builder {
            this.concealedTitleId = id
            return this
        }

        fun withConcealedTitle(title: CharSequence): Builder {
            this.concealedTitle = title
            return this
        }

        fun withNavigationIconRevealSettings(settings: RevealSettings): Builder {
            this.navIconRevealSettings = settings
            return this
        }

        fun withMenuItemRevealSettings(vararg settings: Pair<Int, RevealSettings>): Builder {
            this.menuRevealSettings.clear()
            this.menuRevealSettings.putAll(settings)
            return this
        }

        fun withMenuItemRevealSettings(settings: Map<Int, RevealSettings>): Builder {
            this.menuRevealSettings.clear()
            this.menuRevealSettings.putAll(settings)
            return this
        }

        fun withInteractionSettings(vararg settings: InteractionSettings): Builder {
            this.interactionSettingsField.clear()
            this.interactionSettingsField.addAll(settings)
            return this
        }

        fun withInteractionSettings(settings: Iterable<InteractionSettings>): Builder {
            this.interactionSettingsField.clear()
            this.interactionSettingsField.addAll(settings)
            return this
        }

        fun build(): BackdropController {
            val toolbarStrategy = this.toolbarStrategy
                    ?: throw IllegalStateException(NO_TOOLBAR_MSG)

            val concealedTitle = selectString(concealedTitle, concealedTitleId)
                    ?: toolbarStrategy.title
            val concealedDrawable = selectDrawable(this.concealedNavigationIcon, this.concealedNavigationIconId)
                    ?: throw IllegalStateException(NO_CONCEALED_DRAWABLE_MSG)
            val defaultRevealedDrawable = selectDrawable(this.revealedNavigationIcon, this.revealedNavigationIconId)
                    ?: concealedDrawable


            for (s in interactionSettingsField) {
                val data = backLayer.getInteractionData(s.view)
                s.assignTo(data)
            }

            val navIconRevealData = navIconRevealSettings?.let {
                val revealedTitle = selectString(it.title, it.titleId) ?: concealedTitle
                val revealedNavIcon = selectDrawable(it.navIcon, it.navIconId)
                        ?: defaultRevealedDrawable
                RevealControllerData(it.view, revealedTitle, revealedNavIcon)
            }
            val menuRevealData: MutableMap<Int, RevealControllerData> = mutableMapOf()
            for ((menuId, settings) in menuRevealSettings) {
                val revealedTitle = selectString(settings.title, settings.titleId) ?: concealedTitle
                val revealedNavIcon = selectDrawable(settings.navIcon, settings.navIconId)
                        ?: defaultRevealedDrawable
                menuRevealData[menuId] = RevealControllerData(settings.view, revealedTitle, revealedNavIcon)
            }


            return BackdropController(
                    backLayer,
                    navIconRevealData ?: EmptyControllerData,
                    menuRevealData,
                    toolbarStrategy,
                    concealedTitle,
                    concealedDrawable
            )
        }

        private fun selectString(string: CharSequence?, stringId: Int): CharSequence? {
            return when {
                string != null -> string
                stringId != EMPTY_ID -> context.resources.getString(stringId)
                else -> null
            }
        }

        private fun selectDrawable(drawable: Drawable?, drawableId: Int): Drawable? {
            return when {
                drawable != null -> drawable
                drawableId != EMPTY_ID -> getDrawable(drawableId)
                else -> null
            }
        }

        private fun getDrawable(resId: Int): Drawable {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.resources.getDrawable(resId, context.theme)
            } else {
                context.resources.getDrawable(resId)
            }
        }
    }

    class BackdropListener(owner: BackdropController): BackdropBackLayer.SimpleListener() {
        val weakOwner = WeakReference(owner)

        override fun onBeforeReveal(backLayer: BackdropBackLayer, revealedView: View): Boolean {
            weakOwner.get()?.let {
                it.findDataByView(revealedView)?.onReveal(it)
            }
            return false
        }

        override fun onBeforeConceal(backLayer: BackdropBackLayer, revealedView: View): Boolean {
            weakOwner.get()?.updateControllerOnConceal()
            return false
        }
    }
}

@DslBackdropControllerMarker
class RevealSettings(val view: View) {
    var title: CharSequence? = null
        set(value) {
            if (value != null) {
                titleId = EMPTY_ID
            }
            field = value
        }
    var titleId: Int = EMPTY_ID
        set(@StringRes value) {
            if (value != EMPTY_ID) {
                title = null
            }
            field = value
        }
    var navIcon: Drawable? = null
        set(value) {
            if (value != null) {
                navIconId = EMPTY_ID
            }
            field = value
        }
    var navIconId: Int = EMPTY_ID
        set(@DrawableRes value) {
            if (value != EMPTY_ID) {
                navIcon = null
            }
            field = value
        }

    fun withTitle(title: CharSequence): RevealSettings {
        this.title = title
        return this
    }

    fun withTitle(titleId: Int): RevealSettings {
        this.titleId = titleId
        return this
    }

    fun withNavIcon(navIcon: Drawable): RevealSettings {
        this.navIcon = navIcon
        return this
    }

    fun withNavIcon(@DrawableRes navIconId: Int): RevealSettings {
        this.navIconId = navIconId
        return this
    }
}

@DslBackdropControllerMarker
class InteractionSettings(val view: View) {
    var animationProvider: BackdropBackLayerInteractionData.ContentAnimatorProvider? = null
    var hideHeader: Boolean = false
    var inAnimationDuration: Long = DEFAULT_ANIMATION_DURATION
    var outAnimationDuration: Long = DEFAULT_ANIMATION_DURATION

    fun withContentAnimationProvider(animationProvider: BackdropBackLayerInteractionData.ContentAnimatorProvider): InteractionSettings {
        this.animationProvider = animationProvider
        return this
    }

    fun withHideHeader(hideHeader: Boolean): InteractionSettings {
        this.hideHeader = hideHeader
        return this
    }

    fun withAnimationDurations(inDuration: Long, outDuration: Long): InteractionSettings {
        this.inAnimationDuration = inDuration
        this.outAnimationDuration = outDuration
        return this
    }

    internal fun assignTo(interactionData: BackdropBackLayerInteractionData) {
        interactionData.hideHeader = hideHeader
        if (animationProvider != null) {
            interactionData.contentAnimatorProvider = animationProvider
        }
        if (inAnimationDuration != DEFAULT_ANIMATION_DURATION) {
            interactionData.inAnimationDuration = inAnimationDuration
        }
        if (outAnimationDuration != DEFAULT_ANIMATION_DURATION) {
            interactionData.outAnimationDuration = outAnimationDuration
        }
    }
}


private interface ControllerData {
    fun onMenuItemSelected(controller: BackdropController): Boolean
    fun onReveal(controller: BackdropController)
}

private class RevealControllerData(
        val view: View,
        val title: CharSequence,
        val navIcon: Drawable
) : ControllerData {
    override fun onMenuItemSelected(controller: BackdropController) = controller.reveal(view)
    override fun onReveal(controller: BackdropController) = controller.updateControllerOnReveal(title, navIcon)
}

private object EmptyControllerData : ControllerData {
    override fun onMenuItemSelected(controller: BackdropController) = false
    override fun onReveal(controller: BackdropController) { }

}


private interface ToolbarStrategy {
    val title: CharSequence

    fun updateContent(title: CharSequence, navIcon: Drawable?)
    fun setOwner(owner: BackdropController) {}
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private class PlatformToolbarStrategy(
        val toolbar: Toolbar
) : ToolbarStrategy {
    override val title: CharSequence
        get() = toolbar.title

    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        toolbar.title = title
        toolbar.navigationIcon = navIcon
    }

    override fun setOwner(owner: BackdropController) {
        val weakOwner = WeakReference(owner)
        toolbar.setOnMenuItemClickListener {
            val controller = weakOwner.get() ?: return@setOnMenuItemClickListener false
            return@setOnMenuItemClickListener controller.onOptionsItemSelected(it.itemId)
        }
    }
}

private class SupportToolbarStrategy(
        val toolbar: SupportToolbar
) : ToolbarStrategy {
    override val title: CharSequence
        get() = toolbar.title

    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        toolbar.title = title
        toolbar.navigationIcon = navIcon
    }

    override fun setOwner(owner: BackdropController) {
        val weakOwner = WeakReference(owner)
        toolbar.setNavigationOnClickListener {
            weakOwner.get()?.onNavigationIconSelected()
        }
        toolbar.setOnMenuItemClickListener {
            weakOwner.get()?.onOptionsItemSelected(it.itemId) ?: false
        }
    }
}

class ListenerBuilder {
    private var onBeforeConcealAction: (BackdropBackLayer, View) -> Boolean = { _, _ -> false }
    private var onConcealAction: (BackdropBackLayer, View) -> Unit = { _, _ -> }
    private var onBeforeRevealAction: (BackdropBackLayer, View) -> Boolean = { _, _ -> false }
    private var onRevealAction: (BackdropBackLayer, View) -> Unit = { _, _ -> }

    fun onBeforeConceal(action: (backLayer: BackdropBackLayer, revealedView: View) -> Boolean) {
        onBeforeConcealAction = action
    }
    fun onConceal(action: (backLayer: BackdropBackLayer, revealedView: View) -> Unit) {
        onConcealAction = action
    }
    fun onBeforeReveal(action: (backLayer: BackdropBackLayer, revealedView: View) -> Boolean) {
        onBeforeRevealAction = action
    }
    fun onReveal(action: (backLayer: BackdropBackLayer, revealedView: View) -> Unit) {
        onRevealAction = action
    }

    fun build(): BackdropBackLayer.Listener = LambdaListener(onBeforeConcealAction, onConcealAction, onBeforeRevealAction, onRevealAction)
}

class LambdaListener(
        private val onBeforeConcealAction: (BackdropBackLayer, View) -> Boolean ,
        private val onConcealAction: (BackdropBackLayer, View) -> Unit,
        private val onBeforeRevealAction: (BackdropBackLayer, View) -> Boolean,
        private val onRevealAction: (BackdropBackLayer, View) -> Unit
): BackdropBackLayer.Listener {
    override fun onBeforeReveal(backLayer: BackdropBackLayer, revealedView: View) = onBeforeRevealAction(backLayer, revealedView)
    override fun onReveal(backLayer: BackdropBackLayer, revealedView: View) = onRevealAction(backLayer, revealedView)
    override fun onBeforeConceal(backLayer: BackdropBackLayer, revealedView: View) = onBeforeConcealAction(backLayer, revealedView)
    override fun onConceal(backLayer: BackdropBackLayer, revealedView: View) = onConcealAction(backLayer, revealedView)
}