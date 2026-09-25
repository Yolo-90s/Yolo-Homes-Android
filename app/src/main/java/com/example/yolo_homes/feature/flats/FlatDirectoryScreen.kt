package com.example.yolo_homes.feature.flats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.UnitTypes
import com.example.yolo_homes.data.model.Flat
import com.example.yolo_homes.data.model.Role
import com.example.yolo_homes.ui.components.GradientHeader
import com.example.yolo_homes.ui.components.LabeledDropdown
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.SurfaceCard
import com.example.yolo_homes.ui.theme.LightPrimary
import com.example.yolo_homes.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatDirectoryScreen(
    onBack: () -> Unit,
    onOpenFlat: (String) -> Unit,
    viewModel: FlatDirectoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Flat?>(null) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is FlatSaveState.Saved -> {
                editing = null
                creating = false
                snackbar.showSnackbar("Saved")
                viewModel.clearSaveState()
            }
            is FlatSaveState.Error -> {
                snackbar.showSnackbar(s.message)
                viewModel.clearSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flat Directory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add unit")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GradientHeader {
                    Text(
                        "Flat Directory",
                        style = MaterialTheme.typography.headlineSmall,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        "${state.residentFlats.size} residential units" +
                            if (state.staffUnits.isNotEmpty()) " · ${state.staffUnits.size} staff" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            item { SectionHeader("Residents", Modifier.padding(horizontal = 16.dp)) }
            items(state.residentFlats, key = { it.id.ifBlank { it.flatNo } }) { flat ->
                FlatRow(
                    flat = flat,
                    onClick = { onOpenFlat(flat.id.ifBlank { flat.flatNo }) },
                    onEdit = if (state.isAdmin) ({ editing = flat }) else null,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (state.staffUnits.isNotEmpty()) {
                item { SectionHeader("Staff", Modifier.padding(horizontal = 16.dp)) }
                items(state.staffUnits, key = { it.id.ifBlank { it.flatNo } }) { flat ->
                    FlatRow(
                        flat = flat,
                        onClick = { onOpenFlat(flat.id.ifBlank { flat.flatNo }) },
                        onEdit = if (state.isAdmin) ({ editing = flat }) else null,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (creating) {
        AddEditFlatDialog(
            flat = null,
            saving = saveState is FlatSaveState.Saving,
            onDismiss = { creating = false },
            onSave = { viewModel.save(it, isNew = true) }
        )
    }
    editing?.let { flat ->
        AddEditFlatDialog(
            flat = flat,
            saving = saveState is FlatSaveState.Saving,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it, isNew = false) }
        )
    }
}

@Composable
private fun FlatRow(
    flat: Flat,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    SurfaceCard(modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background((if (flat.isStaff) Warning else LightPrimary).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (flat.isStaff) Icons.Outlined.Security else Icons.Outlined.Home,
                    contentDescription = null,
                    tint = if (flat.isStaff) Warning else LightPrimary
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(flat.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    flat.occupantName.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (flat.isStaff) {
                AssistChip(onClick = onClick, label = { Text("Staff") })
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit ${flat.displayName}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditFlatDialog(
    flat: Flat?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Flat) -> Unit
) {
    val isNew = flat == null
    var flatNo by remember(flat) { mutableStateOf(flat?.flatNo ?: "") }
    var block by remember(flat) { mutableStateOf(flat?.block ?: "") }
    var unitType by remember(flat) { mutableStateOf(flat?.unitType ?: UnitTypes.RESIDENT) }
    var ownerName by remember(flat) { mutableStateOf(flat?.ownerName ?: "") }
    var ownerPhone by remember(flat) { mutableStateOf(flat?.ownerPhone ?: "") }
    var tenantName by remember(flat) { mutableStateOf(flat?.tenantName ?: "") }
    var tenantPhone by remember(flat) { mutableStateOf(flat?.tenantPhone ?: "") }
    var email by remember(flat) { mutableStateOf(flat?.email ?: "") }
    var role by remember(flat) {
        mutableStateOf(Role.from(flat?.role).let { if (it == Role.UNKNOWN) Role.OWNER else it })
    }
    val isStaff = unitType == UnitTypes.STAFF
    val roleOptions = listOf(Role.DEVELOPER, Role.ADMIN, Role.OWNER, Role.TENANT)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (isNew) "Add Unit" else "Edit ${flat?.displayName}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = flatNo,
                    onValueChange = { flatNo = it },
                    label = { Text("Unit No. (e.g. 101 or G01)") },
                    enabled = isNew,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = block,
                    onValueChange = { block = it },
                    label = { Text("Block (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LabeledDropdown(
                    label = "Unit Type",
                    options = listOf(UnitTypes.RESIDENT, UnitTypes.STAFF),
                    selected = unitType,
                    optionLabel = { if (it == UnitTypes.STAFF) "Staff / Watchman" else "Resident Flat" },
                    onSelect = { unitType = it }
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(if (isStaff) "Staff Name" else "Owner Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isStaff) {
                    OutlinedTextField(
                        value = tenantName,
                        onValueChange = { tenantName = it },
                        label = { Text("Tenant Name (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tenantPhone,
                        onValueChange = { tenantPhone = it },
                        label = { Text("Tenant Phone (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Sign-in email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isStaff) {
                    LabeledDropdown(
                        label = "Role",
                        options = roleOptions,
                        selected = role,
                        optionLabel = { it.label },
                        onSelect = { role = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Flat(
                            id = flat?.id.orEmpty(),
                            flatNo = flatNo.trim(),
                            block = block.trim(),
                            ownerName = ownerName.trim(),
                            ownerPhone = ownerPhone.trim(),
                            tenantName = if (isStaff) "" else tenantName.trim(),
                            tenantPhone = if (isStaff) "" else tenantPhone.trim(),
                            role = if (isStaff) "resident" else role.name.lowercase(),
                            email = email.trim(),
                            unitType = unitType
                        )
                    )
                },
                enabled = !saving && flatNo.isNotBlank() && ownerName.isNotBlank()
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}
