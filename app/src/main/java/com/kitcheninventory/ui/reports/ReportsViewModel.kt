package com.kitcheninventory.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.repo.InventoryRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportsState(
    val loading: Boolean = true,
    /** The preset picked, or null when the user chose their own dates. */
    val period: ReportPeriod? = ReportPeriod.WEEK,
    val range: ReportRange = ReportRange(LocalDate.now(), LocalDate.now()),
    val report: Report = Report(),
    val stockValue: Double = 0.0,
    val stockByCategory: List<ReportLine> = emptyList(),
    val hasAnyItems: Boolean = false,
)

/** A preset period, or a [custom] range of the user's own dates. */
private data class Selection(val period: ReportPeriod?, val custom: ReportRange?) {
    fun range(today: LocalDate): ReportRange = custom ?: ReportRange.of(period ?: ReportPeriod.WEEK, today)
}

class ReportsViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val selection = MutableStateFlow(Selection(ReportPeriod.WEEK, null))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodReport = selection.flatMapLatest { sel ->
        val zone = ZoneId.systemDefault()
        val range = sel.range(LocalDate.now(zone))
        // From the start of the first day up to the start of the day after the last one.
        val from = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        repo.movementsBetween(from, to).map { movements -> Triple(sel.period, range, buildReport(movements)) }
    }

    val state: StateFlow<ReportsState> =
        combine(repo.activeItems, periodReport) { items, (period, range, report) ->
            ReportsState(
                loading = false,
                period = period,
                range = range,
                report = report,
                stockValue = items.sumOf { it.stockValue.coerceAtLeast(0.0) },
                stockByCategory = stockValueByCategory(items),
                hasAnyItems = items.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsState())

    fun setPeriod(value: ReportPeriod) {
        selection.value = Selection(value, null)
    }

    /** Reports on [start] to [end], both days included. The two may be given in either order. */
    fun setCustomRange(start: LocalDate, end: LocalDate) {
        selection.value = Selection(null, ReportRange(minOf(start, end), maxOf(start, end)))
    }
}
