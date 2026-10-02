package com.kitcheninventory.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitcheninventory.InventoryApp
import com.kitcheninventory.ui.AppShellViewModel
import com.kitcheninventory.ui.history.HistoryViewModel
import com.kitcheninventory.ui.items.ItemEditViewModel
import com.kitcheninventory.ui.items.ItemListViewModel
import com.kitcheninventory.ui.stock.CountViewModel
import com.kitcheninventory.ui.stock.RecordViewModel
import com.kitcheninventory.ui.suppliers.SupplierListViewModel

/** Builds every ViewModel with the app's repository; no DI framework to keep the app small. */
val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { ItemListViewModel(repo()) }
    initializer { ItemEditViewModel(repo(), createSavedStateHandle()) }
    initializer { SupplierListViewModel(repo()) }
    initializer { AppShellViewModel(repo()) }
    initializer { RecordViewModel(repo()) }
    initializer { CountViewModel(repo()) }
    initializer { HistoryViewModel(repo()) }
}

private fun androidx.lifecycle.viewmodel.CreationExtras.repo() =
    (this[APPLICATION_KEY] as InventoryApp).repository
