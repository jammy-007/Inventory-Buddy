package com.kitcheninventory.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.parseNumber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/** Records deliveries, kitchen usage and waste for many items at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(viewModel: RecordViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val input = state.input
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lowAlert by remember { mutableStateOf<List<ItemWithStock>>(emptyList()) }
    var pickingDate by remember { mutableStateOf(false) }

    LaunchedEffect(input.result) {
        val result = input.result ?: return@LaunchedEffect
        viewModel.resultShown()
        lowAlert = result.newlyLow
        scope.launch {
            val action = snackbar.showSnackbar(result.message, actionLabel = "Undo", withDismissAction = true)
            if (action == SnackbarResult.ActionPerformed) viewModel.undo(result.movementIds)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Record stock") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.hasAnyItems) {
                SaveBar(
                    label = when {
                        state.entryCount == 0 -> "Enter amounts to save"
                        state.entryCount == 1 -> "Save 1 ${input.kind.label.lowercase()} entry"
                        else -> "Save ${state.entryCount} ${input.kind.label.lowercase()} entries"
                    },
                    enabled = state.entryCount > 0 && !state.hasErrors && !input.saving,
                    onSave = viewModel::save,
                    onClear = viewModel::clear.takeIf { state.entryCount > 0 || input.note.isNotEmpty() },
                )
            }
        },
    ) { padding ->
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
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    EntryKind.entries.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = input.kind == kind,
                            onClick = { viewModel.setKind(kind) },
                            shape = SegmentedButtonDefaults.itemShape(index, EntryKind.entries.size),
                        ) { Text(kind.label) }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { pickingDate = true },
                        label = { Text(input.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Today") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    )
                    OutlinedTextField(
                        value = input.note,
                        onValueChange = viewModel::setNote,
                        placeholder = { Text(notePlaceholder(input.kind)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.hasAnyItems) {
                item {
                    ItemFilterBar(
                        query = input.query,
                        onQueryChange = viewModel::setQuery,
                        categories = state.categories,
                        selectedCategoryId = input.categoryId,
                        onToggleCategory = viewModel::toggleCategory,
                    )
                }
            }
            when {
                !state.hasAnyItems -> item { EmptyState(Icons.Filled.Edit, "No items yet", "Add items on the Stock tab, then record deliveries, usage and waste here.") }
                state.items.isEmpty() -> item { EmptyState(Icons.Filled.Search, "No matches", "Try a different search or category.") }
            }
            items(state.items, key = { it.id }) { item ->
                val text = input.quantities[item.id].orEmpty()
                val amount = parseNumber(text)?.takeIf { text.isNotBlank() && entryError(text) == null }
                val removes = input.kind.sign < 0
                ItemQuantityRow(
                    item = item,
                    value = text,
                    onValueChange = { viewModel.setQuantity(item.id, it) },
                    fieldLabel = if (removes) "Used" else "Received",
                    error = entryError(text),
                    detail = "In stock: ${formatQuantity(item.onHand)} ${item.unitAbbreviation}",
                    feedback = amount?.let {
                        val after = item.onHand + input.kind.sign * it
                        if (removes && after < 0) "More than the amount in stock"
                        else "After: ${formatQuantity(after)} ${item.unitAbbreviation}"
                    },
                    feedbackIsWarning = amount != null && removes && item.onHand - amount < 0,
                )
                HorizontalDivider()
            }
        }
    }

    if (lowAlert.isNotEmpty()) LowStockAlert(lowAlert, onDismiss = { lowAlert = emptyList() })

    if (pickingDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = input.date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: todayUtc,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

private fun notePlaceholder(kind: EntryKind) = when (kind) {
    EntryKind.STOCK_IN -> "Note, e.g. invoice number"
    EntryKind.USAGE -> "Note (optional)"
    EntryKind.WASTE -> "Reason, e.g. spoiled"
}

/** Sticky save button that stays above the keyboard. */
@Composable
fun SaveBar(label: String, enabled: Boolean, onSave: () -> Unit, onClear: (() -> Unit)?) {
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClear != null) OutlinedButton(onClick = onClear) { Text("Clear") }
            Button(onClick = onSave, enabled = enabled, modifier = Modifier.weight(1f)) { Text(label) }
        }
    }
}
