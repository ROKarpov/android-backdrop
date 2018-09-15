package io.github.rokarpov.backdrop.demo

import android.annotation.TargetApi
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import io.github.rokarpov.backdrop.*

//import io.github.rokarpov.backdroplayout.BackdropLayout

class MainActivity : AppCompatActivity() {
    private val navigationView: NavigationView by lazy {
        this.findViewById<NavigationView>(R.id.main__navigation_back_layer)
    }
    private val backdropBackLayer: BackdropBackLayer by lazy {
        this.findViewById<BackdropBackLayer>(R.id.rootLayout)
    }


    private val backdropController by lazy {
        BackdropControllerBuilder()
                .withActivity(this)
                .withBackLayer(R.id.rootLayout)
                .withFrontLayer(R.id.main__front_layer)
                .withMappings(
                        Mapping().isNavigationMapping()
                                .withContentView(navigationView)
                                .withAppTitle(R.string.main__navigation_title),
                        Mapping().withMenuItem(R.id.menu_main__tune)
                                .withContentView(R.id.main__tune_back_layer)
                                .withAppTitle(R.string.main__tune_title))
                .withConcealedNavigationIcon(resources.getDrawable(R.drawable.ic_hamburger))
                .withRevealedNavigationIcon(resources.getDrawable(R.drawable.ic_close))
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = this.findViewById(R.id.main__toolbar)
        this.setSupportActionBar(toolbar)
        this.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (backdropController.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configureSingleViewBackdrop() {

    }

    private fun configureHideToolbarSingleView() {

    }

    private fun configureTwoViewsBackdrop() {

    }
}
