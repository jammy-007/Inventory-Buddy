package com.kitcheninventory.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.ui.common.AppIcons
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.ConfirmDialog
import com.kitcheninventory.ui.common.Dropdown
import com.kitcheninventory.ui.common.QuantityPill
import com.kitcheninventory.ui.common.SectionCard
import com.kitcheninventory.ui.common.currencySymbol
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.rememberBarcodeScanner

/** [onDone] gets a short message to show on the list, or null when the user just backed out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    onDone: (message: String?) -> Unit,
    viewModel: ItemEditViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    val currency = remember { currencySymbol() }
    val scanBarcode = rememberBarcodeScanner { code -> viewModel.edit { it.copy(barcode = code) } }

    LaunchedEffect(state.finished) { if (state.finished) onDone(state.resultMessage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = { onDone(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew && !state.loading) {
                        IconButton(onClick = { confirmArchive = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove item")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!state.loading) {
                Surface(tonalElevation = 3.dp) {
                    Column {
                        HorizontalDivider()
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.saving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(48.dp),
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(if (state.isNew) "Add item" else "Save changes")
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val form = state.form
        val errors = state.errors
        val unit = units.firstOrNull { it.id == form.unitId }
        val unitLabel = unit?.abbreviation.orEmpty()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Details") {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> viewModel.edit { it.copy(name = v) } },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Tomatoes") },
                    isError = errors.name != null,
                    supportingText = if (errors.name != null) { { Text(errors.name) } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Dropdown(
                    label = "Unit",
                    options = units,
                    selected = unit,
                    optionLabel = { "${it.name} (${it.abbreviation})" },
                    onSelect = { u -> viewModel.edit { it.copy(unitId = u.id) } },
                    placeholder = "Choose a unit",
                    isError = errors.unit != null,
                    supportingText = errors.unit,
                )
                Dropdown(
                    label = "Category",
                    options = listOf(null) + categories,
                    selected = categories.firstOrNull { it.id == form.categoryId },
                    optionLabel = { it?.name ?: "None" },
                    onSelect = { c -> viewModel.edit { it.copy(categoryId = c?.id) } },
                    placeholder = "None",
                )
                Dropdown(
                    label = "Supplier",
                    options = listOf(null) + suppliers,
                    selected = suppliers.firstOrNull { it.id == form.supplierId },
                    optionLabel = { it?.name ?: "None" },
                    onSelect = { s -> viewModel.edit { it.copy(supplierId = s?.id) } },
                    placeholder = "None",
                    supportingText = if (suppliers.isEmpty()) "Add suppliers from the main screen" else null,
                )
                OutlinedTextField(
                    value = form.barcode,
                    onValueChange = { v -> viewModel.edit { it.copy(barcode = v) } },
                    label = { Text("Barcode (optional)") },
                    isError = errors.barcode != null,
                    supportingText = { Text(errors.barcode ?: "Scan the pack to find this item faster") },
                    trailingIcon = {
                        IconButton(onClick = scanBarcode) {
                            Icon(AppIcons.Barcode, contentDescription = "Scan barcode")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard("Stock and cost") {
                if (!state.isNew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "In stock now",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        QuantityPill(
                            text = "${formatQuantity(state.onHand)} $unitLabel".trim(),
                            low = state.isLowStock,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = if (unit != null) "Cost per ${unit.abbreviation}" else "Cost per unit",
                        value = form.unitCost,
                        error = errors.unitCost,
                        onChange = { v -> viewModel.edit { it.copy(unitCost = v) } },
                        modifier = Modifier.weight(1f),
                        prefix = currency,
                    )
                    NumberField(
                        label = "Reorder at",
                        value = form.parLevel,
                        error = errors.parLevel,
                        onChange = { v -> viewModel.edit { it.copy(parLevel = v) } },
                        modifier = Modifier.weight(1f),
                        suffix = unitLabel,
                        help = "Low-stock alert level",
                    )
                }
                NumberField(
                    label = "Order up to",
                    value = form.orderUpTo,
                    error = errors.orderUpTo,
                    onChange = { v -> viewModel.edit { it.copy(orderUpTo = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = unitLabel,
                    help = "Orders refill to this level. Blank means twice the reorder level",
                )
                if (state.isNew) {
                    NumberField(
                        label = "Current stock",
                        value = form.openingStock,
                        error = errors.openingStock,
                        onChange = { v -> viewModel.edit { it.copy(openingStock = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        suffix = unitLabel,
                        help = "How much you have right now",
                    )
                }
            }

            SectionCard("Notes") {
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { v -> viewModel.edit { it.copy(notes = v) } },
                    placeholder = { Text("Storage, brand, pack size…") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmArchive) {
        ConfirmDialog(
            title = "Remove ${state.form.name.ifBlank { "this item" }}?",
            message = "It will disappear from your list. Its stock history is kept for reports.",
            confirmLabel = "Remove",
            onConfirm = {
                confirmArchive = false
                viewModel.archive()
            },
            onDismiss = { confirmArchive = false },
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    help: String? = null,
    prefix: String = "",
    suffix: String = "",
) {
    val hint = error ?: help
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (hint != null) { { Text(hint) } } else null,
        prefix = if (prefix.isNotEmpty()) { { Text(prefix) } } else null,
        suffix = if (suffix.isNotEmpty()) { { Text(suffix) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}
