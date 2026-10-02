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
                    }
                    Text(
                        dateRange(state.start, state.end),
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
}

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

private fun dateRange(start: LocalDate, end: LocalDate): String =
    if (start.year == end.year) "${start.format(shortDay)} – ${end.format(longDay)}"
    else "${start.format(longDay)} – ${end.format(longDay)}"
