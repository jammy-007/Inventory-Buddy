package com.kitcheninventory.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

/** Lets the user pick the currency for costs and stock value. Applies right away. */
@Composable
fun CurrencyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val selected = AppCurrency.selected
    val phoneDefault = NumberFormat.getCurrencyInstance().currency
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item {
                    CurrencyRow(
                        label = "Phone default" + (phoneDefault?.let { " (${it.currencyCode})" } ?: ""),
                        selected = selected == null,
                        onClick = { AppCurrency.select(context, null) },
                    )
                }
                items(currencyOptions, key = { it.code }) { option ->
                    CurrencyRow(
                        label = "${option.symbol.trim()}  ${option.name} (${option.code})",
                        selected = selected?.code == option.code,
                        onClick = { AppCurrency.select(context, option) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun CurrencyRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
