package io.github.rokarpov.backdrop

import android.annotation.TargetApi
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.view.GestureDetector
import androidx.appcompat.app.ActionBar as SupportActionBar
import androidx.appcompat.widget.Toolbar as SupportToolbar
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import java.lang.ref.WeakReference

class BackdropController {
    private val backLayer: BackdropBackLayer
    private val appBarStrategy: AppBarStrategy
    private val menuIdToRevealDataMap: Map<Int, RevealData>
    private val defaultAppBarTitle: CharSequence
    private val defaultNavIcon: Drawable
    private val revealedNavIcon: Drawable

    private var isBackdropRevealed: Boolean = false

    internal constructor(
            backLayer: BackdropBackLayer,
            frontLayer: View?,
            menuIdToRevealDataMap: Map<Int, RevealData>,
            appBarStrategy: AppBarStrategy,
            defaultTitle: CharSequence,
            defaultNavIcon: Drawable,
            revealedNavIcon: Drawable) {
        this.backLayer = backLayer
        this.menuIdToRevealDataMap = menuIdToRevealDataMap

        this.appBarStrategy = appBarStrategy
        this.appBarStrategy.setOwner(WeakReference(this))

        this.defaultAppBarTitle = defaultTitle
        this.defaultNavIcon = defaultNavIcon

        this.revealedNavIcon = revealedNavIcon

        if (frontLayer != null) {
            val lp = frontLayer.layoutParams
            if (lp is CoordinatorLayout.LayoutParams) {
                val behavior = lp.behavior
                if (behavior is BackdropBackLayer.FrontLayerBehavior) {
                    behavior.revealedFrontClickCallback = RevealedFrontViewCallback(WeakReference(this))
                }
            }
        }
    }

    fun onOptionsItemSelected(menuItem: MenuItem):Boolean {
        val menuItemId = menuItem.itemId
        if (menuItemId == android.R.id.home && isBackdropRevealed) {
            conceal()
        } else {
            val data = menuIdToRevealDataMap[menuItemId] ?: return false
            reveal(data)
        }
        return true
    }

    fun syncState() {
        backLayer.prepare()
        when (backLayer.state) {
            BackdropBackLayerState.REVEALED -> {
                val data = findDataByView(backLayer.revealedView) ?: return
                appBarStrategy.updateContent(data.title, revealedNavIcon)
                isBackdropRevealed = true
            }
            BackdropBackLayerState.CONCEALED -> {
                appBarStrategy.updateContent(defaultAppBarTitle, defaultNavIcon)
                isBackdropRevealed = false
            }
        }
    }

    fun reveal(view: View): Boolean{
        val data = findDataByView(view)
        if (data != null) {
            return reveal(data)
        }
        return false
    }
    fun conceal(): Boolean {
        appBarStrategy.updateContent(defaultAppBarTitle, defaultNavIcon)
        isBackdropRevealed = false
        return backLayer.concealBackView()
    }

    private fun reveal(data: RevealData): Boolean {
        appBarStrategy.updateContent(data.title, revealedNavIcon)
        isBackdropRevealed = true
        return backLayer.revealBackView(data.view)
    }

    private fun findDataByView(view:View?): RevealData? {
        if (view == null) return null
        for ((_, data) in menuIdToRevealDataMap) {
            if (data.view == view) return data
        }
        return null
    }

    private class RevealedFrontViewCallback(
            private val owner: WeakReference<BackdropController>
    ): BackdropBackLayer.FrontLayerBehavior.RevealedFrontClickCallback {
        override fun onRevealedFrontViewClick() {
            val ownerInstance = owner.get()
            if (ownerInstance != null) {
                ownerInstance.appBarStrategy.updateContent(ownerInstance.defaultAppBarTitle, ownerInstance.defaultNavIcon)
                ownerInstance.isBackdropRevealed = false
            }
        }
    }
}

class Mapping {
    private var isNavigation: Boolean = false
    internal var menuItemId: Int = EMPTY_ID
        private set
    internal var viewId: Int = EMPTY_ID
        private set
    internal var view: View? = null
        private set
    internal var titleId: Int = EMPTY_ID
        private set
    internal var title: CharSequence? = null
        private set

    fun isNavigationMapping(value: Boolean = true): Mapping {
        if (value) {
            this.menuItemId = android.R.id.home
        } else {
            this.menuItemId = EMPTY_ID
        }
        this.isNavigation = value
        return this
    }

    fun withMenuItem(id: Int): Mapping {
        this.menuItemId = id
        if (menuItemId == android.R.id.home) {
            this.isNavigation = true
        }
        return this
    }

    fun withContentView(id: Int): Mapping {
        this.viewId = id
        this.view = null
        return this
    }
    fun withContentView(view: View): Mapping {
        this.view = view
        this.viewId = EMPTY_ID
        return this
    }

    fun withAppTitle(id: Int): Mapping {
        this.titleId = id
        this.title = null
        return this
    }
    fun withAppTitle(title: CharSequence): Mapping {
        this.title = title
        this.titleId = EMPTY_ID
        return this
    }
}

interface UnmappedMenuItemClickedCallback {
    fun onMenuItemClicked(menuItem: MenuItem): Boolean
}

internal interface AppBarStrategy {
    fun updateContent(title: CharSequence, navIcon: Drawable?)
    fun setOwner(owner: WeakReference<BackdropController>) { }
}
private class ToolbarStrategy(
        private val toolbar: Toolbar,
        private val callback: UnmappedMenuItemClickedCallback?
): AppBarStrategy {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        toolbar.title = title
        toolbar.navigationIcon = navIcon
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun setOwner(owner: WeakReference<BackdropController>) {
        toolbar.setOnMenuItemClickListener {
            val controller = owner.get() ?: return@setOnMenuItemClickListener false
            if (controller.onOptionsItemSelected(it)) return@setOnMenuItemClickListener true
            return@setOnMenuItemClickListener callback?.onMenuItemClicked(it) ?: false
        }
    }
}
private class SupportToolbarStrategy(
        private val toolbar: SupportToolbar,
        private val callback: UnmappedMenuItemClickedCallback?
): AppBarStrategy {
    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        toolbar.title = title
        toolbar.navigationIcon = navIcon
    }

    override fun setOwner(owner: WeakReference<BackdropController>) {
        toolbar.setOnMenuItemClickListener {
            val controller = owner.get() ?: return@setOnMenuItemClickListener false
            if (controller.onOptionsItemSelected(it)) return@setOnMenuItemClickListener true
            return@setOnMenuItemClickListener callback?.onMenuItemClicked(it) ?: false
        }
    }
}
private class ActionBarStrategy(
        private val actionBar: ActionBar
): AppBarStrategy {
    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        actionBar.title = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            actionBar.setHomeAsUpIndicator(navIcon)
        }
    }
}
private class SupportActionBarStrategy(
        private val actionBar: SupportActionBar
): AppBarStrategy {
    override fun updateContent(title: CharSequence, navIcon: Drawable?) {
        actionBar.title = title
        actionBar.setHomeAsUpIndicator(navIcon)
    }
}

internal class RevealData(val view: View, val title: CharSequence)

const val EMPTY_ID: Int = -1

class BackdropControllerBuilder {
    companion object {
        private const val NO_CONTEXT_MSG = "The builder should have the specified context. Use the withContext() or withActivity() method to do this."
        private const val NO_ACTION_BAR_MSG = "The builder should have the specified toolbar, action bar or activity. Use the withToolbar() or withActivity() method."
        private const val NO_ACTIVITY_MSG = "The builder should have the specified activity. Use the withActivity() method."
        private const val TOOLBAR_NOT_FOUND_MSG = "The view under the specified toolbarId is not android.widget.Toolbar or android.support.v7.widget.Toolbar."
        private const val NO_DRAWABLE_MSG = "The drawable is not specified."
        private const val NO_STRING_MSG = "The string is not specified."
    }
    private var context: Context? = null
    private var activity: Activity? = null
    private var appCompatActivity: AppCompatActivity? = null

    private var backLayerId: Int = EMPTY_ID
    private var backLayer: BackdropBackLayer? = null

    private var frontLayerId: Int = EMPTY_ID
    private var frontLayer: View? = null

    private var toolbarId: Int = EMPTY_ID
    private var toolbar: Toolbar? = null
    private var supportToolbar: SupportToolbar? = null

    private var unmappedMenuItemClickedCallback: UnmappedMenuItemClickedCallback? = null

    private var revealedNavIconId: Int = EMPTY_ID
    private var revealedNavIcon: Drawable? = null
    private var concealedNavIconId: Int = EMPTY_ID
    private var concealedNavIcon: Drawable? = null

    private val mappings: MutableList<Mapping> = mutableListOf()

    fun withContext(context: Context): BackdropControllerBuilder {
        this.context = context
        this.activity = null
        this.appCompatActivity = null
        return this
    }
    fun withActivity(activity: Activity): BackdropControllerBuilder {
        this.activity = activity
        this.context = activity.applicationContext
        this.appCompatActivity = null
        return this
    }
    fun withActivity(activity: AppCompatActivity): BackdropControllerBuilder {
        this.appCompatActivity = activity
        this.context = activity.applicationContext
        this.activity = null
        return this
    }

    fun withBackLayer(id: Int): BackdropControllerBuilder {
        this.backLayerId = id
        this.backLayer = null
        return this
    }
    fun withBackLayer(backLayer: BackdropBackLayer): BackdropControllerBuilder {
        this.backLayer = backLayer
        this.backLayerId = EMPTY_ID
        return this
    }

    fun withFrontLayer(id: Int): BackdropControllerBuilder {
        this.frontLayerId = id
        this.frontLayer = null
        return this
    }
    fun withFrontLayer(frontLayer: BackdropBackLayer): BackdropControllerBuilder {
        this.frontLayer = backLayer
        this.frontLayerId = EMPTY_ID
        return this
    }

    fun withToolbar(id: Int, unmappedMenuItemClickedCallback: UnmappedMenuItemClickedCallback? = null): BackdropControllerBuilder {
        this.toolbarId = id
        this.toolbar = null
        this.supportToolbar = null
        this.unmappedMenuItemClickedCallback = unmappedMenuItemClickedCallback
        return this
    }
    fun withToolbar(id: Int, unmappedMenuItemClickedCallback: ((i: MenuItem) -> Boolean)): BackdropControllerBuilder {
        return withToolbar(id, object: UnmappedMenuItemClickedCallback {
            override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
                return unmappedMenuItemClickedCallback(menuItem)
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun withToolbar(toolbar: Toolbar, unmappedMenuItemClickedCallback: UnmappedMenuItemClickedCallback? = null): BackdropControllerBuilder {
        this.toolbar = toolbar
        this.toolbarId = EMPTY_ID
        this.supportToolbar = null
        this.unmappedMenuItemClickedCallback = unmappedMenuItemClickedCallback
        return this
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun withToolbar(toolbar: Toolbar, unmappedMenuItemClickedCallback: ((i: MenuItem) -> Boolean)): BackdropControllerBuilder {
        return withToolbar(toolbar, object: UnmappedMenuItemClickedCallback {
            override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
                return unmappedMenuItemClickedCallback(menuItem)
            }
        })
    }

    fun withToolbar(toolbar: SupportToolbar, unmappedMenuItemClickedCallback: UnmappedMenuItemClickedCallback? = null): BackdropControllerBuilder {
        this.supportToolbar = toolbar
        this.toolbarId = EMPTY_ID
        this.toolbar = null
        this.unmappedMenuItemClickedCallback = unmappedMenuItemClickedCallback
        return this
    }
    fun withToolbar(toolbar: SupportToolbar, unmappedMenuItemClickedCallback: ((i: MenuItem) -> Boolean)): BackdropControllerBuilder {
        return withToolbar(toolbar, object: UnmappedMenuItemClickedCallback {
            override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
                return unmappedMenuItemClickedCallback(menuItem)
            }
        })
    }

    fun withRevealedNavigationIcon(id: Int): BackdropControllerBuilder {
        this.revealedNavIconId = id
        this.revealedNavIcon = null
        return this
    }
    fun withRevealedNavigationIcon(drawable: Drawable): BackdropControllerBuilder {
        this.revealedNavIcon = drawable
        this.revealedNavIconId = EMPTY_ID
        return this
    }
    fun withConcealedNavigationIcon(id: Int): BackdropControllerBuilder {
        this.concealedNavIconId = id
        this.concealedNavIcon = null
        return this
    }
    fun withConcealedNavigationIcon(drawable: Drawable): BackdropControllerBuilder {
        this.concealedNavIcon = drawable
        this.concealedNavIconId = EMPTY_ID
        return this
    }

    fun withMappings(vararg mappings: Mapping): BackdropControllerBuilder {
        this.mappings.clear()
        this.mappings.addAll(mappings)
        return this
    }
    fun withMappings(mappings: Iterable<Mapping>): BackdropControllerBuilder {
        this.mappings.clear()
        this.mappings.addAll(mappings)
        return this
    }

    fun build(): BackdropController {
        return BackdropController(
                selectBackView(),
                selectFrontView(),
                createMenuIdToRevealDataMap(),
                createAppBarStrategy(),
                getActivityTitle(),
                selectConcealedNavIconDrawable(),
                selectRevealedNavIconDrawable())
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createAppBarStrategy(): AppBarStrategy {
        val callback = this.unmappedMenuItemClickedCallback
        val toolbar = this.toolbar
        val supportToolbar = this.supportToolbar
        val actionBar = this.activity?.actionBar
        val supportActionBar = this.appCompatActivity?.supportActionBar

        return  when {
            toolbar != null -> ToolbarStrategy(toolbar, callback)
            supportToolbar != null -> SupportToolbarStrategy(supportToolbar, callback)
            toolbarId != EMPTY_ID -> {
                val view = getView<View>(toolbarId)
                when (view) {
                    is Toolbar -> ToolbarStrategy(view, callback)
                    is SupportToolbar -> SupportToolbarStrategy(view, callback)
                    else -> throw IllegalStateException(TOOLBAR_NOT_FOUND_MSG)
                }
            }
            actionBar != null -> ActionBarStrategy(actionBar)
            supportActionBar != null -> SupportActionBarStrategy(supportActionBar)
            else -> throw IllegalStateException(NO_ACTION_BAR_MSG)
        }
    }
    private fun createMenuIdToRevealDataMap(): Map<Int, RevealData> {
        val resources = getResources()
        val menuIdToRevealDataMap: MutableMap<Int, RevealData> = mutableMapOf()
        for (mapping in this.mappings) {
            addRevealData(menuIdToRevealDataMap, mapping, resources)
        }
        return menuIdToRevealDataMap
    }
    private fun selectBackView(): BackdropBackLayer {
        return this.backLayer ?: getView(this.backLayerId)
    }
    private fun selectFrontView(): View? {
        return when {
            this.frontLayer != null -> this.frontLayer
            this.frontLayerId != EMPTY_ID -> getView(this.frontLayerId)
            else -> null
        }
    }
    private fun selectConcealedNavIconDrawable() : Drawable {
        return selectDrawable(this.concealedNavIcon, this.concealedNavIconId, getResources(), getTheme())
    }
    private fun selectRevealedNavIconDrawable() : Drawable {
        return selectDrawable(this.revealedNavIcon, this.revealedNavIconId, getResources(), getTheme())
    }

    private fun addRevealData(
            menuIdToRevealDataMap: MutableMap<Int, RevealData>,
            dataSource: Mapping, resources: Resources) {
        val view = dataSource.view ?: getView(dataSource.viewId)
        val title = selectString(dataSource.title, dataSource.titleId, resources)
        menuIdToRevealDataMap[dataSource.menuItemId] = RevealData(view, title)
    }

    private fun <T: View> getView(id: Int): T {
        val activity = this.activity
        val appCompatActivity = this.appCompatActivity
        return when {
            activity != null -> activity.findViewById(id)
            appCompatActivity != null -> appCompatActivity.findViewById(id)
            else -> throw IllegalStateException(NO_ACTIVITY_MSG)
        }
    }
    private fun getActivityTitle():CharSequence {
        val activity = this.activity
        val appCompatActivity = this.appCompatActivity
        return when {
            activity != null -> activity.title
            appCompatActivity != null -> appCompatActivity.title
            else -> ""
        }
    }
    private fun getTheme(): Resources.Theme {
        val activity = this.activity
        val appCompatActivity = this.appCompatActivity
        val context = this.context
        return when {
            activity != null -> activity.theme
            appCompatActivity != null -> appCompatActivity.theme
            context != null -> context.theme
            else -> throw IllegalStateException(NO_CONTEXT_MSG)
        }
    }
    private fun getResources(): Resources {
        val activity = this.activity
        val appCompatActivity = this.appCompatActivity
        val context = this.context
        return when {
            activity != null -> activity.resources
            appCompatActivity != null -> appCompatActivity.resources
            context != null -> context.resources
            else -> throw IllegalStateException(NO_CONTEXT_MSG)
        }
    }
    private fun selectString(string: CharSequence?, stringId: Int, resources: Resources): CharSequence {
        return when {
            string != null -> string
            stringId!= EMPTY_ID -> resources.getString(stringId)
            else -> throw IllegalStateException(NO_STRING_MSG)
        }
    }
    private fun selectDrawable(drawable: Drawable?, drawableId: Int, resources: Resources, theme: Resources.Theme) : Drawable {
        return when {
            drawable != null -> drawable
            drawableId != EMPTY_ID -> getDrawable(drawableId, resources, theme)
            else -> throw IllegalStateException(NO_DRAWABLE_MSG)
        }
    }
    private fun getDrawable(resId: Int, resources: Resources, theme: Resources.Theme): Drawable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            resources.getDrawable(resId, theme)
        } else {
            resources.getDrawable(resId)
        }
    }
}