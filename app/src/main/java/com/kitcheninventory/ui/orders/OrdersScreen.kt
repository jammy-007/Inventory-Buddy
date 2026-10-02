package com.kitcheninventory.ui.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.formatMoney
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.stock.ItemQuantityRow
import kotlinx.coroutines.launch

/**
 * Low-stock items grouped by supplier, with suggested amounts. Each supplier's order can be sent
 * by any app on the phone and added to stock when it arrives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onBack: () -> Unit, viewModel: OrdersViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var receiving by remember { mutableStateOf<OrderGroup?>(null) }

    LaunchedEffect(state.result) {
        val result = state.result ?: return@LaunchedEffect
        viewModel.resultShown()
        scope.launch {
            val action = snackbar.showSnackbar(result.message, actionLabel = "Undo", withDismissAction = true)
            if (action == SnackbarResult.ActionPerformed) viewModel.undo(result.movementIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
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
            item(key = "intro") {
                Text(
                    "Suggested amounts refill each item to its \"Order up to\" level. Change any amount, " +
                        "send the order, then mark it received when it arrives.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.hasAnyItems) {
                item(key = "filter") {
                    FilterChip(
                        selected = state.includeAll,
                        onClick = viewModel::toggleIncludeAll,
                        label = { Text("Show all items") },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            when {
                !state.hasAnyItems -> item(key = "empty") {
                    EmptyState(Icons.Filled.ShoppingCart, "No items yet", "Add items on the Stock tab to build orders.")
                }
                state.groups.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        Icons.Filled.CheckCircle,
                        "Nothing to order",
                        "No items are at their reorder level. Turn on \"Show all items\" to order anyway.",
                    )
                }
            }
            state.groups.forEach { group ->
                val lines = orderLines(group, state.quantities)
                val supplier = group.supplierId?.let { state.suppliers[it] }
                item(key = "group-${group.supplierId}") {
                    OrderHeader(
                        group = group,
                        supplier = supplier,
                        lines = lines,
                        enabled = !state.saving,
                        onSend = { sendOrder(context, supplier, orderMessage(group.supplierName, lines)) },
                        onCall = { phone -> dial(context, phone) },
                        onReceived = { receiving = group },
                    )
                }
                items(group.items, key = { it.id }) { item ->
                    val text = state.quantities[item.id].orEmpty()
                    ItemQuantityRow(
                        item = item,
                        value = text,
                        onValueChange = { viewModel.setQuantity(item.id, it) },
                        fieldLabel = "Order",
                        error = orderQuantityError(text),
                        detail = stockDetail(item),
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    receiving?.let { group ->
        val lines = orderLines(group, state.quantities)
        AlertDialog(
            onDismissRequest = { receiving = null },
            title = { Text("Order received?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("These amounts will be added to stock:")
                    lines.take(10).forEach { (item, amount) ->
                        Text("${item.name}: +${formatQuantity(amount)} ${item.unitAbbreviation}")
                    }
                    if (lines.size > 10) Text("and ${lines.size - 10} more")
                    Text(
                        "If the delivery was different, change the amounts first.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.receive(group)
                    receiving = null
                }) { Text("Add to stock") }
            },
            dismissButton = { TextButton(onClick = { receiving = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OrderHeader(
    group: OrderGroup,
    supplier: SupplierEntity?,
    lines: List<Pair<ItemWithStock, Double>>,
    enabled: Boolean,
    onSend: () -> Unit,
    onCall: (String) -> Unit,
    onReceived: () -> Unit,
) {
    val cost = lines.sumOf { (item, amount) -> item.unitCost * amount }
    val summary = buildList {
        add(if (lines.size == 1) "1 item to order" else "${lines.size} items to order")
        if (cost > 0) add("about ${formatMoney(cost)}")
    }.joinToString(" · ")
    val canOrder = enabled && lines.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.supplierName ?: "No supplier", style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (group.supplierId == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Set a supplier on these items to order them together.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onSend, enabled = canOrder) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text("Send", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
                val phone = supplier?.phone?.takeIf { it.isNotBlank() }
                if (phone != null) {
                    OutlinedButton(onClick = { onCall(phone) }) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text("Call", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                    }
                }
                TextButton(onClick = onReceived, enabled = canOrder) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text("Received", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
        }
    }
}

private fun stockDetail(item: ItemWithStock): String {
    val unit = item.unitAbbreviation
    val stock = "In stock: ${formatQuantity(item.onHand)} $unit"
    return if (item.parLevel > 0) "$stock · reorder at ${formatQuantity(item.parLevel)}" else stock
}

/** Opens the share sheet, with the supplier's email filled in for email apps. */
private fun sendOrder(context: Context, supplier: SupplierEntity?, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Order")
        putExtra(Intent.EXTRA_TEXT, text)
        supplier?.email?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
    }
    context.startActivity(Intent.createChooser(intent, "Send order"))
}

private fun dial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No phone app found", Toast.LENGTH_SHORT).show()
    }
}
