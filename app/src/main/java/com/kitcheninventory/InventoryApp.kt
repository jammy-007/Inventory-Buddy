package com.kitcheninventory

import android.app.Application
import com.kitcheninventory.data.db.InventoryDatabase
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.common.AppCurrency

class InventoryApp : Application() {
    /** Created on first use so app start stays fast. */
    val repository: InventoryRepository by lazy {
        InventoryRepository(InventoryDatabase.create(this))
    }

    override fun onCreate() {
        super.onCreate()
        AppCurrency.load(this)
    }
}
