package com.dark.tool_neuron.laptop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class LaptopCore(
    private val persistence: LaptopPersistence,
    private val workspacesBaseDir: File,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<CipherLaptop> = _state.asStateFlow()

    private fun loadInitial(): CipherLaptop {
        val raw = runCatching { persistence.load() }.getOrNull()
        if (raw == null) {
            val fresh = seedFresh()
            persist(fresh)
            return fresh
        }
        return try {
            val loaded = LaptopCodec.decode(raw)
            val bumped = loaded.copy(
                session = loaded.session.copy(lastOpenMs = nowMs(), openCount = loaded.session.openCount + 1),
            )
            persist(bumped)
            bumped
        } catch (e: LaptopStateError) {
            runCatching { persistence.quarantineCorrupt(raw) }
            val recovered = seedFresh().copy(
                session = SessionState(
                    createdAtMs = nowMs(),
                    lastOpenMs = nowMs(),
                    openCount = 1,
                    recoveredFromCorruption = true,
                    recoveryReason = e.message,
                    recoveredAtMs = nowMs(),
                ),
            )
            persist(recovered)
            recovered
        }
    }

    private fun seedFresh(): CipherLaptop = CipherLaptop(
        session = SessionState(createdAtMs = nowMs(), lastOpenMs = nowMs(), openCount = 1),
        knowledge = KnowledgeRegistry.SEEDS,
        procedures = ProcedureRegistry.SEEDS,
    )

    private fun persist(state: CipherLaptop) {
        persistence.save(LaptopCodec.encode(state))
    }

    @Synchronized
    private fun mutate(block: (CipherLaptop) -> CipherLaptop): CipherLaptop {
        val next = block(_state.value)
        LaptopCodec.validate(next)
        persist(next)
        _state.value = next
        return next
    }

    private fun shortId(prefix: String): String =
        "$prefix-" + UUID.randomUUID().toString().replace("-", "").take(10)

    private fun updateWorkstation(update: (WorkstationSession) -> WorkstationSession): Result<Unit> = runCatching {
        mutate { it.copy(workstation = update(it.workstation)) }
        Unit
    }

    fun openWorkstationApp(app: WorkstationApp): Result<Unit> = updateWorkstation { it.open(app) }

    fun minimizeWorkstationApp(app: WorkstationApp): Result<Unit> = updateWorkstation { it.minimize(app) }

    fun closeWorkstationApp(app: WorkstationApp): Result<Unit> = updateWorkstation { it.close(app) }

    fun showWorkstationDesktop(): Result<Unit> = updateWorkstation { it.desktop() }

    data class WorkspaceEntry(val path: String, val isDir: Boolean, val bytes: Long)

    fun createWorkspace(
        name: String,
        metadata: Map<String, String> = emptyMap(),
        initialFiles: Map<String, String> = emptyMap(),
    ): Result<WorkspaceRef> = runCatching {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 64) { "workspace name must be 1..64 characters" }
        val id = shortId("ws")
        val dir = File(workspacesBaseDir, id)
        if (!dir.mkdirs()) throw IllegalStateException("could not create workspace directory")
        try {
            File(dir, "README.md").writeText("# $clean\n\nToolNeuron workspace.\n")
            initialFiles.forEach { (path, text) ->
                require(!File(path).isAbsolute)
                val target = File(dir, path).canonicalFile
                require(target.toPath().startsWith(dir.canonicalFile.toPath()) && target != dir.canonicalFile)
                check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                target.writeText(text)
            }
            val ref = WorkspaceRef(id, clean, nowMs(), metadata + ("created_by" to "user"))
            mutate { it.copy(workspaces = it.workspaces + ref, activeWorkspaceId = id) }
            ref
        } catch (failure: Throwable) {
            dir.deleteRecursively()
            throw failure
        }
    }

    fun recordVibeBuild(record: com.dark.tool_neuron.studio.VibeBuildRecord): Result<com.dark.tool_neuron.studio.VibeBuildRecord> = runCatching {
        check(hasPermission(com.dark.tool_neuron.studio.VibeStudio.CAPABILITY)) { "Vibe Studio permission denied" }
        val evidence = EvidenceRecord(shortId("ev"), null, EvidenceKind.CHECK,
            "Static bundle ${record.id}: ${record.verdict}; structural checks only, browser behavior not tested",
            listOf("workspace:${record.workspaceId}", "vibe-build:${record.id}"), record.digest, nowMs())
        val saved = record.copy(evidenceId = evidence.id)
        val verification = VerificationRecord(shortId("vr"), null, "Build static web bundle ${record.id}",
            "entrypoint and local asset checks pass", "deterministic snapshot checks (not browser/device acceptance)",
            evidence.id, record.verdict, nowMs())
        mutate { state ->
            require(state.workspaces.any { it.id == record.workspaceId }) { "Unknown workspace" }
            require(state.vibeBuilds.none { it.id == record.id }) { "Duplicate build id" }
            state.copy(vibeBuilds = state.vibeBuilds + saved, evidence = state.evidence + evidence,
                verifications = state.verifications + verification)
        }
        saved
    }

    fun openWorkspace(id: String): Result<Unit> = runCatching {
        if (_state.value.workspaces.none { it.id == id }) throw IllegalArgumentException("unknown workspace $id")
        mutate { it.copy(activeWorkspaceId = id) }
        Unit
    }

    fun activeWorkspace(): WorkspaceRef? =
        _state.value.activeWorkspaceId?.let { id -> _state.value.workspaces.firstOrNull { it.id == id } }

    fun listWorkspaceFiles(): Result<List<WorkspaceEntry>> = runCatching {
        val ws = activeWorkspace() ?: throw IllegalStateException("no active workspace")
        val root = File(workspacesBaseDir, ws.id)
        val out = mutableListOf<WorkspaceEntry>()
        walk(root, root, "", 0, out)
        out.sortBy { it.path }
        out
    }

    private fun walk(root: File, dir: File, rel: String, depth: Int, out: MutableList<WorkspaceEntry>) {
        if (depth > 3 || out.size >= MAX_FILE_ENTRIES) return
        val children = dir.listFiles()?.sortedBy { it.name } ?: return
        for (f in children) {
            if (out.size >= MAX_FILE_ENTRIES) return
            val childRel = if (rel.isEmpty()) f.name else "$rel/${f.name}"
            out += WorkspaceEntry(childRel, f.isDirectory, if (f.isFile) f.length() else 0L)
            if (f.isDirectory) walk(root, f, childRel, depth + 1, out)
        }
    }

    fun createTask(objective: String, capabilityRef: String?): Result<TaskRecord> = runCatching {
        val clean = objective.trim()
        require(clean.isNotEmpty() && clean.length <= 500) { "objective must be 1..500 characters" }
        val declared = capabilityRef?.let { ref ->
            CapabilityRegistry.lookup(ref) ?: throw IllegalArgumentException("unknown capability $ref")
        }
        val now = nowMs()
        val task = TaskRecord(
            id = shortId("task"),
            userObjective = clean,
            capabilityId = capabilityRef,
            status = TaskStatus.CREATED,
            permissions = declared?.permissions ?: emptyList(),
            createdAtMs = now,
            updatedAtMs = now,
        )
        mutate { it.copy(tasks = it.tasks + task) }
        task
    }

    fun nextStatus(status: TaskStatus): TaskStatus? = when (status) {
        TaskStatus.CREATED -> TaskStatus.PLANNED
        TaskStatus.PLANNED -> TaskStatus.EXECUTING
        TaskStatus.EXECUTING -> TaskStatus.VERIFYING
        TaskStatus.VERIFYING -> TaskStatus.SUCCEEDED
        TaskStatus.SUCCEEDED, TaskStatus.FAILED, TaskStatus.CANCELLED -> null
    }

    fun allowedTransitions(from: TaskStatus): Set<TaskStatus> = when (from) {
        TaskStatus.CREATED -> setOf(TaskStatus.PLANNED, TaskStatus.CANCELLED)
        TaskStatus.PLANNED -> setOf(TaskStatus.EXECUTING, TaskStatus.CANCELLED)
        TaskStatus.EXECUTING -> setOf(TaskStatus.VERIFYING, TaskStatus.FAILED, TaskStatus.CANCELLED)
        TaskStatus.VERIFYING -> setOf(TaskStatus.SUCCEEDED, TaskStatus.FAILED)
        TaskStatus.SUCCEEDED, TaskStatus.FAILED, TaskStatus.CANCELLED -> emptySet()
    }

    fun transitionTask(taskId: String, to: TaskStatus, note: String? = null): Result<TaskRecord> = runCatching {
        val task = _state.value.tasks.firstOrNull { it.id == taskId }
            ?: throw IllegalArgumentException("unknown task $taskId")
        if (to !in allowedTransitions(task.status)) {
            throw IllegalStateException("illegal transition ${task.status} -> $to")
        }
        val updated = task.copy(
            status = to,
            currentStep = task.currentStep + 1,
            updatedAtMs = nowMs(),
            result = if (to == TaskStatus.SUCCEEDED) (note ?: task.result) else task.result,
            failureReason = if (to == TaskStatus.FAILED) (note ?: task.failureReason) else task.failureReason,
        )
        mutate { s -> s.copy(tasks = s.tasks.map { if (it.id == taskId) updated else it }) }
        updated
    }

    fun addCheckpoint(taskId: String, label: String): Result<CheckpointRef> = runCatching {
        val task = _state.value.tasks.firstOrNull { it.id == taskId }
            ?: throw IllegalArgumentException("unknown task $taskId")
        val cp = CheckpointRef(
            id = shortId("cp"),
            taskId = taskId,
            label = label.trim().ifEmpty { "checkpoint" },
            step = task.currentStep,
            createdAtMs = nowMs(),
        )
        mutate { s ->
            s.copy(
                checkpoints = s.checkpoints + cp,
                tasks = s.tasks.map { if (it.id == taskId) it.copy(checkpointIds = it.checkpointIds + cp.id) else it },
            )
        }
        cp
    }

    fun addEvidence(
        taskId: String?,
        kind: EvidenceKind,
        summary: String,
        refs: List<String> = emptyList(),
    ): Result<EvidenceRecord> = runCatching {
        taskId?.let { id -> require(_state.value.tasks.any { it.id == id }) { "unknown task $id" } }
        val ev = EvidenceRecord(
            id = shortId("ev"),
            taskId = taskId,
            kind = kind,
            summary = summary.trim().ifEmpty { "(empty)" },
            refs = refs,
            createdAtMs = nowMs(),
        )
        mutate { s ->
            s.copy(
                evidence = s.evidence + ev,
                tasks = if (taskId == null) s.tasks
                else s.tasks.map { if (it.id == taskId) it.copy(evidenceRefs = it.evidenceRefs + ev.id) else it },
            )
        }
        ev
    }

    fun addVerification(
        taskId: String?,
        requestedAction: String,
        expectedResult: String,
        verificationMethod: String,
        evidenceId: String?,
        verdict: Verdict,
    ): Result<VerificationRecord> = runCatching {
        taskId?.let { id -> require(_state.value.tasks.any { it.id == id }) { "unknown task $id" } }
        evidenceId?.let { id -> require(_state.value.evidence.any { it.id == id }) { "unknown evidence $id" } }
        val v = VerificationRecord(
            id = shortId("vf"),
            taskId = taskId,
            requestedAction = requestedAction.trim(),
            expectedResult = expectedResult.trim(),
            verificationMethod = verificationMethod.trim(),
            evidenceId = evidenceId,
            verdict = verdict,
            createdAtMs = nowMs(),
        )
        mutate { it.copy(verifications = it.verifications + v) }
        v
    }

    fun setPermission(capabilityRef: String, granted: Boolean): Result<Unit> = runCatching {
        require(CapabilityRegistry.lookup(capabilityRef) != null) { "unknown capability $capabilityRef" }
        mutate { s ->
            s.copy(permissions = s.permissions.filterNot { it.capabilityId == capabilityRef } + PermissionGrant(capabilityRef, granted, nowMs()))
        }
        Unit
    }

    fun hasPermission(capabilityRef: String): Boolean =
        _state.value.permissions.firstOrNull { it.capabilityId == capabilityRef }?.granted ?: false

    fun startResidentSession(identity: com.dark.tool_neuron.laptop.resident.ResidentIdentity, workspaceId: String?, taskId: String?): Result<com.dark.tool_neuron.laptop.resident.ResidentSessionRecord> = runCatching {
        val now = nowMs()
        val record = com.dark.tool_neuron.laptop.resident.ResidentSessionRecord(
            id = shortId("rs"),
            identity = identity,
            activeWorkspaceId = workspaceId,
            activeTaskId = taskId,
            turns = 0,
            lookupCount = 0,
            status = com.dark.tool_neuron.laptop.resident.ResidentSessionStatus.ACTIVE,
            startedAtMs = now,
            updatedAtMs = now,
        )
        mutate { s ->
            val kept = (s.resident.sessions.filter { it.status != com.dark.tool_neuron.laptop.resident.ResidentSessionStatus.ACTIVE } + record).takeLast(MAX_RESIDENT_SESSIONS)
            s.copy(resident = s.resident.copy(sessions = kept, activeSessionId = record.id))
        }
        record
    }

    fun updateResidentSession(sessionId: String, turns: Int, lookupCount: Int): Result<com.dark.tool_neuron.laptop.resident.ResidentSessionRecord> = runCatching {
        val rec = _state.value.resident.sessions.firstOrNull { it.id == sessionId }
            ?: throw IllegalArgumentException("unknown resident session $sessionId")
        val updated = rec.copy(turns = turns, lookupCount = lookupCount, updatedAtMs = nowMs())
        mutate { s -> s.copy(resident = s.resident.copy(sessions = s.resident.sessions.map { if (it.id == sessionId) updated else it })) }
        updated
    }

    fun endResidentSession(
        sessionId: String,
        status: com.dark.tool_neuron.laptop.resident.ResidentSessionStatus,
        reason: String? = null,
    ): Result<com.dark.tool_neuron.laptop.resident.ResidentSessionRecord> = runCatching {
        val rec = _state.value.resident.sessions.firstOrNull { it.id == sessionId }
            ?: throw IllegalArgumentException("unknown resident session $sessionId")
        val updated = rec.copy(status = status, statusReason = reason, updatedAtMs = nowMs(), endedAtMs = nowMs())
        mutate { s ->
            s.copy(
                resident = s.resident.copy(
                    sessions = s.resident.sessions.map { if (it.id == sessionId) updated else it },
                    activeSessionId = if (s.resident.activeSessionId == sessionId) null else s.resident.activeSessionId,
                ),
            )
        }
        updated
    }

    /** Truthful restart policy: a session still ACTIVE at boot ended with the process. */
    fun recoverResidentOnBoot(): com.dark.tool_neuron.laptop.resident.ResidentSessionRecord? {
        val activeId = _state.value.resident.activeSessionId ?: return null
        val rec = _state.value.resident.sessions.firstOrNull { it.id == activeId } ?: return null
        val updated = rec.copy(
            status = com.dark.tool_neuron.laptop.resident.ResidentSessionStatus.ENDED_PROCESS_RESTART,
            statusReason = "app process restarted",
            updatedAtMs = nowMs(),
            endedAtMs = nowMs(),
        )
        mutate { s ->
            s.copy(
                resident = s.resident.copy(
                    sessions = s.resident.sessions.map { if (it.id == activeId) updated else it },
                    activeSessionId = null,
                ),
            )
        }
        return updated
    }

    private companion object {
        const val MAX_FILE_ENTRIES = 200
        const val MAX_RESIDENT_SESSIONS = 10
    }
}
