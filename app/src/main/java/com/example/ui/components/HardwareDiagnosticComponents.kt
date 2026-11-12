package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HfModelEntity
import com.example.domain.CompatibilityBadge
import com.example.domain.DeviceHardwareProfile
import com.example.domain.ModelCompatibilityAssessment
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.NeuralViolet
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Reusable 'Model Compatibility Badge' component that rates Hugging Face models
 * as Optimal, Playable, or Too Heavy based on live device RAM, CPU, and Storage.
 */
@Composable
fun ModelCompatibilityBadge(
    assessment: ModelCompatibilityAssessment,
    modifier: Modifier = Modifier,
    showMetricsSubtitle: Boolean = false
) {
    val (badgeColor, badgeIcon) = when (assessment.badge) {
        CompatibilityBadge.OPTIMAL -> EmeraldEnclave to Icons.Default.CheckCircle
        CompatibilityBadge.PLAYABLE -> AmberConflict to Icons.Default.WarningAmber
        CompatibilityBadge.TOO_HEAVY -> CrimsonAlert to Icons.Default.ErrorOutline
    }

    val ratingKey = assessment.badge.name.lowercase()

    Surface(
        color = badgeColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp)
            )
            .testTag("model_compatibility_badge_$ratingKey")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = "Compatibility: ${assessment.badgeLabel}",
                tint = badgeColor,
                modifier = Modifier.size(15.dp)
            )
            Column {
                Text(
                    text = assessment.badgeLabel, // "Optimal", "Playable", or "Too Heavy"
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
                if (showMetricsSubtitle) {
                    Text(
                        text = "~${assessment.estimatedRamMb} MB RAM (${assessment.ramUsagePercentOfAvailable}%) • ${assessment.estimatedTokPerSec} tok/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Diagnostic UI component that runs a multi-stage hardware scan (RAM, CPU, Storage),
 * summarizes Optimal / Playable / Too Heavy model counts, and provides an interactive
 * capacity simulator for Hugging Face models.
 */
@Composable
fun HardwareDiagnosticScannerCard(
    deviceHardware: DeviceHardwareProfile?,
    models: List<HfModelEntity>,
    recommendedModel: HfModelEntity?,
    onEvaluateHardwareFit: (Int, String) -> ModelCompatibilityAssessment,
    onRunHardwareScan: () -> Unit,
    onOpenAddCustomModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var scanStageIndex by rememberSaveable { mutableIntStateOf(3) } // 0=RAM, 1=CPU, 2=Storage, 3=Complete
    var showSimulator by rememberSaveable { mutableStateOf(false) }
    var simulatedSizeMb by rememberSaveable { mutableFloatStateOf(1630f) }
    var simulatedQuant by rememberSaveable { mutableStateOf("Q4_K_M") }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStageIndex = 0
            delay(260)
            scanStageIndex = 1
            delay(260)
            scanStageIndex = 2
            delay(260)
            onRunHardwareScan()
            scanStageIndex = 3
            isScanning = false
        }
    }

    val optimalCount = remember(models, deviceHardware) {
        models.count { onEvaluateHardwareFit(it.sizeMb, it.quantization).badge == CompatibilityBadge.OPTIMAL }
    }
    val playableCount = remember(models, deviceHardware) {
        models.count { onEvaluateHardwareFit(it.sizeMb, it.quantization).badge == CompatibilityBadge.PLAYABLE }
    }
    val tooHeavyCount = remember(models, deviceHardware) {
        models.count { onEvaluateHardwareFit(it.sizeMb, it.quantization).badge == CompatibilityBadge.TOO_HEAVY }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                ElectricCyan.copy(alpha = 0.55f),
                RoundedCornerShape(20.dp)
            )
            .testTag("hardware_diagnostic_scanner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row + Run Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Hardware Diagnostic Scanner",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hardware Diagnostic & Model Compatibility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanning) {
                                when (scanStageIndex) {
                                    0 -> "Scanning physical & available RAM headroom..."
                                    1 -> "Benchmarking CPU cores & SIMD/ABI instruction set..."
                                    else -> "Probing internal flash storage & KV-cache reserve..."
                                }
                            } else {
                                "${deviceHardware?.deviceModelName ?: "Android Device"} • ${deviceHardware?.deviceTierLabel ?: "Performance Tier"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isScanning) ElectricCyan else EmeraldEnclave
                        )
                    }
                }

                Button(
                    onClick = { isScanning = true },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.testTag("run_hardware_diagnostic_button")
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Run Hardware Scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScanning) "Scanning..." else "Run Scan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isScanning) {
                LinearProgressIndicator(
                    progress = { (scanStageIndex + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ElectricCyan
                )
            }

            // 3-Pillar Diagnostic Gauges: RAM, CPU, Storage
            if (deviceHardware != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticGaugeBar(
                        icon = Icons.Default.Memory,
                        title = "RAM Headroom (Inference + KV Cache)",
                        valueRight = "${deviceHardware.availableRamMb} MB Free / ${deviceHardware.totalRamMb} MB Total",
                        progress = (deviceHardware.availableRamMb.toFloat() / deviceHardware.totalRamMb.coerceAtLeast(1).toFloat()),
                        accentColor = EmeraldEnclave
                    )
                    DiagnosticGaugeBar(
                        icon = Icons.Default.DeveloperBoard,
                        title = "CPU Compute & Architecture",
                        valueRight = "${deviceHardware.cpuCores} Cores • ${deviceHardware.primaryAbi}",
                        progress = (deviceHardware.cpuCores.toFloat() / 8f).coerceIn(0.35f, 1f),
                        accentColor = ElectricCyan
                    )
                    DiagnosticGaugeBar(
                        icon = Icons.Default.Storage,
                        title = "On-Device Model Storage",
                        valueRight = "${deviceHardware.freeStorageMb / 1024} GB Free / ${deviceHardware.totalStorageMb / 1024} GB",
                        progress = (deviceHardware.freeStorageMb.toFloat() / deviceHardware.totalStorageMb.coerceAtLeast(1).toFloat()).coerceIn(0.15f, 1f),
                        accentColor = NeuralViolet
                    )
                }

                // Model Compatibility Rating Distribution Row (Optimal / Playable / Too Heavy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompatibilitySummaryPill(
                        title = "Optimal",
                        count = optimalCount,
                        subtitle = "≤ 65% Free RAM",
                        color = EmeraldEnclave,
                        modifier = Modifier.weight(1f)
                    )
                    CompatibilitySummaryPill(
                        title = "Playable",
                        count = playableCount,
                        subtitle = "66–95% Free RAM",
                        color = AmberConflict,
                        modifier = Modifier.weight(1f)
                    )
                    CompatibilitySummaryPill(
                        title = "Too Heavy",
                        count = tooHeavyCount,
                        subtitle = "> 95% Free RAM",
                        color = CrimsonAlert,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Best Recommended Model Banner with live ModelCompatibilityBadge
                if (recommendedModel != null) {
                    val recFit = onEvaluateHardwareFit(recommendedModel.sizeMb, recommendedModel.quantization)
                    Surface(
                        color = EmeraldEnclave.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                EmeraldEnclave.copy(alpha = 0.45f),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Recommended Model",
                                        tint = EmeraldEnclave,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Best Match: ${recommendedModel.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EmeraldEnclave,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${recommendedModel.quantization} (${recommendedModel.sizeMb} MB) • Safe Budget ≤ ${deviceHardware.recommendedMaxModelMb} MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            ModelCompatibilityBadge(assessment = recFit)
                        }
                    }
                }
            }

            // Interactive Simulator Toggle + Add Custom Model CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showSimulator = !showSimulator },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_capacity_simulator_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Capacity Simulator", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showSimulator) "Hide Simulator" else "Test Model Size",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onOpenAddCustomModel,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuralViolet,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("open_add_custom_model_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Any Hugging Face Model", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Any HF Model", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Interactive What-If Model Capacity Simulator
            AnimatedVisibility(visible = showSimulator) {
                val simAssessment = onEvaluateHardwareFit(simulatedSizeMb.roundToInt(), simulatedQuant)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("capacity_simulator_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Interactive Model Capacity Simulator",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Simulated Weight Size: ${simulatedSizeMb.roundToInt()} MB ($simulatedQuant)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                            ModelCompatibilityBadge(
                                assessment = simAssessment,
                                showMetricsSubtitle = false
                            )
                        }

                        Slider(
                            value = simulatedSizeMb,
                            onValueChange = { simulatedSizeMb = it },
                            valueRange = 350f..6000f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("simulator_size_slider")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Q4_K_M", "Q5_K_M", "Q8_0", "INT4-TFLite").forEach { q ->
                                FilterChip(
                                    selected = simulatedQuant == q,
                                    onClick = { simulatedQuant = q },
                                    label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Simulated Fit",
                                tint = ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = simAssessment.reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticGaugeBar(
    icon: ImageVector,
    title: String,
    valueRight: String,
    progress: Float,
    accentColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "gauge_progress"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = valueRight,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
private fun CompatibilitySummaryPill(
    title: String,
    count: Int,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("summary_pill_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$count $title",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
