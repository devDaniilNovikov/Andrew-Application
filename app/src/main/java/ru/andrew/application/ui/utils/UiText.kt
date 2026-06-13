package ru.andrew.application.ui.utils

import androidx.annotation.StringRes

/**
 * Обертка для поддержки локализованных строковых ресурсов и динамических строк во ViewModel.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int) : UiText()
}
