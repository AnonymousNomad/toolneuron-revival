package com.dark.tool_neuron.laptop.resident

import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.KnowledgeRegistry
import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.ModelRegistry
import com.dark.tool_neuron.laptop.ProcedureRegistry
import com.dark.tool_neuron.laptop.ToolRegistry

enum class LookupKind(val id: String) {
    WORKSPACE_CURRENT("laptop.workspace.current"),
    WORKSPACE_READ("laptop.workspace.read"),
    CREATION_CURRENT("laptop.creation.current"),
    TASK_CURRENT("laptop.task.current"),
    CAPABILITY_FIND("laptop.capability.find"),
    MODEL_FIND("laptop.model.find"),
    KNOWLEDGE_FIND("laptop.knowledge.find"),
    PROCEDURE_FIND("laptop.procedure.find"),
    EFFECT_DESCRIBE("laptop.effect.describe"),
    EVIDENCE_FIND("laptop.evidence.find"),
    ;

    companion object {
        fun fromId(id: String): LookupKind? = entries.firstOrNull { it.id == id.trim() }
    }
}

data class LaptopLookupRequest(
    val kind: LookupKind,
    val query: String = "",
    val limit: Int = 4,
)

enum class LookupTrust { TRUSTED_SYSTEM, PROJECT_DATA }

data class LaptopLookupResult(
    val source: String,
    val refs: List<String>,
    val summary: String,
    val revision: String,
    val trust: LookupTrust,
    val truncated: Boolean,
)

class LaptopLookupService(
    private val core: LaptopCore,
    private val workspaceFileNames: (String) -> List<String>,
    private val workspaceFileContent: (String, String) -> String?,
    private val modelDossiers: () -> List<ModelRegistry.ModelDossier>,
) {
    fun execute(request: LaptopLookupRequest, workspaceId: String? = core.state.value.activeWorkspaceId): LaptopLookupResult {
        val limit = request.limit.coerceIn(1, 8)
        return when (request.kind) {
            LookupKind.WORKSPACE_CURRENT -> workspaceCurrent(limit, workspaceId)
            LookupKind.WORKSPACE_READ -> workspaceRead(request.query, workspaceId)
            LookupKind.CREATION_CURRENT -> creationCurrent(workspaceId, limit)
            LookupKind.TASK_CURRENT -> taskCurrent(workspaceId)
            LookupKind.CAPABILITY_FIND -> capabilityFind(request.query, limit)
            LookupKind.MODEL_FIND -> modelFind(request.query, limit)
            LookupKind.KNOWLEDGE_FIND -> knowledgeFind(request.query, limit)
            LookupKind.PROCEDURE_FIND -> procedureFind(request.query, limit)
            LookupKind.EFFECT_DESCRIBE -> effectDescribe(request.query)
            LookupKind.EVIDENCE_FIND -> evidenceFind(request.query, limit, workspaceId)
        }
    }

    private fun bound(text: String, cap: Int = SUMMARY_CAP): Pair<String, Boolean> {
        val t = text.trim()
        return if (t.length <= cap) t to false else t.take(cap) + "\n[truncated]" to true
    }

    private fun workspaceCurrent(limit: Int, workspaceId: String?): LaptopLookupResult {
        val ws = core.state.value.workspaces.firstOrNull { it.id == workspaceId }
            ?: return LaptopLookupResult(LookupKind.WORKSPACE_CURRENT.id, emptyList(), "no active workspace", REV, LookupTrust.TRUSTED_SYSTEM, false)
        val files = workspaceFileNames(ws.id).take(limit)
        val truncated = workspaceFileNames(ws.id).size > limit
        val summary = buildString {
            append("active workspace: ").append(ws.name).append(" (").append(ws.id).append(")")
            if (files.isEmpty()) append("\nfiles: (empty)") else append("\nfiles: ").append(files.joinToString(", "))
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.WORKSPACE_CURRENT.id, listOf(ws.id), bounded, REV, LookupTrust.TRUSTED_SYSTEM, truncated || cut)
    }

    private fun workspaceRead(path: String, workspaceId: String?): LaptopLookupResult {
        val ws = core.state.value.workspaces.firstOrNull { it.id == workspaceId }
            ?: return LaptopLookupResult(LookupKind.WORKSPACE_READ.id, emptyList(), "no active workspace", REV, LookupTrust.PROJECT_DATA, false)
        if (path.isBlank()) {
            return LaptopLookupResult(LookupKind.WORKSPACE_READ.id, emptyList(), "no path given", REV, LookupTrust.PROJECT_DATA, false)
        }
        val content = workspaceFileContent(ws.id, path)
            ?: return LaptopLookupResult(LookupKind.WORKSPACE_READ.id, listOf(path), "file not found: $path", REV, LookupTrust.PROJECT_DATA, false)
        val (bounded, cut) = bound(content, CONTENT_CAP)
        return LaptopLookupResult(LookupKind.WORKSPACE_READ.id, listOf("${ws.id}/$path"), bounded, REV, LookupTrust.PROJECT_DATA, cut)
    }

    private fun creationCurrent(workspaceId: String?, limit: Int): LaptopLookupResult {
        val workspace = core.state.value.workspaces.firstOrNull { it.id == workspaceId }
        val plan = workspace?.creation
            ?: return LaptopLookupResult(LookupKind.CREATION_CURRENT.id, emptyList(), "no creation plan in the pinned workspace", REV, LookupTrust.PROJECT_DATA, false)
        val shots = plan.shots()
        val summary = buildString {
            append("creation: ${workspace.name} (${workspace.id}) · ${plan.kind}\n")
            append("synopsis: ").append(plan.synopsis.take(400)).append('\n')
            append("hierarchy: ${plan.seasons.size} seasons, ${plan.episodes().size} episodes, ${plan.scenes().size} scenes, ${shots.size} shots\n")
            shots.take(limit).forEach { append("shot ${it.id}: ").append(it.prompt.take(100)).append('\n') }
            append("characters: ").append(plan.characters.take(limit).joinToString { it.name }).append('\n')
            append("asset metadata: ").append(plan.assets.take(limit).joinToString { "${it.name} [${it.rights}]" }).append('\n')
            append("PLANNING_ONLY; renderers unconfigured; no generated media")
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.CREATION_CURRENT.id, listOf(workspace.id) + shots.take(limit).map { it.id }, bounded, REV, LookupTrust.PROJECT_DATA, cut || shots.size > limit)
    }

    private fun taskCurrent(workspaceId: String?): LaptopLookupResult {
        val task = core.state.value.tasks.filter { it.workspaceId == workspaceId }.maxByOrNull { it.updatedAtMs }
            ?: return LaptopLookupResult(LookupKind.TASK_CURRENT.id, emptyList(), "no tasks recorded", REV, LookupTrust.TRUSTED_SYSTEM, false)
        val summary = buildString {
            append("task ").append(task.id).append(" [").append(task.status).append("]")
            append("\nobjective: ").append(task.userObjective)
            task.capabilityId?.let { append("\ncapability: ").append(it) }
            append("\ncheckpoints: ").append(task.checkpointIds.size).append(" · evidence: ").append(task.evidenceRefs.size)
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.TASK_CURRENT.id, listOf(task.id), bounded, REV, LookupTrust.TRUSTED_SYSTEM, cut)
    }

    private fun capabilityFind(query: String, limit: Int): LaptopLookupResult {
        val words = query.lowercase().split(Regex("[^a-z0-9.@]+")).filter { it.length > 2 }
        val matches = CapabilityRegistry.SEED.filter { cap ->
            if (words.isEmpty()) return@filter true
            val hay = (cap.id + " " + cap.description).lowercase()
            words.any { hay.contains(it) }
        }.take(limit)
        if (matches.isEmpty()) {
            return LaptopLookupResult(LookupKind.CAPABILITY_FIND.id, emptyList(), "no capability matched '$query'", REV, LookupTrust.TRUSTED_SYSTEM, false)
        }
        val summary = matches.joinToString("\n") { cap ->
            val ref = CapabilityRegistry.ref(cap)
            val granted = core.hasPermission(ref)
            "$ref — ${cap.description} [permission: ${if (granted) "granted" else "not granted"}]"
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.CAPABILITY_FIND.id, matches.map { CapabilityRegistry.ref(it) }, bounded, REV, LookupTrust.TRUSTED_SYSTEM, matches.size >= limit || cut)
    }

    private fun modelFind(query: String, limit: Int): LaptopLookupResult {
        val words = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }
        val dossiers = modelDossiers()
        val matches = dossiers.filter { d ->
            if (words.isEmpty()) return@filter true
            val hay = (d.name + " " + d.roles.joinToString(" ") + " " + (d.architecture ?: "")).lowercase()
            words.any { hay.contains(it) }
        }.take(limit)
        if (matches.isEmpty()) {
            return LaptopLookupResult(LookupKind.MODEL_FIND.id, emptyList(), "no installed model matched '$query'", REV, LookupTrust.TRUSTED_SYSTEM, false)
        }
        val summary = matches.joinToString("\n") { d ->
            val rag = d.ragStatus?.let { " · RAG: $it" } ?: ""
            "${d.name} — roles: ${d.roles.joinToString(",")} · admissions: ${d.admissions.joinToString(",").ifEmpty { "none" }}$rag · arch: ${d.architecture ?: "unknown"}"
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.MODEL_FIND.id, matches.map { it.id }, bounded, REV, LookupTrust.TRUSTED_SYSTEM, matches.size >= limit || cut)
    }

    private fun knowledgeFind(query: String, limit: Int): LaptopLookupResult {
        val words = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }
        val matches = KnowledgeRegistry.SEEDS.filter { k ->
            if (words.isEmpty()) return@filter true
            val hay = (k.id + " " + k.title + " " + k.text).lowercase()
            words.any { hay.contains(it) }
        }.take(limit)
        if (matches.isEmpty()) {
            return LaptopLookupResult(LookupKind.KNOWLEDGE_FIND.id, emptyList(), "no knowledge matched '$query'", REV, LookupTrust.TRUSTED_SYSTEM, false)
        }
        val summary = matches.joinToString("\n") { "${it.id} — ${it.title}: ${it.text.take(160)}" }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.KNOWLEDGE_FIND.id, matches.map { it.id }, bounded, REV, LookupTrust.TRUSTED_SYSTEM, cut)
    }

    private fun procedureFind(query: String, limit: Int): LaptopLookupResult {
        val words = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }
        val matches = ProcedureRegistry.SEEDS.filter { p ->
            if (words.isEmpty()) return@filter true
            val hay = (p.id + " " + p.title).lowercase()
            words.any { hay.contains(it) }
        }.take(limit)
        if (matches.isEmpty()) {
            return LaptopLookupResult(LookupKind.PROCEDURE_FIND.id, emptyList(), "no procedure matched '$query'", REV, LookupTrust.TRUSTED_SYSTEM, false)
        }
        val summary = matches.joinToString("\n") { "${it.id} — ${it.title} (${it.steps.size} steps)" }
        return LaptopLookupResult(LookupKind.PROCEDURE_FIND.id, matches.map { it.id }, summary, REV, LookupTrust.TRUSTED_SYSTEM, false)
    }

    private fun effectDescribe(query: String): LaptopLookupResult {
        val id = query.trim()
        val tool = ToolRegistry.SEEDS.firstOrNull { it.id == id }
            ?: return LaptopLookupResult(LookupKind.EFFECT_DESCRIBE.id, emptyList(), "no effect named '$id'", REV, LookupTrust.TRUSTED_SYSTEM, false)
        val caps = CapabilityRegistry.SEED.filter { tool.id in it.requiredEffects }
        val summary = buildString {
            append(tool.id).append(" [").append(tool.kind).append("] — ").append(tool.title)
            append("\nstatus: ").append(tool.status)
            if (tool.note.isNotBlank()) append("\nnote: ").append(tool.note)
            if (caps.isNotEmpty()) {
                append("\ncapabilities: ").append(caps.joinToString(", ") { CapabilityRegistry.ref(it) })
                append("\npermissions: ").append(
                    caps.joinToString(", ") { ref ->
                        val r = CapabilityRegistry.ref(ref)
                        "$r=${if (core.hasPermission(r)) "granted" else "not granted"}"
                    },
                )
            }
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(LookupKind.EFFECT_DESCRIBE.id, listOf(tool.id), bounded, REV, LookupTrust.TRUSTED_SYSTEM, cut)
    }

    private fun evidenceFind(query: String, limit: Int, workspaceId: String?): LaptopLookupResult {
        val st = core.state.value
        val taskIds = st.tasks.filter { it.workspaceId == workspaceId }.map { it.id }.toSet()
        val scopedEvidence = st.evidence.filter { evidence ->
            if (evidence.taskId != null) evidence.taskId in taskIds
            else if (workspaceId == null) evidence.refs.none { it.startsWith("workspace:") }
            else "workspace:$workspaceId" in evidence.refs
        }
        val evidenceIds = scopedEvidence.map { it.id }.toSet()
        val ev = scopedEvidence.takeLast(limit).reversed()
        val vf = st.verifications.filter { verification ->
            if (verification.taskId != null) verification.taskId in taskIds
            else verification.evidenceId in evidenceIds
        }.takeLast(limit).reversed()
        if (ev.isEmpty() && vf.isEmpty()) {
            return LaptopLookupResult(LookupKind.EVIDENCE_FIND.id, emptyList(), "no evidence recorded", REV, LookupTrust.PROJECT_DATA, false)
        }
        val summary = buildString {
            vf.forEach { append("[${it.verdict}] ").append(it.requestedAction.take(120)).append('\n') }
            ev.forEach { append("[${it.kind}] ").append(it.summary.take(120)).append('\n') }
        }
        val (bounded, cut) = bound(summary)
        return LaptopLookupResult(
            LookupKind.EVIDENCE_FIND.id,
            (vf.map { it.id } + ev.map { it.id }).take(limit),
            bounded,
            REV,
            LookupTrust.PROJECT_DATA,
            cut,
        )
    }

    private companion object {
        const val REV = "cipher-laptop/1"
        const val SUMMARY_CAP = 1200
        const val CONTENT_CAP = 2000
    }
}
