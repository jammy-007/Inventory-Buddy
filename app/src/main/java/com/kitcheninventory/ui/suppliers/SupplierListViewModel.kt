package com.kitcheninventory.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.data.repo.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupplierListViewModel(private val repo: InventoryRepository) : ViewModel() {
    val suppliers: StateFlow<List<SupplierEntity>?> =
        repo.supplierList.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The supplier open in the edit dialog, kept here so it survives screen rotation. */
    private val _editing = MutableStateFlow<SupplierEntity?>(null)
    val editing: StateFlow<SupplierEntity?> = _editing.asStateFlow()

    fun startAdd() { _editing.value = SupplierEntity(name = "") }
    fun startEdit(supplier: SupplierEntity) { _editing.value = supplier }
    fun updateDraft(supplier: SupplierEntity) { _editing.value = supplier }
    fun cancelEdit() { _editing.value = null }

    fun save() {
        val draft = _editing.value ?: return
        if (draft.name.isBlank()) return
        _editing.value = null
        viewModelScope.launch {
            repo.saveSupplier(
                draft.copy(
                    name = draft.name.trim(),
                    phone = draft.phone.trim(),
                    email = draft.email.trim(),
                    notes = draft.notes.trim(),
                )
            )
        }
    }

    /** Items that used this supplier keep existing, with no supplier set. */
    fun delete(supplier: SupplierEntity) {
        _editing.value = null
        viewModelScope.launch { repo.deleteSupplier(supplier) }
    }
}
