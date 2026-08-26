package com.yourapp.music.core.util

/**
 * Wraps the result of any data operation (network / db) so ViewModels
 * can expose Loading / Success / Error states to the UI in one shape.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}
