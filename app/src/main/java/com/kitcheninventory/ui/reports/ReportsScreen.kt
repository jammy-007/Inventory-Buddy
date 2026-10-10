package com.kitcheninventory.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.ui.common.AppIcons
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.SectionCard
import com.kitcheninventory.ui.common.StatTile
import com.kitcheninventory.ui.common.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val shortDay = DateTimeFormatter.ofPattern("d MMM")
private val longDay = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Money in and out of the kitchen over a period, and what the shelves are worth now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report = state.report
    val colors = MaterialTheme.colorScheme
    var pickingRange by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Reports") }) }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (!state.hasAnyItems) {
            EmptyState(
                AppIcons.Chart,
                "Nothing to report yet",
                "Add items and record deliveries, usage and waste to see reports here.",
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "period") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ReportPeriod.entries) { period ->
                            FilterChip(
                                selected = state.period == period,
                                onClick = { viewModel.setPeriod(period) },
                                label = { Text(period.label) },
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.period == null,
                                onClick = { pickingRange = true },
                                label = { Text("Custom") },
                                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(18.dp)) },
                            )
                        }
                    }
                    Text(
                        dateRange(state.range),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            item(key = "tiles") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Received", formatMoney(report.received), Modifier.weight(1f))
                        StatTile("Used", formatMoney(report.used), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Waste", formatMoney(report.wasted), Modifier.weight(1f), highlight = report.wasted > 0)
                        StatTile(
                            "Count corrections",
                            formatMoney(report.countCorrections),
                            Modifier.weight(1f),
                            highlight = report.countCorrections < -0.005,
                        )
                    }
                    report.wastePercent?.let { percent ->
                        Text(
                            "${percent.roundToInt()}% of the stock that went out was wasted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (report.countCorrections < -0.005) {
                        Text(
                            "Counts found ${formatMoney(-report.countCorrections)} less stock than expected. " +
                                "Usage or waste may be going unrecorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                        )
                    }
                }
            }
            item(key = "used") {
                SectionCard("Most used (by cost)", Modifier.padding(horizontal = 16.dp)) {
                    BarList(report.topUsed, "No usage recorded in this period.", colors.primary)
                }
            }
            item(key = "wasted") {
                SectionCard("Most wasted", Modifier.padding(horizontal = 16.dp)) {
                    BarList(report.topWasted, "No waste recorded in this period.", colors.error)
                }
            }
            item(key = "categories") {
                SectionCard("Usage by category", Modifier.padding(horizontal = 16.dp)) {
                    BarList(report.usedByCategory, "No usage recorded in this period.", colors.tertiary)
                }
            }
            item(key = "stock") {
                SectionCard("Stock value now: ${formatMoney(state.stockValue)}", Modifier.padding(horizontal = 16.dp)) {
                    BarList(state.stockByCategory, "Add costs to your items to see stock value.", colors.secondary)
                }
            }
        }
    }

    if (pickingRange) {
        RangePickerDialog(
            initial = state.range,
            onPick = { start, end ->
                viewModel.setCustomRange(start, end)
                pickingRange = false
            },
            onDismiss = { pickingRange = false },
        )
    }
}

/**
 * Calendar for choosing the first and last day of a custom report. Future days can't be picked.
 * The picker works in UTC midnight milliseconds, so dates are converted through UTC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(initial: ReportRange, onPick: (LocalDate, LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = initial.end.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
        },
    )
    val start = picker.selectedStartDateMillis
    // Tapping one day and confirming reports on just that day.
    val end = picker.selectedEndDateMillis ?: start
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (start != null && end != null) onPick(utcDate(start), utcDate(end)) },
                enabled = start != null,
            ) { Text("Show report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = picker,
            title = { Text("Report dates", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
            modifier = Modifier.weight(1f),
        )
    }
}

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Ranked rows with a bar scaled to the largest value. */
@Composable
private fun BarList(lines: List<ReportLine>, emptyText: String, color: Color) {
    if (lines.isEmpty()) {
        Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = lines.maxOf { it.value }
    lines.forEach { line ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (line.detail.isNotEmpty()) {
                    Text(
                        line.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                Text(formatMoney(line.value), style = MaterialTheme.typography.labelLarge)
            }
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier.fillMaxWidth((line.value / max).toFloat().coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

/** "11 Sep – 10 Oct 2026 · 30 days", or "10 Oct 2026 · 1 day" for a single day. */
private fun dateRange(range: ReportRange): String {
    val dates = if (range.days == 1L) range.start.format(longDay) else dateSpan(range.start, range.end)
    return "$dates  ·  ${range.days} ${if (range.days == 1L) "day" else "days"}"
}

private fun dateSpan(start: LocalDate, end: LocalDate): String =
    if (start.year == end.year) "${start.format(shortDay)} – ${end.format(longDay)}"
    else "${start.format(longDay)} – ${end.format(longDay)}"
