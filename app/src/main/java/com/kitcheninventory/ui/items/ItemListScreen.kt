package com.kitcheninventory.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.ui.common.AppIcons
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.CurrencyDialog
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.InitialAvatar
import com.kitcheninventory.ui.common.QuantityPill
import com.kitcheninventory.ui.common.StatTile
import com.kitcheninventory.ui.common.formatMoney
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.rememberBarcodeScanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    onAddItem: (barcode: String?) -> Unit,
    onOpenItem: (Long) -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenOrders: () -> Unit,
    message: String? = null,
    onMessageShown: () -> Unit = {},
    viewModel: ItemListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    var pickingCurrency by rememberSaveable { mutableStateOf(false) }
    // A known barcode opens its item; an unknown one offers to add it.
    val scan = rememberBarcodeScanner { code ->
        val match = viewModel.itemWithBarcode(code)
        if (match != null) {
            onOpenItem(match.id)
        } else {
            scope.launch {
                val action = snackbar.showSnackbar("No item has barcode $code", actionLabel = "Add item", withDismissAction = true)
                if (action == SnackbarResult.ActionPerformed) onAddItem(code)
            }
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                actions = {
                    IconButton(onClick = scan) {
                        Icon(AppIcons.Barcode, contentDescription = "Scan barcode")
                    }
                    IconButton(onClick = onOpenOrders) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Orders")
                    }
                    IconButton(onClick = onOpenSuppliers) {
                        Icon(Icons.Filled.Person, contentDescription = "Suppliers")
                    }
                    IconButton(onClick = { pickingCurrency = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Currency")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddItem(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "summary") { Summary(state) }
            item(key = "search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search items or suppliers") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.lowStockOnly,
                            onClick = viewModel::toggleLowStockOnly,
                            label = { Text("Low stock") },
                            leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.toggleCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                item(key = "empty") {
                    if (state.totalItems > 0) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "No matches",
                            message = "No items match your search or filters.",
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.ShoppingCart,
                            title = "No items yet",
                            message = "Tap \"Add item\" to add your first ingredient or supply.",
                        )
                    }
                }
            } else if (state.items.isNotEmpty()) {
                item(key = "count") {
                    Text(
                        if (state.items.size == 1) "1 item" else "${state.items.size} items",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                ItemRow(item, onClick = { onOpenItem(item.id) })
            }
        }
    }

    if (pickingCurrency) CurrencyDialog(onDismiss = { pickingCurrency = false })
}

@Composable
private fun Summary(state: ItemListState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile("Items", state.totalItems.toString(), Modifier.weight(0.8f))
        StatTile("Low stock", state.lowStockCount.toString(), Modifier.weight(0.9f), highlight = state.lowStockCount > 0)
        StatTile("Stock value", formatMoney(state.totalValue), Modifier.weight(1.3f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(item: ItemWithStock, onClick: () -> Unit) {
    val subtitle = listOfNotNull(item.categoryName, item.supplierName).joinToString(" · ")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            leadingContent = { InitialAvatar(item.name) },
            headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = if (subtitle.isNotEmpty()) {
                { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else null,
            trailingContent = {
                QuantityPill(
                    text = "${formatQuantity(item.onHand)} ${item.unitAbbreviation}",
                    low = item.isLowStock,
                )
            },
        )
    }
}
