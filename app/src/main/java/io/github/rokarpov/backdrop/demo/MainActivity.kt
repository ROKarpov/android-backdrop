package io.github.rokarpov.backdrop.demo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import io.github.rokarpov.backdrop.*
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager


//import io.github.rokarpov.backdroplayout.BackdropLayout

class MainActivity : AppCompatActivity() {
    private lateinit var navigationView: NavigationView
    private lateinit var backdropBackLayer: BackdropBackLayer
    private lateinit var searchView: BackSearchView

    private lateinit var backdropController: BackdropController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = this.findViewById<Toolbar>(R.id.main__toolbar)
        navigationView = this.findViewById(R.id.main__navigation_back_layer)
        searchView = this.findViewById(R.id.main__search_view)
        backdropBackLayer = this.findViewById(R.id.rootLayout)

        this.setSupportActionBar(toolbar)
        this.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }

        backdropController = BackdropControllerBuilder()
                .withActivity(this)
                .withBackLayer(backdropBackLayer)
                .withFrontLayer(R.id.main__front_layer)
                .withMappings(
                        Mapping().isNavigationMapping()
                                .withContentView(navigationView)
                                .withAppTitle(R.string.main__navigation_title),
                        Mapping().withMenuItem(R.id.menu_main__search)
                                .withContentView(searchView)
                                .withAppTitle("")
                        )
                .withConcealedNavigationIcon(resources.getDrawable(R.drawable.ic_hamburger))
                .withRevealedNavigationIcon(resources.getDrawable(R.drawable.ic_close))
                .build()

        BackdropBackLayerInteractionData.hideView(searchView.input)
        BackdropBackLayerInteractionData.hideView(searchView.suggestions)

        backdropBackLayer.getInteractionData(searchView).contentAnimatorProvider = object : BackdropBackLayerInteractionData.ContentAnimatorProvider {
            override fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
                if (contentView !is BackSearchView) return 0
                BackdropBackLayerInteractionData.showView(contentView)
                BackdropBackLayerInteractionData.addShowAnimator(animatorSet, contentView.input, delay, duration)
                BackdropBackLayerInteractionData.addShowAnimator(animatorSet, contentView.suggestions, delay, duration)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(contentView.input, InputMethodManager.SHOW_FORCED)

                return duration
            }

            override fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
                if (contentView !is BackSearchView) return 0
                BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView.input, delay, duration)
                BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView.suggestions, delay, duration)
                BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView, delay + duration, 0)
                return duration
            }
        }

        searchView.onCloseListener = object: BackSearchView.OnCloseListener {
            override fun onClose() {
                backdropController.conceal()
            }
        }

        navigationView.setNavigationItemSelectedListener {
            backdropController.conceal()
            when (it.itemId) {
                R.id.menu_navigation__single_view -> this.configureSingleViewBackdrop()
                R.id.menu_navigation__two_views -> this.configureTwoViewsBackdrop()
            }
            return@setNavigationItemSelectedListener false
        }

        val recyclerView: RecyclerView = findViewById(R.id.main__list)
        recyclerView.adapter = TestAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this.applicationContext, RecyclerView.VERTICAL, false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        backdropBackLayer.updateChildState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.menu_main__search);
        val searchActionView = searchItem.actionView
        if (searchActionView is SearchView) {
            val searchCloseIcon = searchActionView.findViewById<ImageView>(android.support.v7.appcompat.R.id.search_close_btn)
            searchCloseIcon.setImageResource(R.drawable.ic_close)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (backdropController.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        backdropController.conceal()
    }

    private fun configureSingleViewBackdrop() {

    }

    private fun configureHideToolbarSingleView() {

    }

    private fun configureTwoViewsBackdrop() {

    }
}
