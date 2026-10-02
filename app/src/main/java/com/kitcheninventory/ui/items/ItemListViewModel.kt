package com.kitcheninventory.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.repo.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ItemListState(
    val loading: Boolean = true,
    val items: List<ItemWithStock> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val query: String = "",
    val categoryId: Long? = null,
    val lowStockOnly: Boolean = false,
    val totalItems: Int = 0,
    val lowStockCount: Int = 0,
    val totalValue: Double = 0.0,
)

private data class Filters(val query: String, val categoryId: Long?, val lowStockOnly: Boolean)

class ItemListViewModel(repo: InventoryRepository) : ViewModel() {
    private val filters = MutableStateFlow(Filters("", null, false))
    private val allItems = repo.activeItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<ItemListState> =
        combine(allItems, repo.categories, filters) { all, categories, f ->
            ItemListState(
                loading = false,
                items = filterItems(all, f.query, f.categoryId, f.lowStockOnly),
                categories = categories,
                query = f.query,
                categoryId = f.categoryId,
                lowStockOnly = f.lowStockOnly,
                totalItems = all.size,
                lowStockCount = all.count { it.isLowStock },
                totalValue = all.sumOf { it.stockValue.coerceAtLeast(0.0) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemListState())

    fun setQuery(query: String) { filters.value = filters.value.copy(query = query) }

    /** Selecting the active category again clears the filter. */
    fun toggleCategory(id: Long) {
        val current = filters.value
        filters.value = current.copy(categoryId = if (current.categoryId == id) null else id)
    }

    fun toggleLowStockOnly() { filters.value = filters.value.copy(lowStockOnly = !filters.value.lowStockOnly) }

    fun itemWithBarcode(barcode: String): ItemWithStock? = allItems.value.firstOrNull { it.barcode == barcode }
}

fun filterItems(
    items: List<ItemWithStock>,
    query: String,
    categoryId: Long?,
    lowStockOnly: Boolean,
): List<ItemWithStock> {
    val q = query.trim()
    return items.filter { item ->
        (q.isEmpty() || item.name.contains(q, ignoreCase = true) ||
            item.supplierName?.contains(q, ignoreCase = true) == true || item.barcode == q) &&
            (categoryId == null || item.categoryId == categoryId) &&
            (!lowStockOnly || item.isLowStock)
    }
}
