package io.github.gdict.core.model

data class Dictionary(
    val id: Long = 0,
    val name: String,
    val path: String,
    val isEnabled: Boolean = true
)
