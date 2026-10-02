package com.kitcheninventory.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.MovementWithItem
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.formatMoney
import com.kitcheninventory.ui.common.formatQuantity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/** Every stock change, newest first, grouped by day. Tap an entry to see it or delete a mistake. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<MovementWithItem?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search items or notes") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(HistoryFilter.entries) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                        )
                    }
                }
            }
            if (state.days.isEmpty()) {
                item {
                    if (state.hasAnyEntries) {
                        EmptyState(Icons.Filled.Search, "No matches", "Try a different search or filter.")
                    } else {
                        EmptyState(
                            Icons.Filled.DateRange,
                            "No stock changes yet",
                            "Deliveries, usage, waste and counts you record will show up here.",
                        )
                    }
                }
            }
            state.days.forEach { day ->
                stickyHeader(key = "day-${day.date}") { DayHeader(day.date) }
                items(day.entries, key = { it.id }) { entry ->
                    EntryRow(entry, onClick = { selected = entry })
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { entry ->
        EntryDialog(
            entry = entry,
            onDismiss = { selected = null },
            onDelete = {
                viewModel.delete(entry)
                selected = null
            },
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(dayFormat)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EntryRow(entry: MovementWithItem, onClick: () -> Unit) {
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(timeFormat)
    val details = listOf(typeLabel(entry.type), entry.note).filter { it.isNotBlank() }.joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(entry.itemName) },
        supportingContent = { Text("$time · $details") },
        trailingContent = {
            Text(
                signedQuantity(entry),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = quantityColor(entry.quantity),
            )
        },
    )
}

@Composable
private fun EntryDialog(entry: MovementWithItem, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val whenText = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.itemName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${typeLabel(entry.type)}: ${signedQuantity(entry)}", style = MaterialTheme.typography.titleMedium)
                Text(whenText)
                if (entry.unitCost > 0) Text("Value: ${formatMoney(abs(entry.quantity) * entry.unitCost)}")
                if (entry.note.isNotBlank()) Text("Note: ${entry.note}")
                if (confirming) {
                    Text(
                        "Delete this entry? The item's stock will be recalculated without it.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (confirming) {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (confirming) {
                TextButton(onClick = { confirming = false }) { Text("Keep") }
            } else {
                TextButton(onClick = { confirming = true }) { Text("Delete entry") }
            }
        },
    )
}

fun typeLabel(type: MovementType): String = when (type) {
    MovementType.OPENING -> "Starting stock"
    MovementType.STOCK_IN -> "Stock in"
    MovementType.USAGE -> "Usage"
    MovementType.WASTE -> "Waste"
    MovementType.ADJUSTMENT -> "Count correction"
}

private fun signedQuantity(entry: MovementWithItem): String {
    val sign = if (entry.quantity > 0) "+" else "−"
    return "$sign${formatQuantity(abs(entry.quantity))} ${entry.unitAbbreviation}"
}

@Composable
private fun quantityColor(quantity: Double): Color =
    if (quantity < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
