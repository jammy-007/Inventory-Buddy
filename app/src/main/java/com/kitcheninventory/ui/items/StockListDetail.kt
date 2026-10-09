package com.kitcheninventory.ui.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState

/**
 * The item opened next to the list on large screens. [itemId] 0 means a new item. [key] changes on
 * every open so each one gets a fresh form, even when the same item is opened twice.
 */
data class OpenItem(val itemId: Long, val barcode: String?, val key: Int) {
    companion object {
        /** Keeps the open item across rotation and process death. */
        val Saver = listSaver<OpenItem?, Any?>(
            save = { if (it == null) emptyList() else listOf(it.itemId, it.barcode, it.key) },
            restore = { if (it.isEmpty()) null else OpenItem(it[0] as Long, it[1] as String?, it[2] as Int) },
        )
    }
}

/**
 * Stock tab for tablets: the item list on the left and the selected item's form on the right.
 * [open] lives in the caller so it survives switching between phone and tablet layouts.
 */
@Composable
fun StockListDetail(
    open: OpenItem?,
    onOpen: (OpenItem?) -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenOrders: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    var paneMessage by remember { mutableStateOf<String?>(null) }
    val nextKey = (open?.key ?: 0) + 1
    BackHandler(enabled = open != null) { onOpen(null) }

    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.42f).fillMaxHeight()) {
            ItemListScreen(
                onAddItem = { barcode -> onOpen(OpenItem(0, barcode, nextKey)) },
                onOpenItem = { id -> onOpen(OpenItem(id, null, nextKey)) },
                onOpenSuppliers = onOpenSuppliers,
                onOpenOrders = onOpenOrders,
                message = message ?: paneMessage,
                onMessageShown = {
                    onMessageShown()
                    paneMessage = null
                },
                selectedId = open?.itemId,
            )
        }
        VerticalDivider()
        Surface(Modifier.weight(0.58f).fillMaxHeight(), color = MaterialTheme.colorScheme.background) {
            if (open == null) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Filled.Edit, "Pick an item", "Choose an item on the left to see or change it, or tap \"Add item\".")
                }
            } else {
                key(open.key) {
                    ItemEditScreen(
                        onDone = { result ->
                            onOpen(null)
                            if (result != null) paneMessage = result
                        },
                        viewModel = viewModel(
                            key = "item-pane-${open.key}",
                            factory = AppViewModelFactory,
                            extras = itemArgs(open),
                        ),
                    )
                }
            }
        }
    }
}

/** Passes the item id and barcode the same way the full-screen route does, as navigation arguments. */
@Composable
private fun itemArgs(open: OpenItem): CreationExtras {
    val owner = LocalViewModelStoreOwner.current
    val base = (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    return MutableCreationExtras(base).apply {
        set(DEFAULT_ARGS_KEY, bundleOf("itemId" to open.itemId, "barcode" to open.barcode))
    }
}
