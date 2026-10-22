package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.Verdict
import kotlinx.serialization.Serializable

@Serializable
data class VibeArtifactFile(val path: String, val sha256: String, val bytes: Long) {
    init {
        require(path.length in 1..160 && path.matches(Regex("[A-Za-z0-9_./-]+")) && !path.startsWith('/') &&
            path.split('/').all { it.isNotEmpty() && !it.startsWith('.') })
        require(sha256.matches(Regex("[a-f0-9]{64}")) && bytes in 0..512L * 1024)
    }
}
@Serializable
data class VibeCheck(val id: String, val passed: Boolean, val detail: String)
@Serializable
data class VibeBuildRecord(
    val id: String,
    val workspaceId: String,
    val createdAtMs: Long,
    val files: List<VibeArtifactFile>,
    val checks: List<VibeCheck>,
    val verdict: Verdict,
    val digest: String,
    val evidenceId: String = "",
) {
    init {
        require(id.matches(Regex("vb-[a-f0-9]{32}")) && workspaceId.isNotBlank() && createdAtMs > 0)
        require(files.size in 1..64 && files.map { it.path }.distinct().size == files.size && files.sumOf { it.bytes } <= 4L * 1024 * 1024)
        require(checks.map { it.id }.toSet() == setOf("static.entrypoint", "static.local-assets", "static.supported-document") && checks.size == 3)
        require(checks.all { it.detail.length <= 8000 })
        require(verdict == if (checks.all { it.passed }) Verdict.PASS else Verdict.FAIL)
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(files.sortedBy { it.path }
            .joinToString("\n") { "${it.path}:${it.sha256}:${it.bytes}" }.toByteArray())
            .joinToString("") { "%02x".format(it) }
        require(digest == expected)
    }
}

data class WorkstationPack(val id: String, val title: String, val capabilities: List<String>, val templates: List<String>, val batteries: List<String>)
object WorkstationPacks {
    val all = listOf(
        WorkstationPack("workstation.web@1", "Web and utility apps", listOf(VibeStudio.CAPABILITY, "dev.terminal.execute@1"), listOf("web", "utility"), listOf("local.instruction@1")),
        WorkstationPack("workstation.game@1", "Small browser games", listOf(VibeStudio.CAPABILITY), listOf("game"), listOf("local.structured@1")),
    )
}

data class VibeTemplate(val id: String, val title: String, val body: String, val script: String) {
    fun files(name: String): Map<String, String> {
        val title = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
        return mapOf(
            "index.html" to """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$title</title><link rel="stylesheet" href="style.css"></head><body><main><h1>$title</h1>$body</main><script src="app.js"></script></body></html>""",
            "style.css" to "body{font:18px system-ui;background:#121b23;color:#ecf5f2;margin:0;padding:24px}main{max-width:720px;margin:auto}button,input,textarea{font:inherit;padding:12px;margin:8px 0}button{background:#76dfb9;color:#10241b;border:0;border-radius:8px;min-height:48px}textarea{width:90%}#arena{height:240px;position:relative;border:1px solid #76dfb9}#target{position:absolute}",
            "app.js" to script,
        )
    }
}
object VibeTemplates {
    val all = listOf(
        VibeTemplate("web", "Landing page", "<p>A local-first web page. Edit the source, build, then preview.</p><button id=\"action\">Try it</button><p id=\"result\" aria-live=\"polite\"></p>",
            "document.getElementById('action').onclick=()=>{document.getElementById('result').textContent='Your page works offline.';};"),
        VibeTemplate("utility", "Text counter", "<label for=\"draft\">Your text</label><textarea id=\"draft\" rows=\"6\"></textarea><p id=\"result\" aria-live=\"polite\">0 characters</p>",
            "document.getElementById('draft').oninput=e=>{document.getElementById('result').textContent=e.target.value.length+' characters';};"),
        VibeTemplate("game", "Tap the target", "<p>Tap the target to score. Game mechanics run locally in the page.</p><p id=\"score\" aria-live=\"polite\">Score: 0</p><div id=\"arena\"><button id=\"target\">Tap me</button></div>",
            "let score=0;const target=document.getElementById('target');target.onclick=()=>{score++;document.getElementById('score').textContent='Score: '+score;target.style.left=Math.floor(Math.random()*65)+'%';target.style.top=Math.floor(Math.random()*170)+'px';};"),
    )
}
