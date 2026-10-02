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
    val period: ReportPeriod = ReportPeriod.WEEK,
    val start: LocalDate = LocalDate.now(),
    /** Last day included in the report. */
    val end: LocalDate = LocalDate.now(),
    val report: Report = Report(),
    val stockValue: Double = 0.0,
    val stockByCategory: List<ReportLine> = emptyList(),
    val hasAnyItems: Boolean = false,
)

class ReportsViewModel(private val repo: InventoryRepository) : ViewModel() {
    private val period = MutableStateFlow(ReportPeriod.WEEK)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodReport = period.flatMapLatest { p ->
        val zone = ZoneId.systemDefault()
        val (start, end) = p.range(LocalDate.now(zone))
        repo.movementsBetween(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atStartOfDay(zone).toInstant().toEpochMilli())
            .map { movements -> Triple(p, start to end.minusDays(1), buildReport(movements)) }
    }

    val state: StateFlow<ReportsState> =
        combine(repo.activeItems, periodReport) { items, (p, days, report) ->
            ReportsState(
                loading = false,
                period = p,
                start = days.first,
                end = days.second,
                report = report,
                stockValue = items.sumOf { it.stockValue.coerceAtLeast(0.0) },
                stockByCategory = stockValueByCategory(items),
                hasAnyItems = items.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsState())

    fun setPeriod(value: ReportPeriod) {
        period.value = value
    }
}
