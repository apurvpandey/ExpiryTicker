package com.apurvpandey.expiryticker.presentation.common

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Text a ViewModel wants to show without knowing the user's language: the screen resolves it
 * against its own resources, so messages follow a language change and stay testable.
 */
sealed interface UiText {
    fun resolve(res: Resources): String

    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText {
        override fun resolve(res: Resources): String = res.getString(id, *args.toTypedArray())
    }

    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = emptyList()) : UiText {
        override fun resolve(res: Resources): String = res.getQuantityString(id, quantity, *args.toTypedArray())
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = resolve(LocalContext.current.resources)
