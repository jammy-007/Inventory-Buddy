package com.kitcheninventory.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.stock.SaveResult
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class OrdersInput(
    /** Amounts the user typed, by item id. Items not in here use the suggested amount. */
    val edits: Map<Long, String> = emptyMap(),
    val includeAll: Boolean = false,
    val saving: Boolean = false,
    val result: SaveResult? = null,
)

data class OrdersState(
    val loading: Boolean = true,
    val groups: List<OrderGroup> = emptyList(),
    /** The amount to order for every listed item, by item id. */
    val quantities: Map<Long, String> = emptyMap(),
    val suppliers: Map<Long, SupplierEntity> = emptyMap(),
    val includeAll: Boolean = false,
    val hasAnyItems: Boolean = false,
    val saving: Boolean = false,
    val result: SaveResult? = null,
)

class OrdersViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val input = MutableStateFlow(OrdersInput())
    private val allItems = repo.activeItems.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<OrdersState> =
        combine(allItems, repo.supplierList, input) { all, suppliers, i ->
            val groups = groupOrders(all.orEmpty(), i.includeAll)
            OrdersState(
                loading = all == null,
                groups = groups,
                quantities = groups.flatMap { it.items }.associate { it.id to (i.edits[it.id] ?: suggestedText(it)) },
                suppliers = suppliers.associateBy { it.id },
                includeAll = i.includeAll,
                hasAnyItems = !all.isNullOrEmpty(),
                saving = i.saving,
                result = i.result,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersState())

    fun setQuantity(itemId: Long, text: String) = input.update { it.copy(edits = it.edits + (itemId to text)) }
    fun toggleIncludeAll() = input.update { it.copy(includeAll = !it.includeAll) }
    fun resultShown() = input.update { it.copy(result = null) }

    /** Adds the order's amounts to stock as a delivery. */
    fun receive(group: OrderGroup) {
        val current = state.value
        if (current.saving) return
        val lines = orderLines(group, current.quantities)
        if (lines.isEmpty()) return
        input.update { it.copy(saving = true) }
        viewModelScope.launch {
            val note = group.supplierName?.let { "Order from $it" } ?: "Order received"
            val ids = repo.recordMovements(receiveOrder(lines, note, System.currentTimeMillis()))
            val noun = if (lines.size == 1) "item" else "items"
            input.update {
                it.copy(
                    saving = false,
                    edits = it.edits - lines.map { (item, _) -> item.id }.toSet(),
                    result = SaveResult("Received ${lines.size} $noun", ids, emptyList()),
                )
            }
        }
    }

    fun undo(ids: List<Long>) {
        viewModelScope.launch { repo.deleteMovements(ids) }
    }
}

/** The suggested amount as editable text, rounded up to 3 decimals; blank when nothing is needed. */
private fun suggestedText(item: ItemWithStock): String {
    val amount = suggestedOrder(item)
    if (amount <= 1e-9) return ""
    // The small shift stops summed-decimal noise (7.0000000001) rounding up to 7.001.
    return (amount - 1e-9).toBigDecimal().setScale(3, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
}
