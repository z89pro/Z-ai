package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HfModelEntity
import com.example.domain.AccelerationBenchmarkResult
import com.example.domain.DeviceHardwareProfile
import com.example.domain.HardwareAcceleratorDelegate
import com.example.domain.InferenceAccelerationConfig
import com.example.domain.KVCacheStats
import com.example.domain.ModelCapabilities
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalZaiTokens

@Composable
fun ZaiSettingsScreen(
    themeMode: AppThemeMode,
    activeModel: HfModelEntity?,
    modelCapabilities: ModelCapabilities,
    deviceHardware: DeviceHardwareProfile = DeviceHardwareProfile(
        totalRamMb = 6144,
        availableRamMb = 3400,
        usedRamMb = 2744,
        cpuCores = 8,
        freeStorageMb = 16384,
        totalStorageMb = 32768,
        primaryAbi = "arm64-v8a",
        deviceModelName = "Android Device",
        deviceTierLabel = "Performance Mobile Tier",
        recommendedMaxModelMb = 2100,
        recommendedQuantization = "Q4_K_M / INT4-TFLite (1.5B–2.6B)"
    ),
    accelerationConfig: InferenceAccelerationConfig = InferenceAccelerationConfig(),
    kvCacheStats: KVCacheStats = KVCacheStats(),
    lastBenchmarkResult: AccelerationBenchmarkResult? = null,
    isBenchmarkingAccelerator: Boolean = false,
    isStrictOfflineEnclave: Boolean,
    requireApprovalForHighRisk: Boolean,
    isWakeWordServiceEnabled: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleHardwareAcceleration: (Boolean) -> Unit = {},
    onSelectAcceleratorDelegate: (HardwareAcceleratorDelegate) -> Unit = {},
    onToggleFp16Relaxation: (Boolean) -> Unit = {},
    onToggleKvCacheReuse: (Boolean) -> Unit = {},
    onClearKvCache: () -> Unit = {},
    onRunAcceleratorBenchmark: () -> Unit = {},
    onToggleStrictOfflineEnclave: () -> Unit,
    onToggleHighRiskFirewall: (Boolean) -> Unit,
    onToggleWakeWordService: (Boolean) -> Unit,
    onOpenHfModelAdvisor: () -> Unit,
    onOpenHfModelHub: () -> Unit,
    onOpenVoiceAssistant: () -> Unit
) {
    val tokens = LocalZaiTokens.current
    val scrollState = rememberScrollState()
    val isDeviceGpuCompatible = deviceHardware.isVulkanGpuSupported || deviceHardware.isNnapiSupported

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.mainBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("zai_settings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Z AI Workspace Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.primaryText
            )

            // 1. APPEARANCE & THEME MODE (Dark Mode, Light Mode, System Default)
            Surface(
                color = tokens.featureCardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.border, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "APPEARANCE & THEME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.primaryCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Switch between Dark Mode (#050B18), Light Mode, and System Default while preserving the exact Z AI layout and proportions.",
                        fontSize = 13.sp,
                        color = tokens.secondaryText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = themeMode == mode
                            val icon = when (mode) {
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            }
                            Surface(
                                color = if (selected) tokens.welcomeCardTop else tokens.inputSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) tokens.primaryCyan else tokens.border,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectThemeMode(mode) }
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = mode.label,
                                        tint = if (selected) tokens.primaryCyan else tokens.secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = mode.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) tokens.primaryText else tokens.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. ACTIVE HUGGING FACE MODEL CAPABILITIES & TFLITE / GGUF GPU DELEGATE CONTROLS
            Surface(
                color = tokens.featureCardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.border, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE AI BRAIN & GPU DELEGATE LOADER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.primaryCyan,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Model Online",
                            tint = tokens.successGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = activeModel?.displayName ?: modelCapabilities.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.primaryText
                    )
                    Text(
                        text = "Architecture: ${modelCapabilities.architecture} • Format: ${modelCapabilities.format.badge} • Quantization: ${modelCapabilities.quantization}",
                        fontSize = 13.sp,
                        color = tokens.secondaryText
                    )
                    Text(
                        text = "Context Window: ${modelCapabilities.contextWindowTokens} tokens • Tool Mode: ${modelCapabilities.toolExecutionModeLabel}",
                        fontSize = 13.sp,
                        color = tokens.secondaryText
                    )
                    Text(
                        text = "Hardware Accelerator: ${modelCapabilities.accelerationModeLabel} • Est. Speed: ${modelCapabilities.tokensPerSecondEstimate} tok/s (${modelCapabilities.timeToFirstTokenMs}ms TTFT)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.successGreen
                    )
                    Text(
                        text = modelCapabilities.loaderDelegateConfigSummary,
                        fontSize = 12.sp,
                        color = tokens.primaryCyan
                    )

                    // Device Compatibility & GPU Hardware Acceleration Toggle
                    Surface(
                        color = tokens.inputSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.border, RoundedCornerShape(14.dp))
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable GPU / NNAPI Hardware Acceleration",
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.primaryText,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isDeviceGpuCompatible) {
                                            "Compatible (${deviceHardware.deviceModelName} • ${deviceHardware.primaryAbi}): Offloads TFLite (GpuDelegateV2) & GGUF (${modelCapabilities.gpuLayersOffloaded} Vulkan layers) to GPU/NPU."
                                        } else {
                                            "Device GPU/NNAPI incompatible (${deviceHardware.primaryAbi}) — Locked to Multi-Core CPU fallback."
                                        },
                                        color = if (isDeviceGpuCompatible) tokens.secondaryText else tokens.primaryCyan,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = accelerationConfig.isHardwareAccelerationEnabled && isDeviceGpuCompatible,
                                    onCheckedChange = { onToggleHardwareAcceleration(it) },
                                    enabled = isDeviceGpuCompatible,
                                    modifier = Modifier.testTag("hardware_acceleration_toggle")
                                )
                            }

                            if (accelerationConfig.isHardwareAccelerationEnabled && isDeviceGpuCompatible) {
                                Text(
                                    text = "SELECT DELEGATE BACKEND",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.primaryCyan,
                                    letterSpacing = 0.8.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val delegates = listOf(
                                        HardwareAcceleratorDelegate.GPU_VULKAN to "GPU Vulkan",
                                        HardwareAcceleratorDelegate.NNAPI to "NNAPI NPU",
                                        HardwareAcceleratorDelegate.AUTO to "Auto"
                                    )
                                    delegates.forEach { (delegate, shortTitle) ->
                                        val isSelected = accelerationConfig.selectedDelegate == delegate
                                        Surface(
                                            color = if (isSelected) tokens.welcomeCardTop else tokens.featureCardSurface,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) tokens.primaryCyan else tokens.border,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onSelectAcceleratorDelegate(delegate) }
                                                .testTag("delegate_option_${delegate.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = shortTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) tokens.primaryCyan else tokens.primaryText
                                                )
                                                Text(
                                                    text = "${delegate.tokensPerSecondTypical} tok/s",
                                                    fontSize = 10.sp,
                                                    color = tokens.secondaryText
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "FP16 Tensor Precision Relaxation",
                                            fontWeight = FontWeight.Medium,
                                            color = tokens.primaryText,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Allows half-precision FP16 execution in TfLiteGpuDelegateV2 & GGUF shaders for ~1.6x higher throughput.",
                                            color = tokens.secondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = accelerationConfig.allowFp16Relaxation,
                                        onCheckedChange = { onToggleFp16Relaxation(it) },
                                        modifier = Modifier.testTag("fp16_relaxation_toggle")
                                    )
                                }
                            }

                            // KV-Cache Multi-Turn Prompt Reuse Toggle & Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "KV-Cache Multi-Turn Prompt Reuse",
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.primaryText,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (kvCacheStats.isEnabled) {
                                            "Reuses system prompt & prior turn attention tensors • ${kvCacheStats.hitCount} hits / ${kvCacheStats.missCount} misses • ${kvCacheStats.totalTokensSaved} tokens saved"
                                        } else {
                                            "Disabled — Recomputes full prompt prefill on every conversation turn."
                                        },
                                        color = if (kvCacheStats.isEnabled) tokens.primaryCyan else tokens.secondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = accelerationConfig.enableKVCacheReuse,
                                    onCheckedChange = { onToggleKvCacheReuse(it) },
                                    modifier = Modifier.testTag("kv_cache_reuse_toggle")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRunAcceleratorBenchmark,
                                    enabled = !isBenchmarkingAccelerator,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("run_accelerator_benchmark_button")
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = tokens.primaryCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBenchmarkingAccelerator) {
                                            "Benchmarking..."
                                        } else {
                                            "Benchmark ${modelCapabilities.activeDelegate.badge}"
                                        },
                                        color = tokens.primaryText,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = onClearKvCache,
                                    modifier = Modifier.testTag("clear_kv_cache_button")
                                ) {
                                    Text("Clear KV-Cache", color = tokens.primaryText, fontSize = 12.sp)
                                }
                            }

                            if (lastBenchmarkResult != null) {
                                Text(
                                    text = "Last Benchmark (${lastBenchmarkResult.delegate.badge}): ⚡ ${lastBenchmarkResult.tokensPerSecond} tok/s • Prefill: ${lastBenchmarkResult.prefillLatencyMs}ms • Decode: ${lastBenchmarkResult.decodeLatencyMs}ms/tok • ${lastBenchmarkResult.speedGainVsCpu}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tokens.successGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenHfModelHub,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.primaryCyan,
                                contentColor = tokens.mainBackground
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HF Model Hub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenHfModelAdvisor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = tokens.primaryCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Model Advisor", color = tokens.primaryText, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 3. PRIVACY ENCLAVE, ZERO-TRUST GATE & WAKE WORD CONTROLS
            Surface(
                color = tokens.featureCardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.border, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = tokens.primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY, ENCLAVE & VOICE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.primaryCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strict Offline Enclave (0 KB Egress)",
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.primaryText,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Processes sensitive project data 100% locally on-device with Gemma 2B.",
                                color = tokens.secondaryText,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isStrictOfflineEnclave,
                            onCheckedChange = { onToggleStrictOfflineEnclave() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero-Trust High-Risk Approval Gate",
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.primaryText,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Require user confirmation before destructive file deletes, shell commands, or calls.",
                                color = tokens.secondaryText,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = requireApprovalForHighRisk,
                            onCheckedChange = onToggleHighRiskFirewall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word Listener (Jarvis, Janu, Friday, Edith, Ultron)",
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.primaryText,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Continuous multi-step voice command listener with 60s silence auto-sleep.",
                                color = tokens.secondaryText,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isWakeWordServiceEnabled,
                            onCheckedChange = onToggleWakeWordService
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenVoiceAssistant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = tokens.primaryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Hands-Free Voice Assistant & Macros", color = tokens.primaryText)
                    }
                }
            }
        }
    }
}
