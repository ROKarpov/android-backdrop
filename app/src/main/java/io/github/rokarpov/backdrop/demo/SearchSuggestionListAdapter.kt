package io.github.rokarpov.backdrop.demo

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.rokarpov.backdrop.demo.viewmodels.SuggestionViewModel

class SuggestionListAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<SuggestionViewHolder>() {
    companion object {
        private val suggestionDrawableRes: Int = R.drawable.ic_search
        private val repeatSearchDrawableRes: Int = R.drawable.ic_history
    }

    var suggestions: List<SuggestionViewModel> = emptyList()
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

    override fun getItemCount(): Int = suggestions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_search_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        val iconResId: Int = if (suggestion.isNewSuggestion) suggestionDrawableRes else repeatSearchDrawableRes
        holder.setContent(suggestion.text, iconResId)
    }
}

class SuggestionViewHolder(itemView: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    private val textView: TextView

    init{
        if (itemView is TextView)
            textView = itemView
        else
            throw IllegalArgumentException("The passed view should be text view.")
    }

    fun setContent(text: CharSequence, iconResId: Int) {
        textView.text = text
        textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0,0 )
    }
}