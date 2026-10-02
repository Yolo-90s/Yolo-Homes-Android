package com.example.yolo_homes.feature.water

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yolo_homes.core.Formatters
import com.example.yolo_homes.ui.components.LabeledDropdown
import com.example.yolo_homes.ui.components.PrimaryButton
import com.example.yolo_homes.ui.components.SurfaceCard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddReadingScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BulkReadingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun recognize(uri: Uri) {
        viewModel.onProcessingStarted()
        scope.launch {
            runCatching {
                // InputImage decodes the full-resolution photo off the main thread; the actual
                // recognition call uses the real suspend await() (kotlinx-coroutines-play-services)
                // rather than a blocking Tasks.await(), so neither step freezes the UI.
                withContext(Dispatchers.IO) {
                    val image = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image).await().text
                }
            }.onSuccess { text ->
                viewModel.onTextRecognized(text)
            }.onFailure {
                viewModel.onProcessingFailed(it.message ?: "Couldn't read that photo. Try a clearer one.")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { recognize(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { recognize(it) } }

    LaunchedEffect(submitState) {
        if (submitState is BulkSubmitState.Success) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Upload Readings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = stage) {
            is BulkStage.Pick -> PickStage(
                modifier = Modifier.padding(padding),
                targetPeriod = state.targetPeriod,
                error = error,
                onTakePhoto = {
                    val file = File(context.cacheDir, "bulk_reading_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onChooseFromGallery = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )

            is BulkStage.Processing -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "Reading your photo…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            is BulkStage.Review -> ReviewStage(
                modifier = Modifier.padding(padding),
                targetPeriod = state.targetPeriod,
                flats = state.flats,
                reportedFlatIds = state.reportedFlatIds,
                currency = state.currency(),
                computedRows = viewModel.computeRows(),
                error = error,
                submitting = submitState is BulkSubmitState.Submitting,
                onCurrentReadingChanged = { idx, text -> viewModel.updateCurrentReadingText(idx, text) },
                onFlatChanged = { idx, flatId -> viewModel.onFlatChanged(idx, flatId) },
                onRemoveRow = { idx -> viewModel.removeRow(idx) },
                onSubmit = { viewModel.submitAll(onDone = {}) }
            )
        }
    }
}

private fun BulkReadingUiState.currency(): String = settings.currency

@Composable
private fun PickStage(
    modifier: Modifier,
    targetPeriod: String,
    error: String?,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Upload one photo of your handwritten reading list for ${Formatters.monthLabel(targetPeriod)}. " +
                "The app will read it and show you an editable table — nothing is saved until you submit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
            Text("  Take Photo")
        }
        OutlinedButton(onClick = onChooseFromGallery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
            Text("  Choose from Gallery")
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val STATUS_LABEL = mapOf(
    RowStatus.OK to "Ready",
    RowStatus.NO_FLAT to "Pick a flat",
    RowStatus.DUPLICATE to "Duplicate in this batch",
    RowStatus.NO_READING to "No reading detected",
    RowStatus.BELOW_PREVIOUS to "Reading lower than previous"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewStage(
    modifier: Modifier,
    targetPeriod: String,
    flats: List<com.example.yolo_homes.data.model.Flat>,
    reportedFlatIds: Set<String>,
    currency: String,
    computedRows: List<ComputedRow>,
    error: String?,
    submitting: Boolean,
    onCurrentReadingChanged: (Int, String) -> Unit,
    onFlatChanged: (Int, String) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    val validCount = computedRows.count { it.status == RowStatus.OK }

    Column(modifier.fillMaxSize()) {
        Text(
            "Recording for ${Formatters.monthLabel(targetPeriod)}. $validCount of ${computedRows.size} rows will be submitted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(computedRows, key = { it.index }) { r ->
                val availableFlats = flats.filter { !reportedFlatIds.contains(it.id) || it.id == r.row.flatId }
                SurfaceCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "OCR read: \"${r.row.rawLine}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveRow(r.index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove row")
                            }
                        }
                        LabeledDropdown(
                            label = "Flat",
                            options = availableFlats,
                            selected = availableFlats.firstOrNull { it.id == r.row.flatId },
                            optionLabel = { "${it.displayName} — ${it.occupantName}" },
                            onSelect = { onFlatChanged(r.index, it.id) }
                        )
                        OutlinedTextField(
                            value = r.row.currentReadingText,
                            onValueChange = { onCurrentReadingChanged(r.index, it.filter { c -> c.isDigit() || c == '.' }) },
                            label = { Text("Current Reading") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Previous ${Formatters.liters(r.row.previousReading)}" +
                                    (r.bill?.let { " · Usage ${Formatters.liters(it.usage)} · ${Formatters.currency(it.amount, currency)}" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                STATUS_LABEL[r.status] ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (r.status == RowStatus.OK) {
                                    com.example.yolo_homes.ui.theme.Success
                                } else {
                                    com.example.yolo_homes.ui.theme.Warning
                                }
                            )
                        }
                    }
                }
            }
        }
        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        PrimaryButton(
            text = "Submit $validCount Reading${if (validCount == 1) "" else "s"}",
            loading = submitting,
            enabled = validCount > 0 && !submitting,
            onClick = onSubmit,
            modifier = Modifier.padding(16.dp)
        )
    }
}
