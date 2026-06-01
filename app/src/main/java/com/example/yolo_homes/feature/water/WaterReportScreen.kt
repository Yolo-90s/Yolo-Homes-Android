package com.example.yolo_homes.feature.water

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.core.PdfExporter
import com.example.yolo_homes.data.model.AppSettings
import com.example.yolo_homes.feature.water.components.MonthFilterBar
import com.example.yolo_homes.ui.components.EmptyState
import com.example.yolo_homes.ui.components.PrimaryButton

/** Columns available in the water report. Each knows its header, on-screen width, and how to render a cell. */
enum class WaterReportColumn(val header: String, val width: Dp) {
    FLAT("Flat", 90.dp),
    BLOCK("Block", 70.dp),
    OWNER("Owner", 130.dp),
    TENANT("Tenant", 130.dp),
    PREV("Prev (L)", 80.dp),
    CURRENT("Current (L)", 90.dp),
    USAGE("Usage (L)", 80.dp),
    BILLABLE("Billable (L)", 90.dp),
    RATE("Rate / L", 80.dp),
    AMOUNT("Amount", 100.dp),
    DATE("Date", 110.dp),
    STATUS("Status", 90.dp);

    fun value(fc: FlatConsumption, settings: AppSettings, currency: String): String = when (this) {
        FLAT -> fc.flat.displayName.ifBlank { "—" }
        BLOCK -> fc.flat.block.ifBlank { "—" }
        OWNER -> fc.flat.ownerName.ifBlank { "—" }
        TENANT -> fc.flat.tenantName.ifBlank { "—" }
        PREV -> if (fc.hasReading) "%,.0f".format(fc.previousReading) else "—"
        CURRENT -> if (fc.hasReading) "%,.0f".format(fc.currentReading) else "—"
        USAGE -> if (fc.hasReading) "%,.0f".format(fc.liters) else "—"
        BILLABLE -> if (fc.hasReading) "%,.0f".format(fc.excessLiters) else "—"
        RATE -> Formatters.currencyPrecise(settings.ratePerExcessLiter, currency)
        AMOUNT -> if (fc.hasReading) Formatters.currency(fc.amount, currency) else "—"
        DATE -> if (fc.hasReading) Formatters.shortDate(fc.date) else "—"
        STATUS -> when {
            !fc.hasReading -> "No reading"
            fc.edited -> "Edited"
            else -> "Read"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReportScreen(
    onBack: () -> Unit,
    viewModel: WaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Column visibility — all on by default; at least one must stay visible.
    val visible = remember {
        mutableStateMapOf<WaterReportColumn, Boolean>().apply {
            WaterReportColumn.entries.forEach { put(it, true) }
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val cols = WaterReportColumn.entries.filter { visible[it] == true }

    fun exportPdf() {
        if (cols.isEmpty()) return
        val headers = cols.map { it.header }
        val body = state.flatConsumption.map { fc -> cols.map { it.value(fc, state.settings, state.currency) } }
        // Totals row across the numeric columns.
        val totalsRow = cols.map { c ->
            when (c) {
                WaterReportColumn.FLAT -> "TOTAL"
                WaterReportColumn.USAGE -> "%,.0f".format(state.flatConsumption.sumOf { it.liters })
                WaterReportColumn.BILLABLE -> "%,.0f".format(state.flatConsumption.sumOf { it.excessLiters })
                WaterReportColumn.AMOUNT -> Formatters.currency(state.flatConsumption.sumOf { it.amount }, state.currency)
                else -> ""
            }
        }
        val file = PdfExporter.exportTable(
            context = context,
            fileName = "water_report_${state.selectedPeriod}.pdf",
            title = "Water Report",
            subtitle = "${state.settings.apartmentName} • ${Formatters.monthLabel(state.selectedPeriod)}",
            headers = headers,
            rows = body + listOf(totalsRow),
            footerNote = "Generated by Yolo-Home's • ${state.flatConsumption.size} flats"
        )
        PdfExporter.share(context, file, "Share water report")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.ViewColumn, contentDescription = "Choose columns")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        WaterReportColumn.entries.forEach { c ->
                            val checked = visible[c] == true
                            DropdownMenuItem(
                                text = { Text(c.header) },
                                onClick = {
                                    // Don't let the user hide the last remaining column.
                                    if (!(checked && cols.size == 1)) visible[c] = !checked
                                },
                                leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) }
                            )
                        }
                    }
                    IconButton(onClick = ::exportPdf, enabled = state.flatConsumption.isNotEmpty() && cols.isNotEmpty()) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Download PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthFilterBar(
                periods = state.availablePeriods,
                selected = state.selectedPeriod,
                onSelect = viewModel::selectPeriod,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            )

            // Month summary strip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Consumption", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.liters(state.selectedMonthTotal), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.currency(state.selectedMonthRevenue, state.currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            when {
                state.flats.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.WaterDrop,
                    title = "No Flats Found",
                    message = "Add flats to masterFlats to see the report here."
                )

                else -> {
                    Text(
                        "${state.flatConsumption.size} flats • ${cols.size} columns shown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    ReportTable(
                        cols = cols,
                        rows = state.flatConsumption,
                        settings = state.settings,
                        currency = state.currency,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Download PDF",
                        onClick = ::exportPdf,
                        enabled = state.flatConsumption.isNotEmpty() && cols.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportTable(
    cols: List<WaterReportColumn>,
    rows: List<FlatConsumption>,
    settings: AppSettings,
    currency: String,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column(modifier.fillMaxSize().horizontalScroll(hScroll)) {
        // Sticky header (outside the vertical scroller).
        Row(Modifier.background(MaterialTheme.colorScheme.primary)) {
            cols.forEach { c -> TableCell(c.header, c.width, header = true) }
        }
        Column(Modifier.weight(1f).verticalScroll(vScroll)) {
            rows.forEachIndexed { index, fc ->
                Row(
                    Modifier.background(
                        if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    cols.forEach { c -> TableCell(c.value(fc, settings, currency), c.width) }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: Dp, header: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 8.dp, vertical = 10.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
        color = if (header) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )
}
