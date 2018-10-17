package io.github.rokarpov.backdrop.demo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.rokarpov.backdrop.*
import io.github.rokarpov.backdrop.demo.viewmodels.SuggestionViewModel
import java.lang.ref.WeakReference

class SearchBackView: RelativeLayout {
    private val suggestionListAdapter = SuggestionListAdapter()

    lateinit var input: TextInputEditText
        private set
    lateinit var inputLayout: TextInputLayout
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
        inputLayout = findViewById(R.id.search_view__input_layout)
        suggestionList = findViewById(R.id.search_view__suggestions)
        closeButton = findViewById(R.id.search_view__close_btn)
        closeButton.setOnClickListener { onCloseListener?.onClose() }

        suggestionList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        suggestionList.adapter = suggestionListAdapter
    }

    interface OnCloseListener {
        fun onClose()
    }

    object AnimatorProvider: BackdropBackLayerInteractionData.ContentAnimatorProvider {
        override fun onPrepare(contentView: View) {
            hideView(contentView)
            if (contentView !is SearchBackView) return
            hideView(contentView.inputLayout)
            hideView(contentView.suggestionList)
        }

        override fun addOnRevealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            if (contentView !is SearchBackView) return 0
            contentView.closeButton.setImageResource(R.drawable.ic_close)
            showView(contentView)

            addShowAnimator(animatorSet, contentView.inputLayout, delay, duration)
            addShowAnimator(animatorSet, contentView.suggestionList, delay, duration)

            return duration
        }

        override fun addOnConcealAnimators(contentView: View, animatorSet: AnimatorSet, delay: Long, duration: Long): Long {
            if (contentView !is SearchBackView) return 0
            contentView.closeButton.setImageResource(R.drawable.ic_hamburger)

            addHideAnimator(animatorSet, contentView.inputLayout, delay, duration)
            addHideAnimator(animatorSet, contentView.suggestionList, delay, duration)

            val weakContentView = WeakReference(contentView)
            animatorSet.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    weakContentView.get()?.let { hideView(it) }
                }
            })
            return duration
        }
    }
}