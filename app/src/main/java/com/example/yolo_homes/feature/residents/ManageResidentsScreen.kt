package com.example.yolo_homes.feature.residents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.Role
import com.example.yolo_homes.ui.components.LabeledDropdown
import com.example.yolo_homes.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageResidentsScreen(
    onBack: () -> Unit,
    viewModel: ManageResidentsViewModel = hiltViewModel()
) {
    val flats by viewModel.flats.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Flat?>(null) }

    LaunchedEffect(status) {
        when (val s = status) {
            is ResidentSaveStatus.Saved -> {
                editing = null
                snackbar.showSnackbar("Flat ${s.flatNo} updated")
                viewModel.clearStatus()
            }
            is ResidentSaveStatus.Error -> {
                snackbar.showSnackbar(s.message)
                viewModel.clearStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Residents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Link each resident's Google sign-in email to their flat and set their role. " +
                        "They'll get that access on next login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(flats, key = { it.id }) { flat ->
                SurfaceCard(Modifier.clickable { editing = flat }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Flat ${flat.flatNo}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                flat.occupantName.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                flat.email.ifBlank { "No email linked" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (flat.email.isBlank()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        AssistChip(
                            onClick = { editing = flat },
                            label = { Text(Role.from(flat.role).label) }
                        )
                    }
                }
            }
        }
    }

    editing?.let { flat ->
        EditResidentDialog(
            flat = flat,
            saving = status is ResidentSaveStatus.Saving,
            onDismiss = { if (status !is ResidentSaveStatus.Saving) editing = null },
            onSave = { email, role -> viewModel.save(flat.flatNo, email, role.name.lowercase()) }
        )
    }
}

@Composable
private fun EditResidentDialog(
    flat: Flat,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (email: String, role: Role) -> Unit
) {
    var email by remember(flat.id) { mutableStateOf(flat.email) }
    var role by remember(flat.id) {
        mutableStateOf(Role.from(flat.role).let { if (it == Role.UNKNOWN) Role.OWNER else it })
    }
    val roleOptions = listOf(Role.DEVELOPER, Role.ADMIN, Role.OWNER, Role.TENANT)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flat ${flat.flatNo} • ${flat.occupantName.ifBlank { "Resident" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LabeledDropdown(
                    label = "Role",
                    options = roleOptions,
                    selected = role,
                    optionLabel = { it.label },
                    onSelect = { role = it }
                )
                Text(
                    "Developer & Admin can capture data and edit settings. Owner & Tenant are read-only, " +
                        "scoped to their own flat.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(email, role) },
                enabled = !saving && email.isNotBlank()
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}
