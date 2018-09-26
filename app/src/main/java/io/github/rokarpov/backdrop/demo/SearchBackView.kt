package io.github.rokarpov.backdrop.demo

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import io.github.rokarpov.backdrop.demo.viewmodels.SuggestionViewModel

class SearchBackView: RelativeLayout {
    private val suggestionListAdapter = SuggestionListAdapter()

    lateinit var input: TextView
        private set
    lateinit var suggestionList: androidx.recyclerview.widget.RecyclerView
        private set
    lateinit var closeButton: ImageButton
        private set

    var onCloseListener: OnCloseListener? = null

    var suggestions: List<SuggestionViewModel>
        get() = suggestionListAdapter.suggestions
        set(value) { suggestionListAdapter.suggestions = value }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    fun init(context: Context) {
        inflate(context, R.layout.content_search_back_layer, this)
        input = findViewById(R.id.search_view__input)
        suggestionList = findViewById(R.id.search_view__suggestions)
        closeButton = findViewById(R.id.search_view__close_btn)
        closeButton.setOnClickListener { onCloseListener?.onClose() }

        suggestionList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        suggestionList.adapter = suggestionListAdapter
    }

    interface OnCloseListener {
        fun onClose()
    }
}