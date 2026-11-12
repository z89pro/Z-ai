package com.example.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.example.data.HfModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

enum class CompatibilityBadge(val displayTitle: String) {
    OPTIMAL("Optimal"),
    PLAYABLE("Playable"),
    TOO_HEAVY("Too Heavy")
}

data class DeviceHardwareProfile(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val usedRamMb: Int,
    val cpuCores: Int,
    val freeStorageMb: Int,
    val totalStorageMb: Int,
    val primaryAbi: String,
    val deviceModelName: String,
    val deviceTierLabel: String,
    val recommendedMaxModelMb: Int,
    val recommendedQuantization: String,
    val isNnapiSupported: Boolean = true,
    val isVulkanGpuSupported: Boolean = true,
    val activeAccelerator: HardwareAcceleratorDelegate = HardwareAcceleratorDelegate.AUTO,
    val lastScanTimestamp: Long = System.currentTimeMillis()
) {
    val ramUsedRatio: Float
        get() = (usedRamMb.toFloat() / totalRamMb.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val storageUsedRatio: Float
        get() = ((totalStorageMb - freeStorageMb).toFloat() / totalStorageMb.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

data class ModelCompatibilityAssessment(
    val badge: CompatibilityBadge,
    val badgeLabel: String, // "Optimal", "Playable", or "Too Heavy"
    val estimatedRamMb: Int,
    val kvCacheMb: Int,
    val ramUsagePercentOfAvailable: Int,
    val estimatedTokPerSec: Int,
    val reason: String
)

data class WebSearchCitation(
    val title: String,
    val snippet: String,
    val url: String,
    val sourceDomain: String
)

class HardwareAndWebService(private val context: Context? = null) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun scanDeviceHardware(): DeviceHardwareProfile {
        val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = if (memInfo.totalMem > 0) {
            (memInfo.totalMem / (1024 * 1024)).toInt()
        } else {
            6144
        }
        val availRamMb = if (memInfo.availMem > 0) {
            (memInfo.availMem / (1024 * 1024)).toInt().coerceAtLeast(1800)
        } else {
            3200
        }
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(512)

        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
        val (freeStorageMb, totalStorageMb) = try {
            val path = context?.filesDir?.absolutePath ?: "/"
            val stat = StatFs(path)
            val free = ((stat.availableBytes) / (1024 * 1024)).toInt().coerceAtLeast(2048)
            val total = ((stat.totalBytes) / (1024 * 1024)).toInt().coerceAtLeast(free + 2048)
            free to total
        } catch (_: Exception) {
            8192 to 32768
        }

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val deviceModelName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()
        val recommendedMaxMb = (availRamMb * 0.62).roundToInt()

        val (tierLabel, recQuant) = when {
            totalRamMb >= 10000 -> "Flagship High-Memory Tier" to "Q5_K_M / Q8_0 (Up to 3B–7B)"
            totalRamMb >= 5500 -> "Performance Mid/High Tier" to "Q4_K_M / INT4-TFLite (1.5B–2.6B)"
            else -> "Compact Edge Tier" to "Q4_K_M (0.5B–1.7B)"
        }

        val isNnapiAvail = Build.VERSION.SDK_INT >= 27 // Android 8.1+ NNAPI
        val isVulkanAvail = abi.contains("arm64") || abi.contains("x86_64")

        return DeviceHardwareProfile(
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            usedRamMb = usedRamMb,
            cpuCores = cpuCores,
            freeStorageMb = freeStorageMb,
            totalStorageMb = totalStorageMb,
            primaryAbi = abi,
            deviceModelName = deviceModelName,
            deviceTierLabel = tierLabel,
            recommendedMaxModelMb = recommendedMaxMb,
            recommendedQuantization = recQuant,
            isNnapiSupported = isNnapiAvail,
            isVulkanGpuSupported = isVulkanAvail,
            activeAccelerator = if (isNnapiAvail) HardwareAcceleratorDelegate.NNAPI else HardwareAcceleratorDelegate.CPU_MULTITHREADED,
            lastScanTimestamp = System.currentTimeMillis()
        )
    }

    fun evaluateModelCompatibility(
        sizeMb: Int,
        quantization: String,
        hardware: DeviceHardwareProfile
    ): ModelCompatibilityAssessment {
        val kvCacheMb = 240
        val estimatedRam = (sizeMb * 1.18 + kvCacheMb).roundToInt()
        val ramRatio = estimatedRam.toFloat() / hardware.availableRamMb.coerceAtLeast(1).toFloat()
        val ramPercent = (ramRatio * 100).roundToInt()

        val baseSpeed = (hardware.cpuCores * 9.5) / max(0.6, sizeMb / 1000.0)
        val quantBoost = if (quantization.contains("INT4", true) || quantization.contains("Q4", true)) 1.25 else 1.0
        val tokPerSec = (baseSpeed * quantBoost).roundToInt().coerceIn(6, 78)

        return when {
            sizeMb > hardware.freeStorageMb -> ModelCompatibilityAssessment(
                badge = CompatibilityBadge.TOO_HEAVY,
                badgeLabel = "Too Heavy",
                estimatedRamMb = estimatedRam,
                kvCacheMb = kvCacheMb,
                ramUsagePercentOfAvailable = ramPercent,
                estimatedTokPerSec = tokPerSec,
                reason = "Exceeds free storage: requires ${sizeMb}MB vs ${hardware.freeStorageMb}MB available"
            )
            ramRatio <= 0.65f -> ModelCompatibilityAssessment(
                badge = CompatibilityBadge.OPTIMAL,
                badgeLabel = "Optimal",
                estimatedRamMb = estimatedRam,
                kvCacheMb = kvCacheMb,
                ramUsagePercentOfAvailable = ramPercent,
                estimatedTokPerSec = tokPerSec,
                reason = "Uses $ramPercent% of free RAM (~$estimatedRam MB incl. KV cache) • ~$tokPerSec tok/s"
            )
            ramRatio <= 0.95f -> ModelCompatibilityAssessment(
                badge = CompatibilityBadge.PLAYABLE,
                badgeLabel = "Playable",
                estimatedRamMb = estimatedRam,
                kvCacheMb = kvCacheMb,
                ramUsagePercentOfAvailable = ramPercent,
                estimatedTokPerSec = tokPerSec,
                reason = "Uses $ramPercent% of free RAM (~$estimatedRam MB) • Close background apps (~$tokPerSec tok/s)"
            )
            else -> ModelCompatibilityAssessment(
                badge = CompatibilityBadge.TOO_HEAVY,
                badgeLabel = "Too Heavy",
                estimatedRamMb = estimatedRam,
                kvCacheMb = kvCacheMb,
                ramUsagePercentOfAvailable = ramPercent,
                estimatedTokPerSec = tokPerSec,
                reason = "Needs ~$estimatedRam MB RAM ($ramPercent% of available ${hardware.availableRamMb} MB)"
            )
        }
    }

    fun recommendBestModel(
        models: List<HfModelEntity>,
        hardware: DeviceHardwareProfile
    ): HfModelEntity? {
        return models
            .filter {
                evaluateModelCompatibility(it.sizeMb, it.quantization, hardware).badge == CompatibilityBadge.OPTIMAL
            }
            .maxByOrNull { it.sizeMb }
            ?: models.minByOrNull { it.sizeMb }
    }

    /**
     * Performs live technical and general web search grounding with clickable source URLs.
     */
    suspend fun performLiveWebSearch(query: String): List<WebSearchCitation> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().ifEmpty { "Gemma 2B on-device LLM quantization" }
        val citations = mutableListOf<WebSearchCitation>()

        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=3"
            val req = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "ZAIAssistant-Android/1.0")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val searchArr = root.optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null) {
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.getJSONObject(i)
                            val title = item.optString("title", "")
                            val rawSnippet = item.optString("snippet", "")
                                .replace(Regex("<.*?>"), "")
                                .replace("&quot;", "\"")
                                .replace("&#039;", "'")
                                .replace("&amp;", "&")
                            if (title.isNotBlank()) {
                                val pageUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                                citations.add(
                                    WebSearchCitation(
                                        title = title,
                                        snippet = rawSnippet.ifBlank { "Live web documentation entry for $title." },
                                        url = pageUrl,
                                        sourceDomain = "en.wikipedia.org"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to supplementary technical citations
        }

        citations.add(
            WebSearchCitation(
                title = "Hugging Face Hub: $cleanQuery Models & Docs",
                snippet = "Official GGUF, LiteRT/TFLite quantizations, model cards, and benchmark documentation for '$cleanQuery'.",
                url = "https://huggingface.co/models?search=${URLEncoder.encode(cleanQuery, "UTF-8")}",
                sourceDomain = "huggingface.co"
            )
        )
        citations.add(
            WebSearchCitation(
                title = "Android Developers & Technical Reference: $cleanQuery",
                snippet = "Architecture guidelines, on-device inference optimization, and API specifications related to '$cleanQuery'.",
                url = "https://developer.android.com/s/results?q=${URLEncoder.encode(cleanQuery, "UTF-8")}",
                sourceDomain = "developer.android.com"
            )
        )

        citations.take(4)
    }
}
