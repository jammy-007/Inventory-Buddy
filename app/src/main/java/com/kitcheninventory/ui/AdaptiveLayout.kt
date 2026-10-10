package com.kitcheninventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * How the app arranges itself for the window width. Phones get bottom tabs; from 600dp (small
 * tablets, phones in landscape) the tabs move to a side rail; from 720dp, which covers most tablets
 * held upright, the Stock tab shows the item list and the open item side by side.
 */
data class AppLayout(val useRail: Boolean, val twoPane: Boolean) {
    companion object {
        fun forWidth(widthDp: Int) = AppLayout(useRail = widthDp >= 600, twoPane = widthDp >= 720)
    }
}

/** Keeps single-column screens at a comfortable reading width on large screens, centred. */
@Composable
fun ReadableWidth(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxWidth().fillMaxHeight()) { content() }
    }
}
