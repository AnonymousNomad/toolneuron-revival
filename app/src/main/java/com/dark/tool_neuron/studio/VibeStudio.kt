package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.Verdict
import com.dark.tool_neuron.laptop.WorkspaceRef
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VibeStudio(private val core: LaptopCore, private val workspacesRoot: File, private val nowMs: () -> Long = System::currentTimeMillis) {
    @Synchronized fun create(name: String, templateId: String): WorkspaceRef {
        requireGrant()
        val template = VibeTemplates.all.singleOrNull { it.id == templateId } ?: error("Unknown template")
        return core.createWorkspace(name, mapOf("studio.kind" to "vibe", "studio.template" to template.id),
            template.files(name).mapKeys { "src/${it.key}" }).getOrThrow()
    }

    @Synchronized fun files(workspaceId: String): List<String> {
        requireGrant()
        return sourceFiles(root(workspaceId)).map { it.first }
    }

    @Synchronized fun read(workspaceId: String, path: String): String {
        requireGrant()
        require(path.substringAfterLast('.').lowercase() in TEXT_TYPES) { "This asset is not editable text" }
        return bounded(sourceFile(root(workspaceId), path), MAX_FILE_BYTES).toString(Charsets.UTF_8)
    }

    @Synchronized fun save(workspaceId: String, path: String, text: String) {
        requireGrant()
        require(path.substringAfterLast('.').lowercase() in TEXT_TYPES) { "Choose a text source extension" }
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FILE_BYTES) { "Source file exceeds 512 KiB" }
        val target = sourceFile(root(workspaceId), path)
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs()) { "Cannot create source directory" }
        val partial = File.createTempFile(".edit-", ".partial", target.parentFile)
        try {
            FileOutputStream(partial).use { it.write(bytes); it.fd.sync() }
            check(partial.renameTo(target)) { "Could not replace source file" }
        } finally { partial.delete() }
    }

    @Synchronized fun build(workspaceId: String): VibeBuildRecord {
        requireGrant()
        val root = root(workspaceId)
        var totalBytes = 0L
        val sources = sourceFiles(root).associate { (path, file) ->
            val bytes = bounded(file, MAX_FILE_BYTES)
            totalBytes += bytes.size
            require(totalBytes <= MAX_TOTAL_BYTES) { "Source bundle exceeds 4 MiB" }
            path to bytes
        }
        require(sources.isNotEmpty()) { "Project has no source files" }
        require(sources.values.sumOf { it.size.toLong() } <= MAX_TOTAL_BYTES) { "Source bundle exceeds 4 MiB" }
        val entry = sources["index.html"]?.toString(Charsets.UTF_8)
        val entryCheck = VibeCheck("static.entrypoint", entry?.contains(Regex("(?i)<html\\b")) == true,
            "index.html must contain an HTML document")
        val missing = mutableListOf<String>()
        sources.filterKeys { it.endsWith(".html", true) || it.endsWith(".css", true) }.forEach { (path, data) ->
            val text = data.toString(Charsets.UTF_8)
            val html = path.endsWith(".html", true)
            val css = if (html) STYLE_BLOCK.findAll(text).joinToString("\n") { it.groupValues[1] } + "\n" +
                STYLE_ATTR.findAll(text).joinToString("\n") { it.groupValues.drop(1).first { value -> value.isNotEmpty() } } else text
            val refs = (if (html) ASSET_REF.findAll(text).map { match ->
                match.groupValues.drop(1).first { it.isNotEmpty() }
            }.toList() else emptyList()) +
                CSS_REF.findAll(css).map { it.groupValues[1].trim().trim('"', '\'') }.toList() +
                CSS_IMPORT.findAll(css).map { it.groupValues[1] }.toList()
            refs.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("data:") }.forEach { ref ->
                val clean = URLDecoder.decode(ref.substringBefore('#').substringBefore('?'), "UTF-8")
                val relative = if (clean.contains(':') || clean.startsWith('/')) null else runCatching {
                    val sourceRoot = File(root, "src").canonicalFile
                    val resolved = File(File(sourceRoot, path).parentFile, clean).canonicalFile
                    if (!resolved.toPath().startsWith(sourceRoot.toPath())) null else resolved.relativeTo(sourceRoot).invariantSeparatorsPath
                }.getOrNull()
                if (relative == null || relative !in sources) missing += "$path → $ref"
            }
        }
        val checks = listOf(entryCheck,
            VibeCheck("static.local-assets", missing.isEmpty(), if (missing.isEmpty()) "Declared HTML/CSS asset references resolve inside the snapshot" else missing.take(10).joinToString("; ").take(8000)),
            VibeCheck("static.supported-document", sources.filterKeys { it.endsWith(".html", true) }.values.none {
                UNSUPPORTED_HTML.containsMatchIn(it.toString(Charsets.UTF_8))
            }, "Use src for images; document base overrides, srcset and embedded media/frames/objects are unsupported"))
        val artifacts = sources.toSortedMap().map { (path, bytes) -> VibeArtifactFile(path, sha(bytes), bytes.size.toLong()) }
        val digest = sha(artifacts.joinToString("\n") { "${it.path}:${it.sha256}:${it.bytes}" }.toByteArray())
        val id = "vb-" + UUID.randomUUID().toString().replace("-", "")
        val buildRoot = contained(root, ".vibe")
        check(buildRoot.isDirectory || buildRoot.mkdirs()) { "Cannot create build directory" }
        val stage = File(buildRoot, ".$id.partial")
        val output = File(buildRoot, id)
        check(stage.mkdir()) { "Cannot stage build" }
        try {
            sources.forEach { (path, bytes) ->
                val target = contained(stage, path)
                check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                FileOutputStream(target).use { it.write(bytes); it.fd.sync() }
            }
            check(stage.renameTo(output)) { "Cannot publish immutable snapshot" }
        } finally { stage.deleteRecursively() }
        val record = VibeBuildRecord(id, workspaceId, nowMs(), artifacts, checks,
            if (checks.all { it.passed }) Verdict.PASS else Verdict.FAIL, digest)
        return core.recordVibeBuild(record).getOrThrow()
    }

    @Synchronized fun preview(workspaceId: String, buildId: String, path: String): ByteArray {
        requireGrant()
        val record = receipt(workspaceId, buildId)
        check(record.verdict == Verdict.PASS) { "Build checks failed; repair the source and rebuild" }
        val artifact = record.files.singleOrNull { it.path == path } ?: error("Asset is outside this build")
        val bytes = bounded(contained(contained(root(workspaceId), ".vibe/$buildId"), path), MAX_FILE_BYTES)
        check(bytes.size.toLong() == artifact.bytes && sha(bytes) == artifact.sha256) { "Build snapshot changed; rebuild before preview/export" }
        return bytes
    }

    @Synchronized fun export(workspaceId: String, buildId: String, output: OutputStream) {
        requireGrant()
        val record = receipt(workspaceId, buildId)
        check(record.verdict == Verdict.PASS) { "Build checks failed" }
        val bytes = record.files.associate { it.path to preview(workspaceId, buildId, it.path) }
        ZipOutputStream(output).use { zip ->
            bytes.forEach { (path, data) -> zip.putNextEntry(ZipEntry("site/$path")); zip.write(data); zip.closeEntry() }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(Json.encodeToString(VibeBuildRecord.serializer(), record).toByteArray())
            zip.closeEntry()
        }
    }

    private fun receipt(workspaceId: String, buildId: String): VibeBuildRecord {
        require(buildId.matches(Regex("vb-[a-f0-9]{32}"))) { "Invalid build id" }
        return core.state.value.vibeBuilds.singleOrNull { it.id == buildId && it.workspaceId == workspaceId } ?: error("Unknown build for this project")
    }
    private fun root(id: String): File {
        require(core.state.value.workspaces.any { it.id == id }) { "Unknown workspace" }
        val root = contained(workspacesRoot.canonicalFile, id)
        require(root.isDirectory) { "Workspace directory is missing" }
        return root
    }
    private fun sourceFile(root: File, path: String): File {
        validPath(path)
        return contained(contained(root, "src"), path)
    }
    private fun sourceFiles(root: File): List<Pair<String, File>> {
        val source = contained(root, "src")
        require(source.isDirectory) { "No source directory; create a Vibe project from a template" }
        val result = mutableListOf<Pair<String, File>>()
        fun walk(dir: File, depth: Int) {
            require(depth <= 5) { "Source tree is too deep" }
            dir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                require(file.canonicalFile == file.absoluteFile) { "Source symlinks are unsupported" }
                val path = file.relativeTo(source).invariantSeparatorsPath
                validPath(path)
                if (file.isDirectory) walk(file, depth + 1) else {
                    require(file.extension.lowercase() in ASSET_TYPES) { "Unsupported source asset: $path" }
                    result += path to file
                    require(result.size <= 64) { "Source bundle exceeds 64 files" }
                }
            }
        }
        walk(source, 0)
        return result
    }
    private fun validPath(path: String) {
        require(path.length in 1..160 && path.matches(Regex("[A-Za-z0-9_./-]+")) &&
            !path.startsWith('/') && path.split('/').all { it.isNotEmpty() && !it.startsWith('.') }) { "Use a relative visible source path without traversal" }
    }
    private fun contained(root: File, path: String): File {
        val base = root.canonicalFile
        val file = File(base, path).canonicalFile
        require(file != base && file.toPath().startsWith(base.toPath())) { "Path leaves the project" }
        return file
    }
    private fun bounded(file: File, limit: Int): ByteArray {
        require(file.isFile && file.length() <= limit) { "File is missing or exceeds size limit" }
        return file.inputStream().use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                check(out.size() + n <= limit) { "File grew past size limit" }
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
    }
    private fun requireGrant() { check(core.hasPermission(CAPABILITY)) { "Grant Vibe Studio in Abilities before opening or changing project files" } }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    companion object {
        const val CAPABILITY = "dev.vibe.workspace@1"
        private const val MAX_FILE_BYTES = 512 * 1024
        private const val MAX_TOTAL_BYTES = 4 * 1024 * 1024
        private val TEXT_TYPES = setOf("html", "css", "js", "json", "txt", "svg")
        private val ASSET_TYPES = TEXT_TYPES + setOf("png", "jpg", "jpeg", "webp", "gif")
        private val ASSET_REF = Regex("""(?is)<(?:script|img|link|source|video|audio)\b[^>]*?\b(?:src|href)\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))""")
        private val UNSUPPORTED_HTML = Regex("""(?i)<(?:base|iframe|object|embed|video|audio|source|track)\b|\bsrcset\s*=""")
        private val STYLE_BLOCK = Regex("""(?is)<style\b[^>]*>(.*?)</style\s*>""")
        private val STYLE_ATTR = Regex("""(?is)\bstyle\s*=\s*(?:"([^"]+)"|'([^']+)')""")
        private val CSS_IMPORT = Regex("""(?i)@import\s+["']([^"']+)["']""")
        private val CSS_REF = Regex("""(?i)url\(\s*([^)]*)\s*\)""")
    }
}
