package com.kitcheninventory.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.ui.common.AppIcons
import com.kitcheninventory.ui.common.formatQuantity

/**
 * Search box and category chips shared by the Record and Count screens. With [onScan], an empty
 * search box shows a scan button; a scanned barcode is searched like typed text.
 */
@Composable
fun ItemFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onToggleCategory: (Long) -> Unit,
    lowStockOnly: Boolean? = null,
    onToggleLowStock: () -> Unit = {},
    onScan: (() -> Unit)? = null,
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search items") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = when {
                query.isNotEmpty() -> {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
                onScan != null -> {
                    {
                        IconButton(onClick = onScan) {
                            Icon(AppIcons.Barcode, contentDescription = "Scan barcode")
                        }
                    }
                }
                else -> null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (lowStockOnly != null) {
                item {
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = onToggleLowStock,
                        label = { Text("Low stock") },
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    )
                }
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onToggleCategory(category.id) },
                    label = { Text(category.name) },
                )
            }
        }
    }
}

/**
 * One item with a number field on the right. [detail] sits under the name (for example the
 * expected stock) and [feedback] under that (for example a variance or warning).
 */
@Composable
fun ItemQuantityRow(
    item: ItemWithStock,
    value: String,
    onValueChange: (String) -> Unit,
    fieldLabel: String,
    error: String?,
    detail: String,
    feedback: String? = null,
    feedbackIsWarning: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val focus = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.isLowStock) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Low stock",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    )
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (feedback != null) {
                Text(
                    feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (feedbackIsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            trailing?.invoke()
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(fieldLabel) },
            suffix = { Text(item.unitAbbreviation) },
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.width(132.dp),
        )
    }
}

/** Shown after a save that pushed items to or below their reorder level. */
@Composable
fun LowStockAlert(items: List<ItemWithStock>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(if (items.size == 1) "1 item is running low" else "${items.size} items are running low") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.take(8).forEach {
                    Text("${it.name}: reorder at ${formatQuantity(it.parLevel)} ${it.unitAbbreviation}")
                }
                if (items.size > 8) Text("and ${items.size - 8} more")
                Text(
                    "Filter by \"Low stock\" on the Stock tab to see them all.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
