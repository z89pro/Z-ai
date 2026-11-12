package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HfHubSearchResult
import com.example.data.HfModelEntity
import com.example.domain.CompatibilityBadge
import com.example.domain.DeviceHardwareProfile
import com.example.domain.ModelCompatibilityAssessment
import com.example.ui.components.HardwareDiagnosticScannerCard
import com.example.ui.components.ModelCompatibilityBadge
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.NeuralViolet

@Composable
fun HfModelsScreen(
    models: List<HfModelEntity>,
    activeModel: HfModelEntity?,
    deviceHardware: DeviceHardwareProfile?,
    hfSearchQuery: String,
    isSearchingHub: Boolean,
    hubSearchResults: List<HfHubSearchResult>,
    downloadingModelId: String?,
    downloadProgress: Int,
    downloadStatusNote: String,
    onEvaluateHardwareFit: (Int, String) -> ModelCompatibilityAssessment,
    onRescanHardware: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchHfHub: (String) -> Unit,
    onActivateOrDownloadModel: (HfModelEntity) -> Unit,
    onDownloadHubResult: (HfHubSearchResult) -> Unit,
    onAddCustomHfModel: (String, String, String, String, Int, String) -> Unit,
    onCancelDownload: () -> Unit = {},
    onDeleteModel: (HfModelEntity) -> Unit = {},
    onImportLocalModelUri: (Uri, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showAddCustomModelDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSectionTab by rememberSaveable { mutableIntStateOf(0) } // 0 = My Models, 1 = Search Hub, 2 = Hardware Check

    val localModelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var resolvedName = "imported_model.gguf"
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            resolvedName = cursor.getString(idx) ?: resolvedName
                        }
                    }
                }
            }
            onImportLocalModelUri(uri, resolvedName)
        }
    }

    val recommendedModel = remember(models, deviceHardware) {
        models.filter {
            onEvaluateHardwareFit(it.sizeMb, it.quantization).badge == CompatibilityBadge.OPTIMAL
        }.maxByOrNull { it.sizeMb } ?: models.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hf_models_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Top Header Card + Action Buttons (Import .gguf/.tflite & Add Repo)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Model Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active: ${activeModel?.displayName ?: "Gemma 2 2B"} (${activeModel?.quantization ?: "Q4_K_M"}) • ${models.count { it.isDownloaded }} downloaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Surface(
                            color = EmeraldEnclave.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${deviceHardware?.availableRamMb ?: 3400} MB Free RAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldEnclave,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { localModelPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_local_model_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Import local model file",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .GGUF/.TFLite", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = { showAddCustomModelDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_add_custom_model_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeuralViolet)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Hugging Face repository",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add HF Repo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Clean 3-tab selector to eliminate scrolling through cluttered sections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSectionTab == 0,
                            onClick = { selectedSectionTab = 0 },
                            label = { Text("Models (${models.size})") },
                            leadingIcon = {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = selectedSectionTab == 1,
                            onClick = { selectedSectionTab = 1 },
                            label = { Text("Search HF Hub") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = selectedSectionTab == 2,
                            onClick = { selectedSectionTab = 2 },
                            label = { Text("Hardware Fit") },
                            leadingIcon = {
                                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }

        // 2. Live Download Progress Banner with Cancel Button
        item {
            AnimatedVisibility(visible = downloadingModelId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan, RoundedCornerShape(14.dp))
                        .testTag("active_model_download_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Downloading $downloadingModelId",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = downloadStatusNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldEnclave
                            )
                            IconButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.testTag("cancel_model_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Download",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Section 1: Search Hugging Face Hub
        if (selectedSectionTab == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Search Hugging Face Hub (GGUF & TFLite)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hfSearchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Search repo (e.g. gemma-2-2b, qwen2.5, llama-3.2)...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hf_search_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { onSearchHfHub(hfSearchQuery) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("hf_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search HF Hub")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val quickQueries = listOf(
                                "gemma-2-2b gguf",
                                "qwen2.5-coder gguf",
                                "deepseek-r1-distill gguf",
                                "llama-3.2-1b gguf",
                                "tflite llm"
                            )
                            items(quickQueries) { q ->
                                SuggestionChip(
                                    onClick = { onSearchHfHub(q) },
                                    label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        if (isSearchingHub) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ElectricCyan)
                        }
                    }
                }
            }

            if (hubSearchResults.isNotEmpty()) {
                items(hubSearchResults, key = { "hub_${it.modelId}" }) { hubItem ->
                    val fit = onEvaluateHardwareFit(hubItem.estimatedSizeMb, hubItem.quantization)
                    HfHubResultCard(
                        item = hubItem,
                        compatibility = fit,
                        onDownload = {
                            onDownloadHubResult(hubItem)
                            selectedSectionTab = 0
                        }
                    )
                }
            }
        }

        // Section 2: Hardware Diagnostic Scanner
        if (selectedSectionTab == 2) {
            item {
                HardwareDiagnosticScannerCard(
                    deviceHardware = deviceHardware,
                    models = models,
                    recommendedModel = recommendedModel,
                    onEvaluateHardwareFit = onEvaluateHardwareFit,
                    onRunHardwareScan = onRescanHardware,
                    onOpenAddCustomModel = { showAddCustomModelDialog = true }
                )
            }
        }

        // Section 0 (Default): Model Catalog & Downloaded Models
        if (selectedSectionTab == 0) {
            items(models, key = { "model_${it.modelId}" }) { model ->
                val compatibility = onEvaluateHardwareFit(model.sizeMb, model.quantization)
                HfModelCatalogCard(
                    model = model,
                    compatibility = compatibility,
                    isDownloadingThis = downloadingModelId == model.modelId,
                    onSelectOrDownload = { onActivateOrDownloadModel(model) },
                    onDeleteDownloaded = { onDeleteModel(model) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddCustomModelDialog) {
        AddCustomHfModelDialog(
            onDismiss = { showAddCustomModelDialog = false },
            onConfirm = { repoId, name, quant, format, sizeMb, specialty ->
                onAddCustomHfModel(repoId, name, quant, format, sizeMb, specialty)
                showAddCustomModelDialog = false
                selectedSectionTab = 0
            }
        )
    }
}

@Composable
private fun HfModelCatalogCard(
    model: HfModelEntity,
    compatibility: ModelCompatibilityAssessment,
    isDownloadingThis: Boolean,
    onSelectOrDownload: () -> Unit,
    onDeleteDownloaded: () -> Unit
) {
    val badgeColor = when (compatibility.badge) {
        CompatibilityBadge.OPTIMAL -> EmeraldEnclave
        CompatibilityBadge.PLAYABLE -> AmberConflict
        CompatibilityBadge.TOO_HEAVY -> CrimsonAlert
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (model.isActive) 2.dp else 1.dp,
                color = if (model.isActive) ElectricCyan else badgeColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("hf_model_card_${model.modelId.replace("/", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModelCompatibilityBadge(
                    assessment = compatibility,
                    showMetricsSubtitle = false
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = NeuralViolet.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${model.format} • ${model.quantization}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${model.sizeMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${model.modelId} • ${model.parameterCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = model.specialty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (model.isDownloaded) EmeraldEnclave else badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (model.isDownloaded) {
                            "Saved: ${model.fileName}"
                        } else {
                            "Ready to download (~${compatibility.estimatedTokPerSec} tok/s)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.isDownloaded) EmeraldEnclave else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (model.isDownloaded) {
                        IconButton(
                            onClick = onDeleteDownloaded,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("delete_model_button_${model.modelId.replace("/", "_")}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete downloaded model",
                                tint = CrimsonAlert
                            )
                        }
                    }

                    Button(
                        onClick = onSelectOrDownload,
                        enabled = !isDownloadingThis,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                model.isActive -> EmeraldEnclave
                                model.isDownloaded -> ElectricCyan
                                else -> NeuralViolet
                            },
                            contentColor = if (model.isActive || model.isDownloaded) Color(0xFF042F2E) else Color.White
                        ),
                        modifier = Modifier.testTag("model_action_button_${model.modelId.replace("/", "_")}")
                    ) {
                        Icon(
                            imageVector = if (model.isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isDownloadingThis -> "Downloading..."
                                model.isActive -> "Active"
                                model.isDownloaded -> "Use Model"
                                else -> "Download"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HfHubResultCard(
    item: HfHubSearchResult,
    compatibility: ModelCompatibilityAssessment,
    onDownload: () -> Unit
) {
    val badgeColor = when (compatibility.badge) {
        CompatibilityBadge.OPTIMAL -> EmeraldEnclave
        CompatibilityBadge.PLAYABLE -> AmberConflict
        CompatibilityBadge.TOO_HEAVY -> CrimsonAlert
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan
                    )
                    ModelCompatibilityBadge(assessment = compatibility)
                }
                Text(
                    text = "${item.modelId} • ${item.format} (${item.quantization}) • ~${item.estimatedSizeMb}MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = compatibility.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onDownload) {
                Icon(Icons.Default.CloudDownload, contentDescription = "Download from HF Hub", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun AddCustomHfModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Int, String) -> Unit
) {
    var repoId by remember { mutableStateOf("bartowski/Qwen2.5-3B-Instruct-GGUF") }
    var displayName by remember { mutableStateOf("Qwen 2.5 3B Instruct") }
    var quantization by remember { mutableStateOf("Q4_K_M") }
    var format by remember { mutableStateOf("GGUF") }
    var sizeMbText by remember { mutableStateOf("1930") }
    var specialty by remember { mutableStateOf("Custom GGUF/TFLite Assistant") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hugging Face Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = repoId,
                    onValueChange = { repoId = it },
                    label = { Text("Hugging Face Repo ID (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_hf_repo_input")
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Q4_K_M", "Q5_K_M", "Q8_0", "INT4-TFLite").forEach { q ->
                        FilterChip(
                            selected = quantization == q,
                            onClick = {
                                quantization = q
                                format = if (q.contains("TFLite", true)) "TFLITE" else "GGUF"
                            },
                            label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sizeMbText,
                        onValueChange = { sizeMbText = it.filter { c -> c.isDigit() } },
                        label = { Text("Size (MB)") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("Role / Specialty") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        repoId,
                        displayName,
                        quantization,
                        format,
                        sizeMbText.toIntOrNull() ?: 1500,
                        specialty
                    )
                },
                modifier = Modifier.testTag("confirm_add_custom_model_button")
            ) {
                Text("Download & Load")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
