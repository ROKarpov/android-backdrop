package io.github.rokarpov.backdrop

import android.annotation.TargetApi
import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.view.GestureDetector
import android.support.v7.app.ActionBar as SupportActionBar
import android.support.v7.widget.Toolbar as SupportToolbar
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toolbar
import java.lang.ref.WeakReference

class BackdropController {
    private val backLayer: BackdropBackLayer
    private val appBarStrategy: AppBarStrategy
    private val menuIdToRevealDataMap: MutableMap<Int, RevealData>
    private val defaultAppBarTitle: CharSequence
    private val defaultNavIcon: Drawable?
    private val revealedNavIcon: Drawable?

    internal constructor(
            backLayer: BackdropBackLayer,
            frontLayer: View?,
            menuIdToRevealDataMap: MutableMap<Int, RevealData>,
            appBarStrategy: AppBarStrategy,
            defaultTitle: CharSequence,
            defaultNavIcon: Drawable?,
            revealedNavIcon: Drawable?) {
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
    private var isBackdropRevealed: Boolean = false

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

    private fun reveal(data: RevealData): Boolean {
        appBarStrategy.updateContent(data.title, revealedNavIcon)
        isBackdropRevealed = true
        return backLayer.revealBackView(data.view)
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

    private fun findDataByView(view:View): RevealData? {
        for ((_, data) in menuIdToRevealDataMap) {
            if (data.view == view) return data
        }
        return null
    }

    private inner class GestureDetectorListener: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            backLayer.concealBackView()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            backLayer.concealBackView()
            return true
        }
    }

    class RevealedFrontViewCallback(
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
    internal var isNavigation: Boolean = false
    internal var menuItemId: Int = BackdropControllerBuilder.EMPTY_ID
    internal var menuItem: MenuItem? = null
    internal var viewId: Int = BackdropControllerBuilder.EMPTY_ID
    internal var view: View? = null
    internal var titleId: Int = BackdropControllerBuilder.EMPTY_ID
    internal var title: CharSequence? = null

    fun isNavigationMapping(value: Boolean = true): Mapping {
        if (value) {
            this.menuItem = null
            this.menuItemId = android.R.id.home
        } else {
            val checkedMenuItem = this.menuItem
            if ((this.menuItemId == android.R.id.home)
                    || ((checkedMenuItem != null) && (checkedMenuItem.itemId == android.R.id.home))) {
                this.menuItemId = BackdropControllerBuilder.EMPTY_ID
                this.menuItem = null
            }
        }
        this.isNavigation = value
        return this
    }

    fun withMenuItem(id: Int): Mapping {
        this.menuItemId = id
        this.menuItem = null
        if (menuItemId == android.R.id.home) {
            this.isNavigation = true
        }
        return this
    }
    fun withMenuItem(item: MenuItem): Mapping {
        this.menuItem = item
        this.menuItemId = BackdropControllerBuilder.EMPTY_ID
        if (item.itemId == android.R.id.home) {
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
        this.viewId = BackdropControllerBuilder.EMPTY_ID
        return this
    }

    fun withAppTitle(id: Int): Mapping {
        this.titleId = id
        this.title = null
        return this
    }
    fun withAppTitle(title: CharSequence): Mapping {
        this.title = title
        this.titleId = BackdropControllerBuilder.EMPTY_ID
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
            val controller = owner.get()
            if (controller == null) return@setOnMenuItemClickListener false
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
            val controller = owner.get()
            if (controller == null) return@setOnMenuItemClickListener false
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

//private override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: View, ev: MotionEvent): Boolean {
//    return backLayer?.let {
//        val isRevealed = it.state == BackdropBackLayerState.REVEALED
//        return isRevealed && isTouchEventInView(child, ev)
//    } ?: return false
//}
//
//override fun onTouchEvent(parent: CoordinatorLayout, child: View, ev: MotionEvent): Boolean {
//    if (gestureDetector.onTouchEvent(ev)) {
//        return true
//    }
//    return super.onTouchEvent(parent, child, ev)
//}
//
//fun isTouchEventInView(child: View, ev: MotionEvent): Boolean {
//    val x = ev.getX()
//    val y = ev.getY()
//
//    return child.left <= x && child.right >= x && child.top <= y && child.bottom >= y
//}


class BackdropControllerBuilder {
    companion object {
        const val EMPTY_ID: Int = -1
        const val NO_CONTEXT_MSG = "The builder should have the specified context. Use the withContext() or withActivity() method to do this."
        const val NO_ACTION_BAR_MSG = "The builder should have the specified toolbar, action bar or activity. Use the withToolbar() or withActivity() method."
        const val NO_ACTIVITY_MSG = "The builder should have the specified activity. Use the withActivity() method."
        const val TOOLBAR_NOT_FOUND_MSG = "The view under the specified toolbarId is not android.widget.Toolbar or android.support.v7.widget.Toolbar."

        const val MENU_ITEM_NOT_SPECIFIED_MSG = "The mapping does not specify menu item id or menu item."
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

    private var revealedNavigationIconId: Int = EMPTY_ID
    private var revealedNavigationIcon: Drawable? = null
    private var concealedNavigationIconId: Int = EMPTY_ID
    private var concealedNavigationIcon: Drawable? = null

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
        this.revealedNavigationIconId = id
        this.revealedNavigationIcon = null
        return this
    }
    fun withRevealedNavigationIcon(drawable: Drawable): BackdropControllerBuilder {
        this.revealedNavigationIcon = drawable
        this.revealedNavigationIconId = EMPTY_ID
        return this
    }
    fun withConcealedNavigationIcon(id: Int): BackdropControllerBuilder {
        this.concealedNavigationIconId = id
        this.concealedNavigationIcon = null
        return this
    }
    fun withConcealedNavigationIcon(drawable: Drawable): BackdropControllerBuilder {
        this.concealedNavigationIcon = drawable
        this.concealedNavigationIconId = EMPTY_ID
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

        var _revealedNavigationIcon = this.revealedNavigationIcon
        var _concealedNavigationIcon = this.concealedNavigationIcon


        val _appBarStrategy = createAppBarStrategy()
        val _backLayer: BackdropBackLayer = this.backLayer ?: getView(this.backLayerId)


        val checkedLayer = this.frontLayer
        val _frontLayer = if (checkedLayer != null) {
            checkedLayer
        } else if (this.frontLayerId != EMPTY_ID) {
            getView<View>(this.frontLayerId)
        } else {
            null
        }

        val menuIdToRevealDataMap: MutableMap<Int, RevealData> = mutableMapOf()
        for (mapping in mappings) {
            addRevealData(menuIdToRevealDataMap, mapping)
        }

        return BackdropController(
                _backLayer,
                _frontLayer,
                menuIdToRevealDataMap,
                _appBarStrategy,
                getTitle(),
                _concealedNavigationIcon,
                _revealedNavigationIcon)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createAppBarStrategy(): AppBarStrategy {
        val _unmappedMenuItemClickedCallback = this.unmappedMenuItemClickedCallback
        val _toolbar = this.toolbar
        val _supportToolbar = this.supportToolbar

        val _activity = this.activity
        val _appCompatActivity = this.appCompatActivity

        return if (toolbarId != EMPTY_ID) {
            val toolbar = getView<View>(toolbarId)
            if (toolbar is SupportToolbar) {
                SupportToolbarStrategy(toolbar, _unmappedMenuItemClickedCallback)
            } else if (toolbar is Toolbar) {
                ToolbarStrategy(toolbar, _unmappedMenuItemClickedCallback)
            } else  {
                throw IllegalStateException(TOOLBAR_NOT_FOUND_MSG)
            }
        } else if (_toolbar != null) {
            ToolbarStrategy(_toolbar, _unmappedMenuItemClickedCallback)
        } else if (_supportToolbar != null) {
            SupportToolbarStrategy(_supportToolbar, _unmappedMenuItemClickedCallback)
        } else if (_activity != null) {
            ActionBarStrategy(_activity.actionBar)
        } else if (_appCompatActivity != null) {
            val supportActionBar = _appCompatActivity.supportActionBar
            if (supportActionBar != null) {
                SupportActionBarStrategy(supportActionBar)
            } else {
                ActionBarStrategy(_appCompatActivity.actionBar)
            }
        } else {
            throw IllegalStateException(NO_ACTION_BAR_MSG)
        }
    }

    private fun addRevealData(menuIdToRevealDataMap: MutableMap<Int, RevealData>, dataSource: Mapping) {
        val menuItem = dataSource.menuItem
        val menuId = if (menuItem != null) {
            menuItem.itemId
        } else if (dataSource.menuItemId != EMPTY_ID) {
            dataSource.menuItemId
        } else {
            throw IllegalStateException(MENU_ITEM_NOT_SPECIFIED_MSG)
        }
        val view = dataSource.view ?: getView(dataSource.viewId)
        val title = dataSource.title ?: getResources().getString(dataSource.titleId)

        menuIdToRevealDataMap[menuId] = RevealData(view, title)
    }

    private fun <T: View> getView(id: Int): T {
        val _activity = this.activity
        val _appCompatActivity = this.appCompatActivity

        return if (_activity != null) {
            _activity.findViewById<T>(id)
        } else if (_appCompatActivity != null) {
            _appCompatActivity.findViewById<T>(id)
        } else {
            throw IllegalStateException(NO_ACTIVITY_MSG)
        }
    }
    private fun getResources(): Resources {
        val context = this.context
        if (context != null) {
            return context.resources
        } else {
            throw IllegalStateException(NO_CONTEXT_MSG)
        }
    }

    private fun getTitle():CharSequence {
        return activity?.title ?: appCompatActivity?.title ?: ""
    }
}