package io.github.rokarpov.backdrop.demo

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

class BackSearchView: RelativeLayout {
    lateinit var input: TextView
        private set
    lateinit var suggestions: RecyclerView
        private set
    lateinit var closeButton: ImageButton
        private set

    var onCloseListener: OnCloseListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    fun init() {
        inflate(this.context, R.layout.view_search_back_layer, this)
        input = findViewById(R.id.search_view__input)
        suggestions = findViewById(R.id.search_view__suggestions)
        closeButton = findViewById(R.id.search_view__close_btn)
        closeButton.setOnClickListener { onCloseListener?.onClose() }
    }

    interface OnCloseListener {
        fun onClose()
    }
}