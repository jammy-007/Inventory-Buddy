package com.kitcheninventory.ui.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.ui.common.AppViewModelFactory
import com.kitcheninventory.ui.common.ConfirmDialog
import com.kitcheninventory.ui.common.EmptyState
import com.kitcheninventory.ui.common.InitialAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    viewModel: SupplierListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add supplier") },
            )
        },
    ) { padding ->
        val list = suppliers
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (list != null && list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Person,
                        title = "No suppliers yet",
                        message = "Add the vendors you buy from so you can link them to items.",
                    )
                }
            }
            items(list.orEmpty(), key = { it.id }) { supplier ->
                SupplierRow(supplier, onClick = { viewModel.startEdit(supplier) })
            }
        }
    }

    editing?.let { draft ->
        SupplierDialog(
            draft = draft,
            onChange = viewModel::updateDraft,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::save,
            onDelete = if (draft.id != 0L) { { viewModel.delete(draft) } } else null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierRow(supplier: SupplierEntity, onClick: () -> Unit) {
    val contact = listOf(supplier.phone, supplier.email).filter { it.isNotBlank() }.joinToString(" · ")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            leadingContent = {
                InitialAvatar(
                    supplier.name,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            },
            headlineContent = { Text(supplier.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = if (contact.isNotEmpty()) {
                { Text(contact, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else null,
        )
    }
}

@Composable
private fun SupplierDialog(
    draft: SupplierEntity,
    onChange: (SupplierEntity) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    var showNameError by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = "Delete ${draft.name.ifBlank { "this supplier" }}?",
            message = "Items from this supplier stay in your inventory, with no supplier set.",
            confirmLabel = "Delete",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == 0L) "Add supplier" else "Edit supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onChange(draft.copy(name = it)); showNameError = false },
                    label = { Text("Name") },
                    isError = showNameError,
                    supportingText = if (showNameError) { { Text("Enter a name") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = draft.phone,
                    onValueChange = { onChange(draft.copy(phone = it)) },
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = draft.email,
                    onValueChange = { onChange(draft.copy(email = it)) },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onChange(draft.copy(notes = it)) },
                    label = { Text("Notes") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (draft.name.isBlank()) showNameError = true else onSave() }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
