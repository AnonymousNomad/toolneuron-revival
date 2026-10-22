package com.dark.tool_neuron.laptop

import kotlinx.serialization.json.Json

class LaptopStateError(message: String) : Exception(message)

object LaptopCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(state: CipherLaptop): ByteArray = json.encodeToString(CipherLaptop.serializer(), state).toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): CipherLaptop {
        val text = bytes.toString(Charsets.UTF_8)
        if (text.isBlank()) throw LaptopStateError("empty state payload")
        val state = try {
            json.decodeFromString(CipherLaptop.serializer(), text)
        } catch (e: Exception) {
            throw LaptopStateError("state parse failed: ${e.message}")
        }
        validate(state)
        return state
    }

    fun validate(state: CipherLaptop) {
        if (state.format != LAPTOP_FORMAT) {
            throw LaptopStateError("format mismatch: ${state.format} != $LAPTOP_FORMAT")
        }
        val wsIds = state.workspaces.map { it.id }
        if (wsIds.size != wsIds.toSet().size) throw LaptopStateError("duplicate workspace id")
        if (wsIds.any { it.isBlank() }) throw LaptopStateError("blank workspace id")
        state.activeWorkspaceId?.let { active ->
            if (active !in wsIds) throw LaptopStateError("active workspace missing: $active")
        }
        state.workspaces.forEach { workspace ->
            try { workspace.companion?.validate() } catch (_: Exception) { throw LaptopStateError("invalid companion plan") }
            try { workspace.creation?.validate() } catch (_: Exception) { throw LaptopStateError("invalid creation plan") }
        }
        val conversations = state.workspaces.flatMap { it.conversationIds }
        if (conversations.any { it.isBlank() || it.length > 256 } || conversations.distinct().size != conversations.size) {
            throw LaptopStateError("invalid or duplicate conversation binding")
        }
        val buildIds = state.vibeBuilds.map { it.id }
        if (buildIds.distinct().size != buildIds.size) throw LaptopStateError("duplicate Vibe build id")
        state.vibeBuilds.forEach { build ->
            if (build.workspaceId !in wsIds || state.evidence.none { it.id == build.evidenceId }) throw LaptopStateError("Vibe build references missing workspace/evidence")
            if (!build.id.matches(Regex("vb-[a-f0-9]{32}")) || !build.digest.matches(Regex("[a-f0-9]{64}"))) throw LaptopStateError("invalid Vibe build identity")
        }
        val taskIds = state.tasks.map { it.id }
        if (taskIds.size != taskIds.toSet().size) throw LaptopStateError("duplicate task id")
        val taskIdSet = taskIds.toSet()
        state.tasks.forEach { t ->
            if (t.workspaceId != null && t.workspaceId !in wsIds) throw LaptopStateError("task references missing workspace")
            if (t.checkpointIds.any { ref -> state.checkpoints.none { it.id == ref } }) {
                throw LaptopStateError("task ${t.id} references missing checkpoint")
            }
            if (t.evidenceRefs.any { ref -> state.evidence.none { it.id == ref } }) {
                throw LaptopStateError("task ${t.id} references missing evidence")
            }
        }
        state.checkpoints.forEach { cp ->
            if (cp.taskId !in taskIdSet) throw LaptopStateError("checkpoint ${cp.id} references missing task")
        }
        state.evidence.forEach { ev ->
            ev.taskId?.let { if (it !in taskIdSet) throw LaptopStateError("evidence ${ev.id} references missing task") }
        }
        state.verifications.forEach { v ->
            v.taskId?.let { if (it !in taskIdSet) throw LaptopStateError("verification ${v.id} references missing task") }
            v.evidenceId?.let { evId ->
                if (state.evidence.none { e -> e.id == evId }) throw LaptopStateError("verification ${v.id} references missing evidence")
            }
        }
        val evIds = state.evidence.map { it.id }
        if (evIds.size != evIds.toSet().size) throw LaptopStateError("duplicate evidence id")
        if (state.modelLabRuns.map { it.id }.distinct().size != state.modelLabRuns.size) throw LaptopStateError("duplicate Model Lab run")
        state.modelLabRuns.forEach { run ->
            try { run.validateHistorical() } catch (_: Exception) { throw LaptopStateError("invalid Model Lab run") }
        }
        state.qualifications.forEach { record ->
            if (record.recordedAtMs <= 0 || record.checks.isEmpty() || record.checks.map { it.id }.distinct().size != record.checks.size)
                throw LaptopStateError("invalid qualification record")
            if (record.checks.any { check -> check.evidenceRefs.isEmpty() || check.evidenceRefs.any { it !in evIds } })
                throw LaptopStateError("qualification references missing evidence")
        }
        val cpIds = state.checkpoints.map { it.id }
        if (cpIds.size != cpIds.toSet().size) throw LaptopStateError("duplicate checkpoint id")
        state.resident.activeSessionId?.let { active ->
            if (state.resident.sessions.none { it.id == active }) {
                throw LaptopStateError("active resident session missing: $active")
            }
        }
        if (state.resident.sessions.size > 10) throw LaptopStateError("resident session history exceeds cap")
    }
}
