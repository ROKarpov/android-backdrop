package io.github.rokarpov.backdrop.demo

import android.content.Context
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.View
import io.github.rokarpov.backdrop.*
import android.view.inputmethod.InputMethodManager
import io.github.rokarpov.backdrop.demo.viewmodels.SearchApiStub

class MainActivity : AppCompatActivity() {
    private lateinit var navigationView: NavigationView
    private lateinit var searchView: SearchBackView

    private lateinit var backdropController: BackdropController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationView = this.findViewById(R.id.main__navigation_back_layer)

        searchView = this.findViewById(R.id.main__search_view)
        searchView.suggestions = SearchApiStub().getSuggestions("")

        val toolbar = this.findViewById<Toolbar>(R.id.main__toolbar)
        toolbar.title = title
        toolbar.inflateMenu(R.menu.menu_main)

        val backLayer = findViewById<BackdropBackLayer>(R.id.rootLayout)
        backdropController = BackdropController.build(backLayer, applicationContext) {
            supportToolbar = toolbar
            navigationIconSettings(navigationView) {
                titleId = R.string.main__navigation_title
            }
            menuItemRevealSettings(R.id.menu_main__search, searchView)
            interationSettings(searchView) {
                hideHeader = true
                animationProvider = SearchBackView.AnimatorProvider
            }
            concealedTitleId = R.string.app_name
            concealedNavigationIconId = R.drawable.ic_hamburger
            revealedNavigationIconId = R.drawable.ic_close
        }

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
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}