package com.dark.tool_neuron.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.tool_neuron.BuildConfig
import com.dark.tool_neuron.data.SecurityManager
import com.dark.tool_neuron.data.SessionHolder
import com.dark.tool_neuron.laptop.LaptopManager
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.repo.ModelRepository
import com.dark.tool_neuron.reporting.*
import com.dark.tool_neuron.service.inference.InferenceClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

data class BetaReportPreview(val json: String, val archive: ByteArray, val screenshot: ByteArray?, val sha256: String)
@HiltViewModel
class BetaReportViewModel @Inject constructor(private val app: Application, private val laptop: LaptopManager,
    private val models: ModelRepository, private val security: SecurityManager, private val session: SessionHolder,
    private val saved: SavedStateHandle) : ViewModel() {
    val notes = saved.getStateFlow("report.notes", "")
    val status = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val preview = MutableStateFlow<BetaReportPreview?>(null)
    val screenshot = MutableStateFlow<ByteArray?>(null)
    private var screenshotRevision = 0L
    private fun unlocked() = !security.isLockEnabled || session.active.value
    init {
        viewModelScope.launch { session.active.collect { if (unlocked()) resumeScreenshot() } }
    }
    fun editNotes(value: String) { saved["report.notes"] = value.take(8000); preview.value = null }
    fun acceptScreenshot(uri: Uri?) {
        if (uri == null) return
        releaseScreenshotGrant(saved.get<String>("report.screenshotUri"))
        screenshotRevision++
        saved["report.screenshotUri"] = uri.toString()
        resumeScreenshot()
    }
    fun clearScreenshot() { releaseScreenshotGrant(saved.get<String>("report.screenshotUri")); screenshotRevision++; saved["report.screenshotUri"] = null; screenshot.value = null; preview.value = null }
    private fun resumeScreenshot() {
        val value = saved.get<String>("report.screenshotUri") ?: return
        if (!unlocked() || busy.value) return
        val revision = screenshotRevision
        action {
            val bytes = try { withContext(Dispatchers.IO) {
                val input = app.contentResolver.openInputStream(Uri.parse(value)) ?: error("Cannot read screenshot")
                val raw = input.use { stream ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) { val n = stream.read(buffer); if (n < 0) break; require(out.size() + n <= 4 * 1024 * 1024) { "Screenshot exceeds 4 MiB" }; out.write(buffer, 0, n) }
                    out.toByteArray()
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                require(bounds.outWidth in 1..16384 && bounds.outHeight in 1..16384) { "Invalid or oversized screenshot" }
                var sample = 1
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("Screenshot cannot be decoded")
                try { ByteArrayOutputStream().also { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray().also { require(it.size <= 4 * 1024 * 1024) } }
                finally { bitmap.recycle() }
            }
            } finally {
                releaseScreenshotGrant(value)
                if (saved.get<String>("report.screenshotUri") == value) saved["report.screenshotUri"] = null
            }
            check(unlocked()) { "Unlock and select the screenshot again" }
            if (revision == screenshotRevision) { screenshot.value = bytes; preview.value = null; saved["report.screenshotUri"] = null; status.value = "Screenshot added locally; review before sharing" }
        }
    }
    private fun releaseScreenshotGrant(value: String?) {
        value?.let { runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(it), Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }
    fun prepare() = action {
        check(unlocked()) { "Unlock before preparing a report" }
        val snapshot = laptop.state.value
        val selectedScreenshot = screenshot.value?.copyOf()
        val reproduction = notes.value
        val manifest = withContext(Dispatchers.IO) {
            val registeredModels = models.models.value.filter { it.pathType == PathType.FILE }.map { File(it.path).canonicalPath }.toSet()
            val imported = File(app.filesDir, "imported_models").listFiles()?.count { it.isFile && it.canonicalPath !in registeredModels } ?: 0
            val registeredWorkspaces = snapshot.workspaces.map { it.id }.toSet()
            val workspaceOrphans = laptop.workspacesRoot.listFiles()?.count { it.isDirectory && it.name !in registeredWorkspaces } ?: 0
            val quarantines = app.filesDir.listFiles()?.count { it.isDirectory && it.name.contains(".quarantine-") } ?: 0
            val buildOrphans = snapshot.workspaces.sumOf { workspace ->
                val root = laptop.workspaceDir(workspace.id) ?: return@sumOf 0
                val registered = snapshot.vibeBuilds.filter { it.workspaceId == workspace.id }.map { it.id }.toSet()
                (File(root, ".vibe").listFiles()?.count { it.isDirectory && it.name !in registered } ?: 0) + (root.listFiles()?.count { it.name.startsWith(".vibe-init-") } ?: 0)
            }
            val logs = mutableListOf("session.openCount=${snapshot.session.openCount}", "session.recovered=${snapshot.session.recoveredFromCorruption}",
                "inference.loaded=${InferenceClient.isModelLoaded.value}", "covert=BLOCKED_LOCAL_SOURCE_UNAVAILABLE", "projects=${snapshot.workspaces.size}")
            snapshot.tasks.takeLast(30).forEach { logs += "task.status=${it.status}; updatedAtMs=${it.updatedAtMs}" }
            snapshot.vibeBuilds.takeLast(20).forEach { logs += "vibe.verdict=${it.verdict}" }
            InferenceClient.lastCrashInfo.value?.let { logs += "lastError.module=${it.lib.take(80)}; category=${it.category?.take(80)}; code=${it.errorCode}; signal=${it.signal}" }
            BetaReportManifest(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, "${Build.MANUFACTURER} ${Build.MODEL}".take(256), Build.VERSION.RELEASE.take(100), Build.SUPPORTED_ABIS.take(10),
                models.models.value.take(100).map { "${it.name}; provider=${it.providerType}; storage=${it.pathType}".take(200) }, reproduction, logs.map { it.take(400) }.take(100), imported + workspaceOrphans + quarantines + buildOrphans,
                sourceRevision = BuildConfig.SOURCE_REVISION)
        }
        val result = withContext(Dispatchers.IO) {
            val bytes = BetaReports.bundle(manifest, selectedScreenshot)
            BetaReportPreview(BetaReports.preview(manifest), bytes, selectedScreenshot, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        }
        check(unlocked()) { "Unlock and prepare the report again" }
        preview.value = result
        status.value = "Review exact JSON and optional screenshot. Recovery candidates are preserved, never deleted automatically."
    }
    fun share() {
        val selected = preview.value ?: return
        action {
            check(unlocked()) { "Unlock before sharing" }
            val file = withContext(Dispatchers.IO) {
                File(File(app.cacheDir, "exports").apply { mkdirs() }, "toolneuron-report-${UUID.randomUUID()}.zip").apply { writeBytes(selected.archive) }
            }
            check(unlocked() && preview.value === selected) { "Report changed or app locked; review again" }
            val uri = FileProvider.getUriForFile(app, app.packageName + ".exports", file)
            app.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share reviewed beta report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            status.value = "System share chooser opened. No automatic upload was performed."
        }
    }
    private fun action(block: suspend () -> Unit) {
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch { try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { status.value = failure.message ?: "Report action failed" } finally { busy.value = false; if (saved.get<String>("report.screenshotUri") != null && unlocked()) resumeScreenshot() } }
    }
}
