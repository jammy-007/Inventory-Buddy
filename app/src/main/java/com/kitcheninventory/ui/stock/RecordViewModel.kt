package com.kitcheninventory.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.items.filterItems
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What happened on the last save, shown once as a snackbar and, if needed, a low-stock alert. */
data class SaveResult(
    val message: String,
    val movementIds: List<Long>,
    val newlyLow: List<ItemWithStock>,
)

data class RecordInput(
    val kind: EntryKind = EntryKind.USAGE,
    val quantities: Map<Long, String> = emptyMap(),
    val note: String = "",
    /** Null means today. */
    val date: LocalDate? = null,
    val query: String = "",
    val categoryId: Long? = null,
    val saving: Boolean = false,
    val result: SaveResult? = null,
)

data class RecordState(
    val loading: Boolean = true,
    val input: RecordInput = RecordInput(),
    val items: List<ItemWithStock> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val hasAnyItems: Boolean = false,
    /** Entries filled in, including ones hidden by the current search. */
    val entryCount: Int = 0,
    val hasErrors: Boolean = false,
)

class RecordViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val input = MutableStateFlow(RecordInput())
    private val allItems = repo.activeItems.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<RecordState> =
        combine(allItems, repo.categories, input) { all, categories, i ->
            val filled = i.quantities.filterValues { it.isNotBlank() }
            RecordState(
                loading = all == null,
                input = i,
                items = filterItems(all.orEmpty(), i.query, i.categoryId, lowStockOnly = false),
                categories = categories,
                hasAnyItems = !all.isNullOrEmpty(),
                entryCount = filled.size,
                hasErrors = filled.values.any { entryError(it) != null },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordState())

    fun setKind(kind: EntryKind) = input.update { it.copy(kind = kind) }
    fun setQuantity(itemId: Long, text: String) = input.update { it.copy(quantities = it.quantities + (itemId to text)) }
    fun setNote(note: String) = input.update { it.copy(note = note) }
    fun setDate(date: LocalDate?) = input.update { it.copy(date = date?.takeIf { d -> d != LocalDate.now() }) }
    fun setQuery(query: String) = input.update { it.copy(query = query) }
    fun toggleCategory(id: Long) = input.update { it.copy(categoryId = if (it.categoryId == id) null else id) }
    fun clear() = input.update { it.copy(quantities = emptyMap(), note = "") }
    fun resultShown() = input.update { it.copy(result = null) }

    fun save() {
        val current = input.value
        val items = allItems.value ?: return
        if (current.saving) return
        val filled = current.quantities.filterValues { it.isNotBlank() }
        if (filled.isEmpty() || filled.values.any { entryError(it) != null }) return
        input.update { it.copy(saving = true) }
        viewModelScope.launch {
            val entries = buildEntries(current.kind, filled, items, current.note, timestampFor(current.date))
            val ids = repo.recordMovements(entries)
            val count = entries.size
            val noun = if (count == 1) "entry" else "entries"
            input.update {
                it.copy(
                    saving = false,
                    quantities = emptyMap(),
                    note = "",
                    result = SaveResult(
                        message = "Saved $count ${current.kind.label.lowercase()} $noun",
                        movementIds = ids,
                        newlyLow = newlyLowStock(items, entries),
                    ),
                )
            }
        }
    }

    fun undo(ids: List<Long>) {
        viewModelScope.launch { repo.deleteMovements(ids) }
    }
}

/** Keeps today's entries at the current time; past days get the current time of day on that date. */
fun timestampFor(date: LocalDate?, zone: ZoneId = ZoneId.systemDefault()): Long =
    if (date == null) System.currentTimeMillis()
    else LocalDateTime.of(date, LocalTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
