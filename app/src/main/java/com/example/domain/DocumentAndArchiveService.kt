package com.example.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class DocumentAnalysisReport(
    val id: Long = System.currentTimeMillis(),
    val fileName: String,
    val fileTypeLabel: String,
    val charCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val estimatedTokens: Int,
    val executiveSummary: String,
    val keyHeadingsAndTopics: List<String>,
    val extractedActionItems: List<String>,
    val structuralMetrics: List<String>,
    val exportMarkdownContent: String,
    val analyzedByModel: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val actionItems: List<String> get() = extractedActionItems
    val markdownReport: String get() = exportMarkdownContent
}

/**
 * Represents an entry in the Code Studio 'Recent Files' history bar (max 5 most recently opened files).
 */
data class RecentCodeFileEntry(
    val projectId: String,
    val projectName: String,
    val filePath: String,
    val language: String,
    val snippetId: Int? = null,
    val openedAtMillis: Long = System.currentTimeMillis()
) {
    val uniqueKey: String
        get() = if (snippetId != null) {
            "snippet:$snippetId"
        } else {
            "proj:${projectId.lowercase()}:${filePath.trim().lowercase()}"
        }
}

class DocumentAndArchiveService {

    companion object {
        const val MAX_RECENT_FILES = 5
        private const val ENTRY_DELIMITER = "\n---RECENT_ENTRY---\n"
        private const val FIELD_DELIMITER = "|||"
    }

    /**
     * Records a newly opened file into the Recent Files list, keeping the most recent first,
     * deduplicating existing entries for the same file, and capping the history at 5 items.
     */
    fun recordRecentFileOpened(
        currentHistory: List<RecentCodeFileEntry>,
        openedEntry: RecentCodeFileEntry
    ): List<RecentCodeFileEntry> {
        val filtered = currentHistory.filterNot { it.uniqueKey == openedEntry.uniqueKey }
        return (listOf(openedEntry) + filtered).take(MAX_RECENT_FILES)
    }

    /**
     * Removes a deleted file from the Recent Files history.
     */
    fun removeRecentFile(
        currentHistory: List<RecentCodeFileEntry>,
        projectId: String,
        filePath: String
    ): List<RecentCodeFileEntry> {
        val targetKey = "proj:${projectId.lowercase()}:${filePath.trim().lowercase()}"
        return currentHistory.filterNot { it.uniqueKey == targetKey }
    }

    /**
     * Serializes the recent files list for local persistence in SharedPreferences.
     */
    fun serializeRecentFiles(entries: List<RecentCodeFileEntry>): String {
        return entries.take(MAX_RECENT_FILES).joinToString(ENTRY_DELIMITER) { entry ->
            listOf(
                entry.projectId.replace(FIELD_DELIMITER, "_"),
                entry.projectName.replace(FIELD_DELIMITER, "_"),
                entry.filePath.replace(FIELD_DELIMITER, "_"),
                entry.language.replace(FIELD_DELIMITER, "_"),
                entry.snippetId?.toString() ?: "",
                entry.openedAtMillis.toString()
            ).joinToString(FIELD_DELIMITER)
        }
    }

    /**
     * Deserializes the recent files list from local storage, capped at [MAX_RECENT_FILES] (5).
     */
    fun deserializeRecentFiles(raw: String?): List<RecentCodeFileEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(ENTRY_DELIMITER)
            .mapNotNull { line ->
                val parts = line.split(FIELD_DELIMITER)
                if (parts.size < 4) {
                    null
                } else {
                    val projId = parts[0].trim()
                    val projName = parts[1].trim()
                    val path = parts[2].trim()
                    val lang = parts[3].trim().ifEmpty { "HTML" }
                    val snipId = parts.getOrNull(4)?.trim()?.toIntOrNull()
                    val openedAt = parts.getOrNull(5)?.trim()?.toLongOrNull() ?: 0L
                    if (path.isEmpty()) null else RecentCodeFileEntry(
                        projectId = projId,
                        projectName = projName,
                        filePath = path,
                        language = lang,
                        snippetId = snipId,
                        openedAtMillis = openedAt
                    )
                }
            }
            .distinctBy { it.uniqueKey }
            .take(MAX_RECENT_FILES)
    }

    /**
     * Performs real structural, lexical, and action-item analysis on any text/code/data document.
     */
    fun analyzeDocumentContent(
        fileName: String,
        rawContent: String,
        activeModelName: String = "Gemma 2 2B Instruct"
    ): DocumentAnalysisReport {
        val cleanName = fileName.trim().ifBlank { "document.txt" }
        val normalized = rawContent.replace("\r\n", "\n")
        val lines = if (normalized.isEmpty()) emptyList() else normalized.lines()
        val nonBlankLines = lines.map { it.trim() }.filter { it.isNotEmpty() }
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        val charCount = normalized.length
        val wordCount = words.size
        val lineCount = lines.size
        val estimatedTokens = (charCount / 4).coerceAtLeast(if (wordCount > 0) 1 else 0)

        val lowerName = cleanName.lowercase()
        val fileTypeLabel = when {
            lowerName.endsWith(".kt") || lowerName.endsWith(".java") -> "Kotlin / JVM Source"
            lowerName.endsWith(".py") -> "Python Script"
            lowerName.endsWith(".js") || lowerName.endsWith(".ts") || lowerName.endsWith(".tsx") -> "JavaScript / TypeScript"
            lowerName.endsWith(".html") || lowerName.endsWith(".htm") -> "HTML Document"
            lowerName.endsWith(".css") -> "CSS Stylesheet"
            lowerName.endsWith(".json") -> "JSON Dataset"
            lowerName.endsWith(".csv") -> "CSV Tabular Data"
            lowerName.endsWith(".md") -> "Markdown Document"
            lowerName.endsWith(".xml") -> "XML Configuration"
            lowerName.endsWith(".sql") -> "SQL Schema / Query"
            else -> "Text Document"
        }

        // Extract headings, function declarations, or top key lines
        val headings = mutableListOf<String>()
        for (line in nonBlankLines) {
            when {
                line.startsWith("#") -> headings.add(line.trimStart('#').trim())
                line.startsWith("fun ") || line.startsWith("suspend fun ") || line.startsWith("private fun ") ->
                    headings.add("Function: ${line.substringBefore("{").trim().take(70)}")
                line.startsWith("class ") || line.startsWith("data class ") || line.startsWith("interface ") ->
                    headings.add("Type: ${line.substringBefore("{").trim().take(70)}")
                line.startsWith("def ") || line.startsWith("async def ") ->
                    headings.add("Python Def: ${line.substringBefore(":").trim().take(70)}")
                line.startsWith("function ") || line.contains("=>") ->
                    if (headings.size < 8 && line.length < 80) headings.add("JS Handler: ${line.substringBefore("{").trim()}")
            }
            if (headings.size >= 8) break
        }
        if (headings.isEmpty()) {
            headings.addAll(nonBlankLines.take(4).map { it.take(80) })
        }

        // Extract actionable items (TODO, FIXME, checkboxes, imperative lines, deadlines)
        val actionItems = mutableListOf<String>()
        val actionRegex = Regex("(?i)^(- \\[ \\]|\\* \\[ \\]|todo[:\\s-]|fixme[:\\s-]|action[:\\s-]|deadline[:\\s-]|must |need to |implement |verify |schedule |fix )")
        for (line in nonBlankLines) {
            val stripped = line.removePrefix("- [ ]").removePrefix("* [ ]").removePrefix("-").removePrefix("*").trim()
            if (actionRegex.containsMatchIn(line) || line.contains("TODO", ignoreCase = true) || line.contains("FIXME", ignoreCase = true)) {
                if (stripped.length in 5..140 && !actionItems.contains(stripped)) {
                    actionItems.add(stripped)
                }
            }
            if (actionItems.size >= 6) break
        }
        if (actionItems.isEmpty() && nonBlankLines.isNotEmpty()) {
            actionItems.add("Review $cleanName ($wordCount words, $lineCount lines)")
            headings.take(2).forEach { topic ->
                actionItems.add("Follow up on section: ${topic.take(60)}")
            }
        }

        // Compute structural metrics based on file type
        val structuralMetrics = mutableListOf<String>()
        structuralMetrics.add("$wordCount words • $lineCount lines • ~$estimatedTokens tokens")
        when {
            lowerName.endsWith(".csv") -> {
                val cols = nonBlankLines.firstOrNull()?.split(",")?.size ?: 0
                structuralMetrics.add("CSV Table: $cols columns × ${(nonBlankLines.size - 1).coerceAtLeast(0)} data rows")
            }
            lowerName.endsWith(".json") -> {
                val keyMatches = Regex("\"([^\"]+)\"\\s*:").findAll(normalized).count()
                structuralMetrics.add("JSON Structure: $keyMatches key-value properties")
            }
            lowerName.endsWith(".kt") || lowerName.endsWith(".py") || lowerName.endsWith(".js") -> {
                val fnCount = Regex("\\b(fun|def|function)\\b").findAll(normalized).count()
                val classCount = Regex("\\b(class|interface|object)\\b").findAll(normalized).count()
                structuralMetrics.add("Code Structure: $classCount classes/types • $fnCount functions")
            }
            else -> {
                val paragraphCount = normalized.split(Regex("\n\\s*\n")).count { it.isNotBlank() }
                structuralMetrics.add("Document Structure: $paragraphCount paragraphs • ${headings.size} key topics")
            }
        }

        val summaryPreview = nonBlankLines.take(3).joinToString(" ") { it.take(100) }
            .ifBlank { "Empty document." }

        val executiveSummary = buildString {
            append("Analyzed **$cleanName** ($fileTypeLabel — $wordCount words, $lineCount lines, ~$estimatedTokens tokens). ")
            append("Key focus: ${summaryPreview.take(180)}")
        }

        val exportMarkdown = buildString {
            appendLine("# Document Analysis Report: $cleanName")
            appendLine("- **File Type**: $fileTypeLabel")
            appendLine("- **Metrics**: ${structuralMetrics.joinToString(" | ")}")
            appendLine("- **Engine**: $activeModelName")
            appendLine()
            appendLine("## Executive Summary")
            appendLine(executiveSummary)
            appendLine()
            appendLine("## Key Topics & Structure")
            headings.forEach { h -> appendLine("- $h") }
            appendLine()
            appendLine("## Extracted Action Items")
            actionItems.forEach { a -> appendLine("- [ ] $a") }
        }

        return DocumentAnalysisReport(
            fileName = cleanName,
            fileTypeLabel = fileTypeLabel,
            charCount = charCount,
            wordCount = wordCount,
            lineCount = lineCount,
            estimatedTokens = estimatedTokens,
            executiveSummary = executiveSummary,
            keyHeadingsAndTopics = headings,
            extractedActionItems = actionItems,
            structuralMetrics = structuralMetrics,
            exportMarkdownContent = exportMarkdown,
            analyzedByModel = activeModelName
        )
    }

    /**
     * Packages all files of a [CodingAgentProject] into a real, standards-compliant ZIP archive byte array.
     */
    fun buildProjectZipBytes(project: CodingAgentProject): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val addedPaths = mutableSetOf<String>()
            for (vFile in project.files) {
                val safePath = vFile.path.trim().removePrefix("/").ifBlank { "file_${addedPaths.size}.txt" }
                if (addedPaths.add(safePath)) {
                    val entry = ZipEntry(safePath)
                    zos.putNextEntry(entry)
                    zos.write(vFile.content.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }
            }

            if (project.bundledPreviewHtml.isNotBlank() && addedPaths.add("bundle_preview.html")) {
                val previewEntry = ZipEntry("bundle_preview.html")
                zos.putNextEntry(previewEntry)
                zos.write(project.bundledPreviewHtml.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }

            if (addedPaths.add("README.md")) {
                val readme = buildString {
                    appendLine("# ${project.name}")
                    appendLine()
                    appendLine(project.description)
                    appendLine()
                    appendLine("## Included Files (${project.files.size})")
                    project.files.forEach { f ->
                        appendLine("- `${f.path}` (${f.language}, ${f.content.lines().size} lines)")
                    }
                }
                val readmeEntry = ZipEntry("README.md")
                zos.putNextEntry(readmeEntry)
                zos.write(readme.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    /**
     * Packages the project's live interactive HTML bundle as a standalone UTF-8 HTML file.
     */
    fun buildStandaloneHtmlBytes(project: CodingAgentProject): ByteArray {
        val html = project.bundledPreviewHtml.ifBlank {
            project.files.firstOrNull { it.path.endsWith(".html", ignoreCase = true) }?.content
                ?: "<!DOCTYPE html><html><body><h1>${project.name}</h1></body></html>"
        }
        return html.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Helper to inspect entry names inside a ZIP byte array (used for verification and tests).
     */
    fun listZipEntryNames(zipBytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                names.add(entry.name)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return names
    }
}
