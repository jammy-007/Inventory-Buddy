package com.kitcheninventory.ui.reports

import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.ReportMovement
import com.kitcheninventory.ui.common.formatQuantity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class ReportPeriod(val label: String) {
    WEEK("7 days"),
    MONTH("30 days"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month");

    /** First day included and the day after the last one, so the range is [start, end). */
    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        WEEK -> today.minusDays(6) to today.plusDays(1)
        MONTH -> today.minusDays(29) to today.plusDays(1)
        THIS_MONTH -> today.withDayOfMonth(1) to today.plusDays(1)
        LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1) to today.withDayOfMonth(1)
    }
}

/** The days a report covers, both ends included. */
data class ReportRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "Report range ends before it starts" }
    }

    /** How many days are covered, counting both ends. */
    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    companion object {
        fun of(period: ReportPeriod, today: LocalDate): ReportRange {
            val (start, endExclusive) = period.range(today)
            return ReportRange(start, endExclusive.minusDays(1))
        }
    }
}

/** One row of a ranked list: a name, its money value and an optional quantity such as "12 kg". */
data class ReportLine(val label: String, val value: Double, val detail: String = "")

data class Report(
    /** Value of deliveries (stock in), not counting opening stock. */
    val received: Double = 0.0,
    val used: Double = 0.0,
    val wasted: Double = 0.0,
    /** Net value of count corrections: negative means stock went missing. */
    val countCorrections: Double = 0.0,
    val topUsed: List<ReportLine> = emptyList(),
    val topWasted: List<ReportLine> = emptyList(),
    val usedByCategory: List<ReportLine> = emptyList(),
) {
    /** Share of stock that went out as waste rather than use, or null when nothing went out. */
    val wastePercent: Double?
        get() = if (used + wasted > 0) wasted / (used + wasted) * 100 else null
}

/** Money values use the cost recorded on each movement, so past reports stay right after price changes. */
fun buildReport(movements: List<ReportMovement>, top: Int = 5): Report {
    fun value(m: ReportMovement) = abs(m.quantity) * m.unitCost
    fun ofType(type: MovementType) = movements.filter { it.type == type }

    fun ranked(list: List<ReportMovement>): List<ReportLine> =
        list.groupBy { it.itemId }
            .map { (_, rows) ->
                val first = rows.first()
                ReportLine(first.itemName, rows.sumOf { value(it) }, "${formatQuantity(rows.sumOf { abs(it.quantity) })} ${first.unitAbbreviation}")
            }
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(top)

    val usage = ofType(MovementType.USAGE)
    val waste = ofType(MovementType.WASTE)
    return Report(
        received = ofType(MovementType.STOCK_IN).sumOf { value(it) },
        used = usage.sumOf { value(it) },
        wasted = waste.sumOf { value(it) },
        countCorrections = ofType(MovementType.ADJUSTMENT).sumOf { it.quantity * it.unitCost },
        topUsed = ranked(usage),
        topWasted = ranked(waste),
        usedByCategory = usage
            .groupBy { it.categoryName ?: UNCATEGORISED }
            .map { (name, rows) -> ReportLine(name, rows.sumOf { value(it) }) }
            .filter { it.value > 0 }
            .sortedByDescending { it.value },
    )
}

/** What the stock on the shelf is worth now, per category. Negative stock counts as zero. */
fun stockValueByCategory(items: List<ItemWithStock>): List<ReportLine> =
    items
        .groupBy { it.categoryName ?: UNCATEGORISED }
        .map { (name, list) -> ReportLine(name, list.sumOf { it.stockValue.coerceAtLeast(0.0) }) }
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

private const val UNCATEGORISED = "No category"
