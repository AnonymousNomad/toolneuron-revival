package com.dark.tool_neuron.laptop

import com.dark.tool_neuron.laptop.resident.ResidentState
import kotlinx.serialization.Serializable

const val LAPTOP_FORMAT = "cipher-laptop/1"

@Serializable
data class CipherLaptop(
    val format: String = LAPTOP_FORMAT,
    val session: SessionState = SessionState(),
    val workspaces: List<WorkspaceRef> = emptyList(),
    val activeWorkspaceId: String? = null,
    val tasks: List<TaskRecord> = emptyList(),
    val checkpoints: List<CheckpointRef> = emptyList(),
    val evidence: List<EvidenceRecord> = emptyList(),
    val verifications: List<VerificationRecord> = emptyList(),
    val knowledge: List<KnowledgeRef> = emptyList(),
    val procedures: List<ProcedureRef> = emptyList(),
    val permissions: List<PermissionGrant> = emptyList(),
    val resident: ResidentState = ResidentState(),
    val workstation: WorkstationSession = WorkstationSession(),
    val cipherAssignment: CipherAssignment? = null,
    val modelLabRuns: List<ModelLabRun> = emptyList(),
    val qualifications: List<QualificationRecord> = emptyList(),
    val vibeBuilds: List<com.dark.tool_neuron.studio.VibeBuildRecord> = emptyList(),
)

@Serializable
data class SessionState(
    val createdAtMs: Long = 0,
    val lastOpenMs: Long = 0,
    val openCount: Long = 0,
    val recoveredFromCorruption: Boolean = false,
    val recoveryReason: String? = null,
    val recoveredAtMs: Long? = null,
)

@Serializable
data class WorkspaceRef(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val metadata: Map<String, String> = emptyMap(),
    val conversationIds: List<String> = emptyList(),
    val creation: com.dark.tool_neuron.creation.CreationPlan? = null,
    val companion: com.dark.tool_neuron.companion.CompanionPlan? = null,
)

@Serializable
enum class TaskStatus { CREATED, PLANNED, EXECUTING, VERIFYING, SUCCEEDED, FAILED, CANCELLED }

@Serializable
data class TaskRecord(
    val id: String,
    val userObjective: String,
    val capabilityId: String?,
    val status: TaskStatus,
    val currentStep: Int = 0,
    val contextRefs: List<String> = emptyList(),
    val worker: String? = null,
    val provider: String? = null,
    val permissions: List<String> = emptyList(),
    val checkpointIds: List<String> = emptyList(),
    val evidenceRefs: List<String> = emptyList(),
    val result: String? = null,
    val failureReason: String? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val workspaceId: String? = null,
)

@Serializable
data class CheckpointRef(
    val id: String,
    val taskId: String,
    val label: String,
    val step: Int,
    val createdAtMs: Long,
    val refs: List<String> = emptyList(),
)

@Serializable
enum class EvidenceKind { NOTE, FILE_STATE, COMMAND_OUTPUT, CHECK, VERIFICATION }

@Serializable
data class EvidenceRecord(
    val id: String,
    val taskId: String?,
    val kind: EvidenceKind,
    val summary: String,
    val refs: List<String> = emptyList(),
    val sha256: String? = null,
    val createdAtMs: Long,
)

@Serializable
enum class Verdict { PENDING, PASS, FAIL }

@Serializable
data class VerificationRecord(
    val id: String,
    val taskId: String?,
    val requestedAction: String,
    val expectedResult: String,
    val verificationMethod: String,
    val evidenceId: String?,
    val verdict: Verdict,
    val createdAtMs: Long,
)

@Serializable
enum class KnowledgeKind { SKILL, SOP, DOC, DOSSIER }

@Serializable
data class KnowledgeRef(
    val id: String,
    val kind: KnowledgeKind,
    val title: String,
    val text: String,
    val ref: String? = null,
)

@Serializable
data class ProcedureRef(
    val id: String,
    val title: String,
    val steps: List<String>,
)

@Serializable
data class PermissionGrant(
    val capabilityId: String,
    val granted: Boolean,
    val changedAtMs: Long,
)

@Serializable
enum class ToolKind { TERMINAL, FILESYSTEM, GIT, MCP, PLUGIN }

@Serializable
enum class ToolStatus { AVAILABLE, STUB, PLANNED }

@Serializable
data class ToolRef(
    val id: String,
    val kind: ToolKind,
    val title: String,
    val status: ToolStatus,
    val note: String = "",
)
