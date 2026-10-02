package com.kitcheninventory.ui

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kitcheninventory.data.repo.InventoryRepository
import com.kitcheninventory.ui.common.AppIcons
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.history.HistoryScreen
import com.kitcheninventory.ui.items.ItemEditScreen
import com.kitcheninventory.ui.items.ItemListScreen
import com.kitcheninventory.ui.orders.OrdersScreen
import com.kitcheninventory.ui.reports.ReportsScreen
import com.kitcheninventory.ui.stock.CountScreen
import com.kitcheninventory.ui.stock.RecordScreen
import com.kitcheninventory.ui.suppliers.SupplierListScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object Routes {
    const val ITEMS = "items"
    const val ITEM_EDIT = "item/{itemId}?barcode={barcode}"
    const val SUPPLIERS = "suppliers"
    const val ORDERS = "orders"
    const val RECORD = "record"
    const val COUNT = "count"
    const val HISTORY = "history"
    const val REPORTS = "reports"

    /** Use 0 to add a new item, optionally with a scanned [barcode] filled in. */
    fun itemEdit(itemId: Long, barcode: String? = null) =
        if (barcode == null) "item/$itemId" else "item/$itemId?barcode=${Uri.encode(barcode)}"
}

/** Key the edit screen uses to hand a snackbar message back to the item list. */
private const val MESSAGE_KEY = "message"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    STOCK(Routes.ITEMS, "Stock", Icons.Filled.Home),
    RECORD(Routes.RECORD, "Record", Icons.Filled.Edit),
    COUNT(Routes.COUNT, "Count", Icons.Filled.CheckCircle),
    HISTORY(Routes.HISTORY, "History", Icons.Filled.DateRange),
    REPORTS(Routes.REPORTS, "Reports", AppIcons.Chart),
}

/** App-wide state for the bottom bar: how many items are low, shown as a badge on Stock. */
class AppShellViewModel(repo: InventoryRepository) : ViewModel() {
    val lowStockCount: StateFlow<Int> = repo.activeItems
        .map { items -> items.count { it.isLowStock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun InventoryNavHost(shell: AppShellViewModel = viewModel(factory = AppViewModelFactory)) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val lowStockCount by shell.lowStockCount.collectAsStateWithLifecycle()
    val showTabs = Tab.entries.any { it.route == route }

    Scaffold(
        // Each screen has its own Scaffold that handles the status bar; this one only adds the tabs.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = {
                                if (tab == Tab.STOCK && lowStockCount > 0) {
                                    BadgedBox(badge = { Badge { Text(lowStockCount.toString()) } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = Routes.ITEMS,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable(Routes.ITEMS) { entry ->
                val message by entry.savedStateHandle
                    .getStateFlow<String?>(MESSAGE_KEY, null)
                    .collectAsStateWithLifecycle()
                ItemListScreen(
                    onAddItem = { barcode -> nav.navigateFrom(entry, Routes.itemEdit(0, barcode)) },
                    onOpenItem = { nav.navigateFrom(entry, Routes.itemEdit(it)) },
                    onOpenSuppliers = { nav.navigateFrom(entry, Routes.SUPPLIERS) },
                    onOpenOrders = { nav.navigateFrom(entry, Routes.ORDERS) },
                    message = message,
                    onMessageShown = { entry.savedStateHandle[MESSAGE_KEY] = null },
                )
            }
            composable(
                Routes.ITEM_EDIT,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.LongType },
                    navArgument("barcode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                ItemEditScreen(
                    onDone = { message ->
                        if (nav.isShowing(entry)) {
                            if (message != null) nav.previousBackStackEntry?.savedStateHandle?.set(MESSAGE_KEY, message)
                            nav.popBackStack()
                        }
                    },
                )
            }
            composable(Routes.SUPPLIERS) { entry ->
                SupplierListScreen(onBack = { if (nav.isShowing(entry)) nav.popBackStack() })
            }
            composable(Routes.ORDERS) { entry ->
                OrdersScreen(onBack = { if (nav.isShowing(entry)) nav.popBackStack() })
            }
            composable(Routes.RECORD) { RecordScreen() }
            composable(Routes.COUNT) { CountScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.REPORTS) { ReportsScreen() }
        }
    }
}

/** Keeps one copy of each tab and remembers where the user was in it. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/*
 * Only the screen on top of the stack may navigate. Without this, a quick double tap on Back pops
 * two screens and leaves a blank app, and a double tap on an item opens it twice.
 */
private fun NavHostController.isShowing(entry: NavBackStackEntry) = currentBackStackEntry?.id == entry.id

private fun NavHostController.navigateFrom(entry: NavBackStackEntry, route: String) {
    if (isShowing(entry)) navigate(route)
}
