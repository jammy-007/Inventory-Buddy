package com.kitcheninventory.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.MovementWithItem
import com.kitcheninventory.data.repo.InventoryRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** History filter chips. OPENING and ADJUSTMENT are shown as "Added" and "Counts". */
enum class HistoryFilter(val label: String, val types: Set<MovementType>?) {
    ALL("All", null),
    STOCK_IN("Stock in", setOf(MovementType.STOCK_IN, MovementType.OPENING)),
    USAGE("Usage", setOf(MovementType.USAGE)),
    WASTE("Waste", setOf(MovementType.WASTE)),
    COUNTS("Counts", setOf(MovementType.ADJUSTMENT)),
}

data class HistoryDay(val date: LocalDate, val entries: List<MovementWithItem>)

data class HistoryState(
    val loading: Boolean = true,
    val days: List<HistoryDay> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val query: String = "",
    val hasAnyEntries: Boolean = false,
)

private data class HistoryFilters(val filter: HistoryFilter = HistoryFilter.ALL, val query: String = "")

class HistoryViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val filters = MutableStateFlow(HistoryFilters())

    val state: StateFlow<HistoryState> =
        combine(repo.recentMovements(), filters) { all, f ->
            HistoryState(
                loading = false,
                days = groupByDay(filterMovements(all, f.filter, f.query)),
                filter = f.filter,
                query = f.query,
                hasAnyEntries = all.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun setFilter(filter: HistoryFilter) = filters.update { it.copy(filter = filter) }
    fun setQuery(query: String) = filters.update { it.copy(query = query) }

    fun delete(entry: MovementWithItem) {
        viewModelScope.launch { repo.deleteMovements(listOf(entry.id)) }
    }
}

fun filterMovements(all: List<MovementWithItem>, filter: HistoryFilter, query: String): List<MovementWithItem> {
    val q = query.trim()
    return all.filter { m ->
        (filter.types == null || m.type in filter.types) &&
            (q.isEmpty() || m.itemName.contains(q, ignoreCase = true) || m.note.contains(q, ignoreCase = true))
    }
}

/** Groups entries (already newest first) into calendar days in the phone's time zone. */
fun groupByDay(entries: List<MovementWithItem>, zone: ZoneId = ZoneId.systemDefault()): List<HistoryDay> =
    entries
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .map { (date, list) -> HistoryDay(date, list) }
        .sortedByDescending { it.date }
