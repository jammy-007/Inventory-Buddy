package com.kitcheninventory.ui.items

import com.kitcheninventory.data.db.ItemEntity
import com.kitcheninventory.ui.common.parseNumber

/** The add/edit form as the user typed it. Numbers stay as text until saved. */
data class ItemForm(
    val name: String = "",
    val unitId: Long? = null,
    val categoryId: Long? = null,
    val supplierId: Long? = null,
    val unitCost: String = "",
    val parLevel: String = "",
    val openingStock: String = "",
    val orderUpTo: String = "",
    val barcode: String = "",
    val notes: String = "",
)

data class ItemFormErrors(
    val name: String? = null,
    val unit: String? = null,
    val unitCost: String? = null,
    val parLevel: String? = null,
    val openingStock: String? = null,
    val orderUpTo: String? = null,
    val barcode: String? = null,
) {
    val isEmpty: Boolean get() = listOf(name, unit, unitCost, parLevel, openingStock, orderUpTo, barcode).all { it == null }
}

/**
 * Checks the form; [nameTaken] and [barcodeTaken] say whether another active item already uses
 * the name or barcode.
 */
fun validate(form: ItemForm, isNew: Boolean, nameTaken: Boolean, barcodeTaken: Boolean = false): ItemFormErrors = ItemFormErrors(
    name = when {
        form.name.isBlank() -> "Enter a name"
        nameTaken -> "An item with this name already exists"
        else -> null
    },
    unit = if (form.unitId == null) "Choose a unit" else null,
    unitCost = numberError(form.unitCost),
    parLevel = numberError(form.parLevel),
    openingStock = if (isNew) numberError(form.openingStock) else null,
    orderUpTo = numberError(form.orderUpTo) ?: run {
        val target = parseNumber(form.orderUpTo) ?: 0.0
        val par = parseNumber(form.parLevel) ?: 0.0
        if (target > 0 && target <= par) "Must be above the reorder level" else null
    },
    barcode = if (barcodeTaken) "Another item has this barcode" else null,
)

private fun numberError(text: String): String? {
    val value = parseNumber(text) ?: return "Enter a number"
    return if (value < 0) "Can't be negative" else null
}

/** Applies a valid form to [base] (a new item, or the one being edited). */
fun ItemForm.toEntity(base: ItemEntity?): ItemEntity {
    val unit = requireNotNull(unitId)
    val cost = parseNumber(unitCost) ?: 0.0
    val par = parseNumber(parLevel) ?: 0.0
    val target = parseNumber(orderUpTo) ?: 0.0
    val code = barcode.trim().ifEmpty { null }
    return base?.copy(
        name = name.trim(), unitId = unit, categoryId = categoryId, supplierId = supplierId,
        unitCost = cost, parLevel = par, orderUpTo = target, barcode = code, notes = notes.trim(),
    ) ?: ItemEntity(
        name = name.trim(), unitId = unit, categoryId = categoryId, supplierId = supplierId,
        unitCost = cost, parLevel = par, orderUpTo = target, barcode = code, notes = notes.trim(),
    )
}

fun ItemEntity.toForm(): ItemForm = ItemForm(
    name = name,
    unitId = unitId,
    categoryId = categoryId,
    supplierId = supplierId,
    unitCost = if (unitCost == 0.0) "" else unitCost.toPlainString(),
    parLevel = if (parLevel == 0.0) "" else parLevel.toPlainString(),
    orderUpTo = if (orderUpTo == 0.0) "" else orderUpTo.toPlainString(),
    barcode = barcode.orEmpty(),
    notes = notes,
)

private fun Double.toPlainString(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toBigDecimal().stripTrailingZeros().toPlainString()
