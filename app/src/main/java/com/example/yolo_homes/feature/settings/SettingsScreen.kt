package com.example.yolo_homes.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SectionHeader
import com.example.yolo_homes.ui.components.SurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    onManageResidents: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var freeLiters by remember(settings) { mutableStateOf(settings.freeLiters.toString()) }
    var rate by remember(settings) { mutableStateOf(settings.ratePerExcessLiter.toString()) }
    var currency by remember(settings) { mutableStateOf(settings.currency) }
    var readingFrequency by remember(settings) { mutableStateOf(settings.readingFrequency) }
    var waterSource by remember(settings) { mutableStateOf(settings.waterSource) }
    var sendReminder by remember(settings) { mutableStateOf(settings.sendReminder) }
    var sendBillMessage by remember(settings) { mutableStateOf(settings.sendBillMessage) }

    LaunchedEffect(saveState) {
        if (saveState is SettingsSaveState.Saved) {
            viewModel.resetSaveState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isAdmin) {
                SurfaceCard {
                    Text(
                        "Only administrators can edit apartment settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isAdmin) {
                SectionHeader("Access")
                SurfaceCard(Modifier.clickable { onManageResidents() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text("Manage Residents", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Link emails & assign roles per flat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null)
                    }
                }
            }

            SectionHeader("Water Billing")
            field("Rate per Liter (${currency.ifBlank { "₹" }})", rate, isAdmin, KeyboardType.Decimal) { rate = it }
            field("Free / Exclude Limit (L)", freeLiters, isAdmin, KeyboardType.Decimal) { freeLiters = it }
            Text(
                "Water meters ship showing ~100+ L. The first ${freeLiters.ifBlank { "200" }} L are excluded — " +
                    "billing counts only liters above this baseline, charged at the rate per liter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            field("Currency Symbol", currency, isAdmin, KeyboardType.Text) { currency = it }
            field("Reading Frequency", readingFrequency, isAdmin, KeyboardType.Text) { readingFrequency = it }
            field("Water Source", waterSource, isAdmin, KeyboardType.Text) { waterSource = it }

            SectionHeader("Notifications")
            toggle("Send Reminders", sendReminder, isAdmin) { sendReminder = it }
            toggle("Send Bill Message", sendBillMessage, isAdmin) { sendBillMessage = it }

            (saveState as? SettingsSaveState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (isAdmin) {
                PrimaryButton(
                    text = "Save Settings",
                    loading = saveState is SettingsSaveState.Saving,
                    onClick = {
                        viewModel.save(
                            settings.copy(
                                freeLiters = freeLiters.toDoubleOrNull() ?: settings.freeLiters,
                                ratePerExcessLiter = rate.toDoubleOrNull() ?: settings.ratePerExcessLiter,
                                currency = currency.ifBlank { settings.currency },
                                readingFrequency = readingFrequency,
                                waterSource = waterSource,
                                sendReminder = sendReminder,
                                sendBillMessage = sendBillMessage
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun field(
    label: String,
    value: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun toggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}
