package com.kitcheninventory.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.items.filterItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountInput(
    val counts: Map<Long, String> = emptyMap(),
    val query: String = "",
    val categoryId: Long? = null,
    val lowStockOnly: Boolean = false,
    val saving: Boolean = false,
    val result: SaveResult? = null,
)

data class CountState(
    val loading: Boolean = true,
    val input: CountInput = CountInput(),
    val items: List<ItemWithStock> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val hasAnyItems: Boolean = false,
    val countedCount: Int = 0,
    /** Counted items whose count differs from the expected stock. */
    val changedCount: Int = 0,
    val hasErrors: Boolean = false,
)

class CountViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val input = MutableStateFlow(CountInput())
    private val allItems = repo.activeItems.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<CountState> =
        combine(allItems, repo.categories, input) { all, categories, i ->
            val filled = i.counts.filterValues { it.isNotBlank() }
            CountState(
                loading = all == null,
                input = i,
                items = filterItems(all.orEmpty(), i.query, i.categoryId, i.lowStockOnly),
                categories = categories,
                hasAnyItems = !all.isNullOrEmpty(),
                countedCount = filled.size,
                changedCount = buildCountAdjustments(filled, all.orEmpty(), 0).size,
                hasErrors = filled.values.any { countError(it) != null },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CountState())

    fun setCount(itemId: Long, text: String) = input.update { it.copy(counts = it.counts + (itemId to text)) }
    fun setQuery(query: String) = input.update { it.copy(query = query) }
    fun toggleCategory(id: Long) = input.update { it.copy(categoryId = if (it.categoryId == id) null else id) }
    fun toggleLowStockOnly() = input.update { it.copy(lowStockOnly = !it.lowStockOnly) }
    fun clear() = input.update { it.copy(counts = emptyMap()) }
    fun resultShown() = input.update { it.copy(result = null) }

    /** Fills a count with the expected amount, for items that match. */
    fun matchExpected(item: ItemWithStock) = setCount(item.id, plain(item.onHand.coerceAtLeast(0.0)))

    fun save() {
        val current = input.value
        val items = allItems.value ?: return
        if (current.saving) return
        val filled = current.counts.filterValues { it.isNotBlank() }
        if (filled.isEmpty() || filled.values.any { countError(it) != null }) return
        input.update { it.copy(saving = true) }
        viewModelScope.launch {
            val adjustments = buildCountAdjustments(filled, items, System.currentTimeMillis())
            val ids = repo.recordMovements(adjustments)
            val message = when (adjustments.size) {
                0 -> "Count saved, everything matched"
                1 -> "Count saved, 1 item corrected"
                else -> "Count saved, ${adjustments.size} items corrected"
            }
            input.update {
                it.copy(
                    saving = false,
                    counts = emptyMap(),
                    result = SaveResult(message, ids, newlyLowStock(items, adjustments)),
                )
            }
        }
    }

    fun undo(ids: List<Long>) {
        viewModelScope.launch { repo.deleteMovements(ids) }
    }
}

private fun plain(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toBigDecimal().stripTrailingZeros().toPlainString()
