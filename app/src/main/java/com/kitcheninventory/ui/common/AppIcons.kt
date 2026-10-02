package com.kitcheninventory.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/*
 * The two icons the core Material icon set lacks. Drawn here instead of pulling in the large
 * extended icon library.
 */
object AppIcons {
    /** Vertical bars of different widths. */
    val Barcode: ImageVector by lazy {
        icon("Barcode") {
            listOf(3f to 2f, 6f to 1f, 8f to 2f, 11f to 1f, 13f to 3f, 17f to 1f, 19f to 2f)
                .forEach { (x, width) -> rect(x, 5f, width, 14f) }
        }
    }

    /** Three rising bars. */
    val Chart: ImageVector by lazy {
        icon("Chart") {
            rect(4f, 12f, 4f, 8f)
            rect(10f, 4f, 4f, 16f)
            rect(16f, 9f, 4f, 11f)
        }
    }
}

private fun icon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black), pathBuilder = draw)
        .build()

private fun PathBuilder.rect(x: Float, y: Float, width: Float, height: Float) {
    moveTo(x, y)
    horizontalLineToRelative(width)
    verticalLineToRelative(height)
    horizontalLineToRelative(-width)
    close()
}
