package com.kitcheninventory.ui.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.ItemEntity
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.data.db.isLowStock
import com.kitcheninventory.data.db.UnitEntity
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.common.parseNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemEditState(
    val isNew: Boolean,
    val loading: Boolean = !isNew,
    val form: ItemForm = ItemForm(),
    val errors: ItemFormErrors = ItemFormErrors(),
    /** Current stock, shown read-only when editing. */
    val onHand: Double = 0.0,
    val saving: Boolean = false,
    val finished: Boolean = false,
    /** Shown on the item list after the screen closes, e.g. "Tomatoes added". */
    val resultMessage: String? = null,
) {
    val isLowStock: Boolean get() = isLowStock(onHand, parseNumber(form.parLevel) ?: 0.0)
}

class ItemEditViewModel(
    private val repo: InventoryRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val itemId: Long = savedState.get<Long>("itemId") ?: 0L
    private var original: ItemEntity? = null

    /** A new item can start with a barcode that was just scanned on the item list. */
    private val _state = MutableStateFlow(
        ItemEditState(isNew = itemId == 0L, form = ItemForm(barcode = savedState.get<String>("barcode").orEmpty())),
    )
    val state: StateFlow<ItemEditState> = _state.asStateFlow()

    val units: StateFlow<List<UnitEntity>> = repo.units.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<CategoryEntity>> = repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val suppliers: StateFlow<List<SupplierEntity>> = repo.supplierList.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Quick-add items not already in the inventory (compared by name, ignoring case). */
    val quickAdd: StateFlow<List<QuickItem>> = repo.activeItems
        .map { items ->
            val taken = items.map { it.name.lowercase() }.toSet()
            quickItems.filter { it.name.lowercase() !in taken }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (itemId != 0L) {
            viewModelScope.launch {
                val item = repo.getItem(itemId)
                if (item == null) {
                    _state.update { it.copy(finished = true) }
                } else {
                    original = item
                    _state.update { it.copy(loading = false, form = item.toForm(), onHand = repo.onHand(itemId)) }
                }
            }
        }
    }

    fun edit(change: (ItemForm) -> ItemForm) {
        _state.update { it.copy(form = change(it.form)) }
    }

    fun quickFill(item: QuickItem) = edit { it.withQuickItem(item, units.value, categories.value) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val form = current.form
            val taken = form.name.isNotBlank() && repo.isNameTaken(form.name, excludeId = itemId)
            val barcodeTaken = form.barcode.isNotBlank() && repo.isBarcodeTaken(form.barcode, excludeId = itemId)
            val errors = validate(form, current.isNew, taken, barcodeTaken)
            if (!errors.isEmpty) {
                _state.update { it.copy(errors = errors, saving = false) }
                return@launch
            }
            val message = if (current.isNew) {
                repo.addItem(form.toEntity(null), parseNumber(form.openingStock) ?: 0.0)
                "${form.name.trim()} added"
            } else {
                repo.updateItem(form.toEntity(original))
                "Changes saved"
            }
            _state.update { it.copy(saving = false, finished = true, resultMessage = message) }
        }
    }

    fun archive() {
        if (itemId == 0L) return
        viewModelScope.launch {
            repo.archiveItem(itemId)
            val name = _state.value.form.name.trim().ifEmpty { "Item" }
            _state.update { it.copy(finished = true, resultMessage = "$name removed") }
        }
    }
}
