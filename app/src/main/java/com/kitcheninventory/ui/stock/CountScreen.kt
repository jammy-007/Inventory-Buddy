package com.kitcheninventory.ui.stock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.parseNumber
import com.kitcheninventory.ui.common.rememberBarcodeScanner
import kotlin.math.abs
import kotlinx.coroutines.launch

/** End-of-day count: type what is on the shelf and the app corrects its stock to match. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountScreen(viewModel: CountViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val input = state.input
    val snackbar = remember { SnackbarHostState() }
    val scan = rememberBarcodeScanner(viewModel::setQuery)
    val scope = rememberCoroutineScope()
    var lowAlert by remember { mutableStateOf<List<ItemWithStock>>(emptyList()) }

    LaunchedEffect(input.result) {
        val result = input.result ?: return@LaunchedEffect
        viewModel.resultShown()
        lowAlert = result.newlyLow
        scope.launch {
            val action = if (result.movementIds.isEmpty()) {
                snackbar.showSnackbar(result.message, withDismissAction = true)
            } else {
                snackbar.showSnackbar(result.message, actionLabel = "Undo", withDismissAction = true)
            }
            if (action == SnackbarResult.ActionPerformed) viewModel.undo(result.movementIds)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quick count") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.hasAnyItems) {
                SaveBar(
                    label = when {
                        state.countedCount == 0 -> "Enter counts to save"
                        state.changedCount == 0 -> "Save count (all match)"
                        state.changedCount == 1 -> "Save count (1 change)"
                        else -> "Save count (${state.changedCount} changes)"
                    },
                    enabled = state.countedCount > 0 && !state.hasErrors && !input.saving,
                    onSave = viewModel::save,
                    onClear = viewModel::clear.takeIf { state.countedCount > 0 },
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
                Text(
                    "Count what's on the shelf. Items you leave blank stay as they are.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.hasAnyItems) {
                item {
                    ItemFilterBar(
                        query = input.query,
                        onQueryChange = viewModel::setQuery,
                        categories = state.categories,
                        selectedCategoryId = input.categoryId,
                        onToggleCategory = viewModel::toggleCategory,
                        lowStockOnly = input.lowStockOnly,
                        onToggleLowStock = viewModel::toggleLowStockOnly,
                        onScan = scan,
                    )
                }
            }
            when {
                !state.hasAnyItems -> item { EmptyState(Icons.Filled.CheckCircle, "No items yet", "Add items on the Stock tab, then count them here at the end of the day.") }
                state.items.isEmpty() -> item { EmptyState(Icons.Filled.Search, "No matches", "Try a different search or filter.") }
            }
            items(state.items, key = { it.id }) { item ->
                val text = input.counts[item.id].orEmpty()
                val counted = parseNumber(text)?.takeIf { text.isNotBlank() && countError(text) == null }
                val difference = counted?.minus(item.onHand)
                ItemQuantityRow(
                    item = item,
                    value = text,
                    onValueChange = { viewModel.setCount(item.id, it) },
                    fieldLabel = "Counted",
                    error = countError(text),
                    detail = "Expected: ${formatQuantity(item.onHand)} ${item.unitAbbreviation}",
                    feedback = difference?.let { varianceText(it, item.unitAbbreviation) },
                    feedbackIsWarning = difference != null && difference < -1e-9,
                    trailing = if (text.isBlank()) {
                        {
                            TextButton(
                                onClick = { viewModel.matchExpected(item) },
                                contentPadding = PaddingValues(horizontal = 0.dp),
                            ) { Text("Matches expected") }
                        }
                    } else null,
                )
                HorizontalDivider()
            }
        }
    }

    if (lowAlert.isNotEmpty()) LowStockAlert(lowAlert, onDismiss = { lowAlert = emptyList() })
}

private fun varianceText(difference: Double, unit: String): String = when {
    abs(difference) < 1e-9 -> "Matches"
    difference > 0 -> "${formatQuantity(difference)} $unit more than expected"
    else -> "${formatQuantity(-difference)} $unit short"
}
