package io.github.rokarpov.backdrop.demo

import android.animation.AnimatorSet
import android.content.Context
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.github.rokarpov.backdrop.*
import android.view.inputmethod.InputMethodManager
import io.github.rokarpov.backdrop.demo.viewmodels.SearchApiStub


class MainActivity : AppCompatActivity() {
    private lateinit var navigationView: NavigationView
    private lateinit var backdropBackLayer: BackdropBackLayer
    private lateinit var searchView: SearchBackView

    private lateinit var backdropController: BackdropController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationView = this.findViewById(R.id.main__navigation_back_layer)

        searchView = this.findViewById(R.id.main__search_view)
        searchView.suggestions = SearchApiStub().getSuggestions("")

        backdropBackLayer = this.findViewById(R.id.rootLayout)

        val toolbar = this.findViewById<Toolbar>(R.id.main__toolbar)
        this.setSupportActionBar(toolbar)
        this.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_hamburger)
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
                .withConcealedNavigationIcon(R.drawable.ic_hamburger)
                .withRevealedNavigationIcon(R.drawable.ic_close)
                .build()

        backdropBackLayer.getInteractionData(searchView).contentAnimatorProvider = SearchBackViewAnimatorProvider()

        searchView.onCloseListener = object: SearchBackView.OnCloseListener {
            override fun onClose() {
                hideKeyboard(searchView.input)
                backdropController.conceal()
            }
        }

        navigationView.setNavigationItemSelectedListener {
            backdropController.conceal()
            return@setNavigationItemSelectedListener false
        }

        val recyclerView: RecyclerView = findViewById(R.id.main__list)
        recyclerView.adapter = TestAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this.applicationContext, RecyclerView.VERTICAL, false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        backdropController.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (backdropController.onOptionsItemSelected(item)) {
            if (item.itemId == R.id.menu_main__search) {
                showKeyboard(searchView.input)
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        backdropController.conceal()
    }

    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view,0)
    }
    fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    class SearchBackViewAnimatorProvider: BackdropBackLayerInteractionData.ContentAnimatorProvider {
        override fun prepare(contentView: View) {
            BackdropBackLayerInteractionData.hideView(contentView)
            if (contentView !is SearchBackView) return
            BackdropBackLayerInteractionData.hideView(contentView.input)
            BackdropBackLayerInteractionData.hideView(contentView.suggestionList)
        }

        override fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            if (contentView !is SearchBackView) return 0
            contentView.closeButton.setImageResource(R.drawable.ic_close)
            BackdropBackLayerInteractionData.showView(contentView)

            BackdropBackLayerInteractionData.addShowAnimator(animatorSet, contentView.input, delay, duration)
            BackdropBackLayerInteractionData.addShowAnimator(animatorSet, contentView.suggestionList, delay, duration)

            return duration
        }

        override fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            if (contentView !is SearchBackView) return 0
            contentView.closeButton.setImageResource(R.drawable.ic_hamburger)

            BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView.input, delay, duration)
            BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView.suggestionList, delay, duration)
            BackdropBackLayerInteractionData.addHideAnimator(animatorSet, contentView, delay + duration, duration)
            return duration
        }
    }
}