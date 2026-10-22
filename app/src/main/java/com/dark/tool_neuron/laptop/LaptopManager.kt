package com.dark.tool_neuron.laptop

import android.content.Context
import com.dark.hxs_encryptor.HxsEncryptor
import com.dark.tool_neuron.data.AppKeyStore
import com.dark.tool_neuron.laptop.terminal.RealTerminalEffectProvider
import com.dark.tool_neuron.laptop.terminal.TerminalSessionManager
import com.dark.tool_neuron.model.ModelInfo
import com.dark.tool_neuron.model.enums.PathType
import com.dark.tool_neuron.util.GgufMeta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaptopManager @Inject constructor(
    @param:ApplicationContext context: Context,
    keyStore: AppKeyStore,
    encryptor: HxsEncryptor,
) {
    val workspacesRoot: File = File(context.filesDir, WORKSPACES_DIR)

    val core = LaptopCore(
        persistence = HxsLaptopPersistence(context, keyStore, encryptor),
        workspacesBaseDir = workspacesRoot,
    )

    val vibe = com.dark.tool_neuron.studio.VibeStudio(core, workspacesRoot)

    val state = core.state
    val tools = ToolRegistry.SEEDS

    val terminalSessions = TerminalSessionManager(context)

    val terminal: TerminalEffectProvider = RealTerminalEffectProvider(
        workspacesRoot = workspacesRoot,
        tmpDir = terminalSessions.tmpDir,
        workspaceDirOf = ::workspaceDir,
    )

    fun workspaceDir(id: String): File? = File(workspacesRoot, id).takeIf { it.isDirectory }

    fun listWorkspaceEntries(id: String): List<LaptopCore.WorkspaceEntry> {
        val dir = workspaceDir(id) ?: return emptyList()
        val out = mutableListOf<LaptopCore.WorkspaceEntry>()
        fun walk(d: File, rel: String, depth: Int) {
            if (depth > 3 || out.size >= 200) return
            d.listFiles()?.sortedBy { it.name }?.forEach { f ->
                if (out.size >= 200) return
                val r = if (rel.isEmpty()) f.name else "$rel/${f.name}"
                out += LaptopCore.WorkspaceEntry(r, f.isDirectory, if (f.isFile) f.length() else 0L)
                if (f.isDirectory) walk(f, r, depth + 1)
            }
        }
        walk(dir, "", 0)
        out.sortBy { it.path }
        return out
    }

    fun readWorkspaceFile(id: String, path: String): String? {
        val dir = workspaceDir(id) ?: return null
        if (path.isBlank()) return null
        val root = dir.canonicalFile
        val target = File(root, path).canonicalFile
        if (target != root && !target.path.startsWith(root.path + File.separator)) return null
        if (!target.isFile) return null
        return runCatching { target.readText().take(8000) }.getOrNull()
    }

    private val archCache = ConcurrentHashMap<String, String?>()

    suspend fun modelDossiers(models: List<ModelInfo>): List<ModelRegistry.ModelDossier> =
        withContext(Dispatchers.IO) {
            models.map { m -> ModelRegistry.dossier(m, architectureOf(m), null) }
        }

    private fun architectureOf(model: ModelInfo): String? {
        if (model.pathType != PathType.FILE) return null
        return archCache.getOrPut(model.path) { GgufMeta.readArchitecture(File(model.path)) }
    }

    private companion object {
        const val WORKSPACES_DIR = "workspaces"
    }
}
