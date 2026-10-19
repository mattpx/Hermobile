package com.hermobile.app.runtime

import com.hermobile.app.model.ChangeItem
import com.hermobile.app.model.ChatMessage
import com.hermobile.app.model.ProjectKind
import com.hermobile.app.model.ProviderKind
import com.hermobile.app.model.ProviderProfile
import com.hermobile.app.model.ProviderProtocol
import com.hermobile.app.model.RuntimeEvent
import com.hermobile.app.model.ToolRequest
import com.hermobile.app.network.LocalNetwork
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase-1 Hermes bridge: website builder over direct provider HTTP, no PRoot.
 *
 * Speaks Anthropic Messages, OpenAI Chat Completions, OpenAI Responses, and
 * OpenRouter natively using the saved BYOK key (Keystore-backed [secretFor]).
 * Supports every [ProviderKind] incl. CUSTOM base URLs — the protocol enum
 * decides the wire format, so any Anthropic/OpenAI-compatible endpoint works.
 *
 * Website flow: asks the model for changed/created files as JSON file blocks,
 * writes them into the app-private workspace, then emits FilesChanged (Undo/
 * Keep reuse [WorkspaceCheckpoints]) and PreviewStarted for index.html.
 */
class DirectApiBridge(
    private val context: android.content.Context,
    private val secretFor: (ProviderProfile) -> String?,
) : RuntimeBridge {

    private val checkpoints = WorkspaceCheckpoints(context.filesDir, context)
    private val eventBus = MutableSharedFlow<RuntimeEvent>(extraBufferCapacity = 128)
    override val events: Flow<RuntimeEvent> = eventBus
    private val stoppedSessions = ConcurrentHashMap.newKeySet<String>()
    private val stopFlags = ConcurrentHashMap<String, AtomicBoolean>()
    @Volatile private var activeSessionId: String? = null

    override suspend fun startSession(
        projectId: String,
        projectSlug: String,
        projectKind: ProjectKind,
        prompt: String,
        conversationHistory: List<ChatMessage>,
        provider: ProviderProfile,
    ): String = withContext(Dispatchers.IO + NonCancellable) {
        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId
        stopFlags[sessionId] = AtomicBoolean(false)
        eventBus.emit(RuntimeEvent.SessionStarted(sessionId))
        try {
            val secret = secretFor(provider).orEmpty()
            if (provider.kind != ProviderKind.CLAUDE && secret.isBlank()) {
                fail(sessionId, "No API key is saved for ${provider.kind.title}.")
                return@withContext sessionId
            }
            if (provider.kind == ProviderKind.CLAUDE) {
                fail(
                    sessionId,
                    "Claude subscription login needs the full agent runtime (Phase 2). " +
                        "Pick a key-based provider or Custom API for website building.",
                )
                return@withContext sessionId
            }
            if (provider.resolvedBaseUrl.isBlank()) {
                fail(sessionId, "Enter a Base URL for ${provider.kind.title} first (Custom API).")
                return@withContext sessionId
            }
            if (provider.model.isBlank()) {
                fail(sessionId, "Pick a model for ${provider.kind.title} first.")
                return@withContext sessionId
            }

            val workspace = checkpoints.ensureWorkspace(projectId)
            checkpoints.createCheckpoint(projectId, workspace)
            val before = checkpoints.snapshot(workspace)

            eventBus.emit(
                RuntimeEvent.RuntimeLog(
                    sessionId,
                    "Building website",
                    "Asking ${provider.model} …",
                ),
            )
            val existingFiles = listWorkspaceFiles(workspace)
            val history = conversationHistory.recentWithinCharacterBudget(MAX_HISTORY_CHARS)
            val systemPrompt = websiteSystemPrompt(existingFiles)
            val userPrompt = buildUserPrompt(prompt, history)

            val raw = callProvider(
                sessionId = sessionId,
                provider = provider,
                secret = secret,
                system = systemPrompt,
                user = userPrompt,
            ) ?: return@withContext sessionId // failure already emitted

            if (stopFlags[sessionId]?.get() == true) {
                fail(sessionId, "Stopped by user")
                return@withContext sessionId
            }

            val (replyText, files) = parseFileBlocks(raw)
            val streamed = replyText.ifBlank { "Done — files updated below." }
            eventBus.emit(RuntimeEvent.AssistantDelta(sessionId, streamed))

            if (files.isNotEmpty()) {
                applyFiles(workspace, files)
            }
            val changed = checkpoints.changedFiles(workspace, before)
            if (changed.isNotEmpty()) {
                checkpoints.saveChangedPaths(projectId, changed)
                val details = checkpoints.buildChangeDetails(
                    projectId, workspace, checkpoints.readChangedPaths(projectId),
                )
                eventBus.emit(RuntimeEvent.FilesChanged(sessionId, details))
            }

            val preview = findPreviewFile(workspace)
            if (preview != null) {
                eventBus.emit(RuntimeEvent.PreviewStarted(sessionId, previewUrl(workspace, preview)))
            } else if (files.isEmpty()) {
                eventBus.emit(
                    RuntimeEvent.RuntimeLog(
                        sessionId,
                        "No files changed",
                        "The model returned no file blocks. Ask it to write index.html.",
                    ),
                )
            }

            eventBus.emit(RuntimeEvent.SessionCompleted(sessionId))
        } catch (e: Exception) {
            fail(sessionId, e.message?.take(300) ?: "Direct API request failed.")
        } finally {
            stopFlags.remove(sessionId)
            if (activeSessionId == sessionId) activeSessionId = null
        }
        sessionId
    }

    override suspend fun respondToApproval(request: ToolRequest, approved: Boolean) {
        // No interactive tool approvals in Phase 1: files apply directly with Undo/Keep.
    }

    override suspend fun stopSession(sessionId: String) {
        stopFlags[sessionId]?.set(true)
        if (activeSessionId == sessionId) {
            withContext(Dispatchers.IO + NonCancellable) {
                eventBus.emit(RuntimeEvent.SessionFailed(sessionId, "Stopped by user"))
            }
            activeSessionId = null
        }
    }

    override suspend fun stopActiveSession() {
        activeSessionId?.let { stopSession(it) }
    }

    override suspend fun undoLastChanges(projectId: String): Boolean = withContext(Dispatchers.IO) {
        val checkpoint = checkpoints.checkpointDir(projectId)
        val backup = File(checkpoint, "project")
        val manifest = File(checkpoint, "changes.json")
        if (!backup.isDirectory || !manifest.isFile) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val paths = checkpoints.readChangedPaths(projectId)
        paths.forEach { path ->
            val target = runCatching { checkpoints.safeWorkspaceFile(workspace, path) }.getOrNull()
                ?: return@forEach
            val original = runCatching { checkpoints.safeWorkspaceFile(backup, path) }.getOrNull()
            if (original != null && original.isFile) {
                target.parentFile?.mkdirs()
                original.copyTo(target, overwrite = true)
            } else if (target.isFile) {
                target.delete()
            }
        }
        checkpoint.deleteRecursively()
        true
    }

    override suspend fun acceptLastChanges(projectId: String) {
        withContext(Dispatchers.IO) {
            checkpoints.checkpointDir(projectId).deleteRecursively()
        }
    }

    override suspend fun loadPendingChanges(projectId: String): List<ChangeItem> =
        withContext(Dispatchers.IO) {
            val workspace = checkpoints.ensureWorkspace(projectId)
            val paths = checkpoints.readChangedPaths(projectId)
            if (paths.isEmpty()) emptyList()
            else checkpoints.buildChangeDetails(projectId, workspace, paths)
        }

    override suspend fun undoFileChange(projectId: String, path: String): Boolean =
        withContext(Dispatchers.IO) {
            if (path !in checkpoints.readChangedPaths(projectId)) return@withContext false
            val workspace = checkpoints.ensureWorkspace(projectId)
            val backup = File(checkpoints.checkpointDir(projectId), "project")
            val target = runCatching { checkpoints.safeWorkspaceFile(workspace, path) }.getOrNull()
                ?: return@withContext false
            val original = runCatching { checkpoints.safeWorkspaceFile(backup, path) }.getOrNull()
            if (original != null && original.isFile) {
                target.parentFile?.mkdirs()
                original.copyTo(target, overwrite = true)
            } else if (target.isFile) {
                target.delete()
            }
            checkpoints.removeChangedPath(projectId, path)
            true
        }

    override suspend fun acceptFileChange(projectId: String, path: String): Boolean =
        withContext(Dispatchers.IO) {
            if (path !in checkpoints.readChangedPaths(projectId)) return@withContext false
            val workspace = checkpoints.ensureWorkspace(projectId)
            val backup = File(checkpoints.checkpointDir(projectId), "project")
            val current = runCatching { checkpoints.safeWorkspaceFile(workspace, path) }.getOrNull()
            val baseline = runCatching { checkpoints.safeWorkspaceFile(backup, path) }.getOrNull()
            if (current != null && baseline != null) {
                if (current.isFile) {
                    baseline.parentFile?.mkdirs()
                    current.copyTo(baseline, overwrite = true)
                } else if (baseline.isFile) {
                    baseline.delete()
                }
            }
            checkpoints.removeChangedPath(projectId, path)
            true
        }

    // ---------- provider HTTP ----------

    private suspend fun callProvider(
        sessionId: String,
        provider: ProviderProfile,
        secret: String,
        system: String,
        user: String,
    ): String? {
        val endpoint = messagesEndpoint(provider)
        if (!LocalNetwork.isPlainHttpAllowed(endpoint)) {
            fail(sessionId, "Plain http:// is only allowed for local network addresses. Use https://.")
            return null
        }
        val body = requestBody(provider, system, user)
        val result = runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 120_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                if (secret.isNotBlank()) setRequestProperty("Authorization", "Bearer $secret")
                when (provider.kind.protocol) {
                    ProviderProtocol.ANTHROPIC,
                    ProviderProtocol.ANTHROPIC_GATEWAY,
                    -> {
                        // Anthropic-native auth: x-api-key + version header.
                        setRequestProperty("x-api-key", secret)
                        setRequestProperty("anthropic-version", "2023-06-01")
                        if (provider.kind.protocol == ProviderProtocol.ANTHROPIC_GATEWAY &&
                            provider.kind == ProviderKind.OPENCODE_ZEN
                        ) {
                            setRequestProperty("User-Agent", "opencode/1.18.20")
                        }
                    }
                    ProviderProtocol.OPENROUTER -> {
                        setRequestProperty("HTTP-Referer", "https://hermes-mobile.local")
                        setRequestProperty("X-Title", "Hermes Mobile Website Builder")
                        val order = provider.openRouterProviders
                        if (order.isNotEmpty()) {
                            // merged into body below
                        }
                    }
                    ProviderProtocol.OPENAI_CHAT,
                    ProviderProtocol.OPENAI_RESPONSES,
                    ProviderProtocol.CLAUDE_LOGIN,
                    -> Unit
                }
                doOutput = true
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            code to responseBody
        }.getOrElse {
            fail(sessionId, "Network error: ${it.message?.take(200) ?: "connection failed"}")
            return null
        }
        val (code, responseBody) = result
        if (code !in 200..299) {
            fail(sessionId, "${httpError(code)}${providerHint(responseBody)}")
            return null
        }
        val text = extractText(provider.kind.protocol, responseBody)
        if (text.isBlank()) {
            fail(sessionId, "The provider returned an empty reply. Try again.")
            return null
        }
        // Stream into chat in chunks so long pages render progressively.
        text.chunked(600).forEach { chunk ->
            if (stopFlags[sessionId]?.get() == true) return null
            eventBus.emit(RuntimeEvent.AssistantDelta(sessionId, chunk))
        }
        return text
    }

    private fun messagesEndpoint(provider: ProviderProfile): String {
        val base = provider.resolvedBaseUrl.trim().trimEnd('/')
        return when (provider.kind.protocol) {
            ProviderProtocol.OPENROUTER -> if (base.endsWith("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"
            ProviderProtocol.OPENAI_CHAT -> if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
            ProviderProtocol.OPENAI_RESPONSES -> if (base.endsWith("/responses")) base else "$base/responses"
            ProviderProtocol.ANTHROPIC -> if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
            ProviderProtocol.ANTHROPIC_GATEWAY -> if (base.endsWith("/messages")) base
            else if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
            ProviderProtocol.CLAUDE_LOGIN -> base
        }
    }

    private fun requestBody(provider: ProviderProfile, system: String, user: String): String {
        val model = provider.model
        return when (provider.kind.protocol) {
            ProviderProtocol.OPENAI_RESPONSES -> JSONObject()
                .put("model", model)
                .put("instructions", system)
                .put("input", user)
                .toString()
            ProviderProtocol.OPENAI_CHAT, ProviderProtocol.OPENROUTER -> JSONObject()
                .put("model", model)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", user)))
                .put("temperature", 0.4)
                .also { body ->
                    if (provider.kind.protocol == ProviderProtocol.OPENROUTER) {
                        val order = provider.openRouterProviders
                        if (order.isNotEmpty()) {
                            body.put("provider", JSONObject()
                                .put("order", JSONArray(order))
                                .put("allow_fallbacks", provider.openRouterAllowFallbacks))
                        }
                    }
                }
                .toString()
            else -> JSONObject()
                .put("model", model)
                .put("max_tokens", 8_000)
                .put("system", system)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", user)))
                .toString()
        }
    }

    private fun extractText(protocol: ProviderProtocol, body: String): String {
        return runCatching {
            val root = JSONObject(body)
            when (protocol) {
                ProviderProtocol.OPENAI_RESPONSES -> {
                    // Responses API: output[].content[].text
                    val out = root.optJSONArray("output") ?: JSONArray()
                    buildString {
                        for (i in 0 until out.length()) {
                            val item = out.optJSONObject(i) ?: continue
                            val content = item.optJSONArray("content") ?: continue
                            for (j in 0 until content.length()) {
                                val part = content.optJSONObject(j) ?: continue
                                val t = part.optString("text", "")
                                if (t.isNotBlank()) append(t)
                            }
                        }
                    }.ifBlank { root.optString("output_text", "") }
                }
                ProviderProtocol.OPENAI_CHAT, ProviderProtocol.OPENROUTER -> {
                    root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content", "").orEmpty()
                }
                else -> {
                    val content = root.optJSONArray("content") ?: JSONArray()
                    buildString {
                        for (i in 0 until content.length()) {
                            val block = content.optJSONObject(i) ?: continue
                            if (block.optString("type") == "text") append(block.optString("text", ""))
                        }
                    }
                }
            }
        }.getOrDefault("")
    }

    private fun httpError(code: Int): String = when (code) {
        401, 403 -> "The provider rejected the saved API key."
        404 -> "Endpoint not found — check the Base URL."
        429 -> "Rate limited — wait a moment, then retry."
        in 500..599 -> "The provider is temporarily unavailable (HTTP $code)."
        else -> "Request failed (HTTP $code)."
    }

    private fun providerHint(body: String): String {
        if (body.isBlank()) return ""
        val msg = runCatching {
            val root = JSONObject(body)
            when (val e = root.opt("error")) {
                is JSONObject -> e.optString("message")
                is String -> e
                else -> root.optString("message")
            }
        }.getOrDefault("").trim().replace(Regex("\\s+"), " ").take(200)
        return if (msg.isBlank()) "" else " $msg"
    }

    private suspend fun fail(sessionId: String, reason: String) {
        if (stoppedSessions.add(sessionId)) {
            eventBus.emit(RuntimeEvent.SessionFailed(sessionId, reason))
        }
    }

    // ---------- website file handling ----------

    private fun websiteSystemPrompt(existingFiles: List<String>): String = buildString {
        appendLine("You are a website builder inside an Android app. Build complete, working websites.")
        appendLine("Rules:")
        appendLine("1. Output file changes ONLY inside fenced blocks tagged with the file path:")
        appendLine("   ```file:index.html")
        appendLine("   ...full file content...")
        appendLine("   ```")
        appendLine("2. Every file block must contain the COMPLETE file, never a diff or snippet.")
        appendLine("3. Keep it self-contained: inline CSS/JS preferred, no build step, no npm.")
        appendLine("4. `index.html` at the workspace root is the entry page and preview target.")
        appendLine("5. Be concise outside file blocks: one short summary of what changed.")
        if (existingFiles.isNotEmpty()) {
            appendLine("Existing workspace files (update or extend them):")
            existingFiles.take(60).forEach { appendLine("- $it") }
        } else {
            appendLine("The workspace is empty — create index.html plus any assets it needs.")
        }
    }

    private fun buildUserPrompt(prompt: String, history: List<ChatMessage>): String = buildString {
        if (history.isNotEmpty()) {
            appendLine("Conversation so far:")
            history.takeLast(20).forEach { m ->
                appendLine((if (m.fromUser) "User: " else "Builder: ") + m.text.take(2_000))
            }
            appendLine()
        }
        appendLine("Request: $prompt")
    }

    private fun listWorkspaceFiles(workspace: File): List<String> {
        if (!workspace.isDirectory) return emptyList()
        return workspace.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(workspace).invariantSeparatorsPath }
            .filter { !checkpoints.isInternalRuntimePath(it) && !it.startsWith(".") }
            .sorted()
            .take(200)
            .toList()
    }

    private data class SiteFile(val path: String, val content: String)

    private fun parseFileBlocks(raw: String): Pair<String, List<SiteFile>> {
        val files = mutableListOf<SiteFile>()
        // ```file:path ... ```  (also tolerates ```html:path / bare ```path)
        val fence = Regex("```(?:file:)?([^`\\n]*?)\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        var reply = raw
        fence.findAll(raw).forEach { match ->
            val rawPath = match.groupValues[1].trim()
            val content = match.groupValues[2]
            val path = sanitizePath(rawPath)
            if (path != null && content.isNotBlank()) {
                files += SiteFile(path, content.trimEnd() + "\n")
                reply = reply.replace(match.value, "[wrote $path]")
            }
        }
        // Fallback: a bare full-HTML reply with no fence becomes index.html.
        val trimmed = reply.trim()
        if (files.isEmpty() && "<html" in trimmed.lowercase() && "</html>" in trimmed.lowercase()) {
            val html = extractHtml(trimmed)
            if (html.isNotBlank()) {
                files += SiteFile("index.html", html)
                reply = "[wrote index.html]"
            }
        }
        // Merge duplicate paths: last block wins.
        val merged = files.groupBy { it.path }.map { (path, blocks) -> SiteFile(path, blocks.last().content) }
        return reply.trim() to merged
    }

    private fun sanitizePath(rawPath: String): String? {
        var p = rawPath.trim().trimStart('/').trim()
        if (p.isBlank()) return null
        // tolerate "html:index.html" style fences
        p = p.substringAfterLast(':').trim().trimStart('/')
        if (p.isBlank() || p.contains("..") || p.startsWith('/') || p.length > 200) return null
        if (p.any { it == '\u0000' }) return null
        // only web-safe relative paths
        if (!p.matches(Regex("[A-Za-z0-9._\\-/]+"))) return null
        return p
    }

    private fun extractHtml(text: String): String {
        val lower = text.lowercase()
        val start = lower.indexOf("<!doctype html")
            .takeIf { it >= 0 } ?: lower.indexOf("<html")
        val end = lower.lastIndexOf("</html>")
        if (start < 0 || end < 0) return ""
        return text.substring(start, end + "</html>".length)
    }

    private fun applyFiles(workspace: File, files: List<SiteFile>) {
        files.take(MAX_FILES_PER_TURN).forEach { file ->
            val target = runCatching { checkpoints.safeWorkspaceFile(workspace, file.path) }.getOrNull()
                ?: return@forEach
            if (file.content.toByteArray().size > MAX_FILE_BYTES) return@forEach
            target.parentFile?.mkdirs()
            target.writeText(file.content)
        }
    }

    private fun findPreviewFile(workspace: File): String? {
        val candidates = listOf("index.html", "index.htm", "public/index.html", "dist/index.html")
        candidates.forEach { rel ->
            if (File(workspace, rel).isFile) return rel
        }
        // any html file as last resort
        return workspace.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("html", "htm") }
            .map { it.relativeTo(workspace).invariantSeparatorsPath }
            .sorted()
            .firstOrNull()
    }

    private fun previewUrl(workspace: File, rel: String): String =
        "file://${File(workspace, rel).absolutePath}"

    private companion object {
        const val MAX_HISTORY_CHARS = 12_000
        const val MAX_FILES_PER_TURN = 20
        const val MAX_FILE_BYTES = 1_000_000
    }
}
