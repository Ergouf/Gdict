package io.github.gdict.core.model

data class SearchResultItem(
    val word: String,
    val definition: String,
    val dictionaryName: String,
    val css: String = ""
)
