package com.kitcheninventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kitcheninventory.ui.InventoryNavHost
import com.kitcheninventory.ui.theme.InventoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InventoryTheme {
                InventoryNavHost()
            }
        }
    }
}
