package io.github.rokarpov.backdrop.demo.viewmodels

interface SearchApi {
    fun getSuggestions(searchText: String): List<SuggestionViewModel>
}

class SearchApiStub: SearchApi {
    override fun getSuggestions(searchText: String): List<SuggestionViewModel> {
        return listOf(
                SuggestionViewModel("Sofas", false),
                SuggestionViewModel("Sectionals", false),
                SuggestionViewModel("Hanging chairs", false),
                SuggestionViewModel("Pillow Covers", false),
                SuggestionViewModel("Ceramics", true),
                SuggestionViewModel("Speakers", true),
                SuggestionViewModel("Eco-friendly Contruction", true))
    }
}