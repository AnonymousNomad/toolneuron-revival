package com.dark.tool_neuron.laptop

const val WORKER_PACKET_FORMAT = "worker-packet/1"

sealed class PacketError(message: String) : Exception(message) {
    class UnresolvedReference(val kind: String, val ref: String) : PacketError("unresolved $kind reference: $ref")
    class EmptyRequiredSection(val section: String) : PacketError("required section empty: $section")
    class OverBudget(val bytes: Int, val budget: Int) : PacketError("packet over budget after reduction: $bytes > $budget")
}

data class PacketTaskIdentity(
    val taskId: String,
    val objective: String,
    val capabilityRef: String?,
)

data class WorkerPacketSpec(
    val governingRules: List<String>,
    val taskIdentity: PacketTaskIdentity,
    val facts: List<String> = emptyList(),
    val procedureRef: String? = null,
    val effectRefs: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val acceptanceCriteria: List<String>,
    val requiredEvidence: List<String>,
    val knowledgeRefs: List<String> = emptyList(),
    val previousResult: String? = null,
    val budgetBytes: Int = 8192,
)

interface PacketResolver {
    fun resolveProcedure(ref: String): ProcedureRef?
    fun resolveKnowledge(ref: String): KnowledgeRef?
    fun resolveEffect(ref: String): ToolRef?
}

data class WorkerPacket(
    val format: String = WORKER_PACKET_FORMAT,
    val text: String,
    val bytes: Int,
    val droppedSections: List<String>,
)

object WorkerPacketBuilder {
    private val DROP_ORDER = listOf(SECTION_PREVIOUS, SECTION_REFS, SECTION_CONSTRAINTS, SECTION_FACTS, SECTION_EFFECTS, SECTION_PROCEDURE)

    fun build(spec: WorkerPacketSpec, resolver: PacketResolver): WorkerPacket {
        val procedure = spec.procedureRef?.let {
            resolver.resolveProcedure(it) ?: throw PacketError.UnresolvedReference("procedure", it)
        }
        val knowledge = spec.knowledgeRefs.map {
            resolver.resolveKnowledge(it) ?: throw PacketError.UnresolvedReference("knowledge", it)
        }
        val effects = spec.effectRefs.map {
            resolver.resolveEffect(it) ?: throw PacketError.UnresolvedReference("effect", it)
        }

        if (spec.taskIdentity.taskId.isBlank() || spec.taskIdentity.objective.isBlank()) {
            throw PacketError.EmptyRequiredSection(SECTION_IDENTITY)
        }
        if (spec.governingRules.isEmpty()) throw PacketError.EmptyRequiredSection(SECTION_RULES)
        if (spec.acceptanceCriteria.isEmpty()) throw PacketError.EmptyRequiredSection(SECTION_ACCEPTANCE)
        if (spec.requiredEvidence.isEmpty()) throw PacketError.EmptyRequiredSection(SECTION_EVIDENCE)

        val dropped = mutableListOf<String>()
        var text = render(spec, procedure, knowledge, effects, dropped.toSet())
        for (section in DROP_ORDER) {
            if (size(text) <= spec.budgetBytes) break
            dropped += section
            text = render(spec, procedure, knowledge, effects, dropped.toSet())
        }
        val bytes = size(text)
        if (bytes > spec.budgetBytes) throw PacketError.OverBudget(bytes, spec.budgetBytes)
        return WorkerPacket(text = text, bytes = bytes, droppedSections = dropped)
    }

    private fun size(text: String): Int = text.toByteArray(Charsets.UTF_8).size

    private fun render(
        spec: WorkerPacketSpec,
        procedure: ProcedureRef?,
        knowledge: List<KnowledgeRef>,
        effects: List<ToolRef>,
        dropped: Set<String>,
    ): String {
        val sb = StringBuilder()
        sb.append("# 1. GOVERNING RULES\n")
        spec.governingRules.forEach { sb.append("- ").append(it).append('\n') }

        sb.append("# 2. TASK IDENTITY\n")
        sb.append("id: ").append(spec.taskIdentity.taskId).append('\n')
        sb.append("objective: ").append(spec.taskIdentity.objective).append('\n')
        spec.taskIdentity.capabilityRef?.let { sb.append("capability: ").append(it).append('\n') }

        if (SECTION_FACTS !in dropped && spec.facts.isNotEmpty()) {
            sb.append("# 3. CURRENT FACTS\n")
            spec.facts.forEach { sb.append("- ").append(it).append('\n') }
        }
        if (SECTION_PROCEDURE !in dropped && procedure != null) {
            sb.append("# 4. PROCEDURE\n")
            sb.append("procedure: ").append(procedure.id).append(" (").append(procedure.title).append(")\n")
            procedure.steps.forEachIndexed { i, step -> sb.append("  ").append(i + 1).append(". ").append(step).append('\n') }
        }
        if (SECTION_EFFECTS !in dropped && effects.isNotEmpty()) {
            sb.append("# 5. AVAILABLE EFFECTS\n")
            effects.forEach { sb.append("- ").append(it.id).append(" [").append(it.kind).append("]: ").append(it.title).append('\n') }
        }
        if (SECTION_CONSTRAINTS !in dropped && spec.constraints.isNotEmpty()) {
            sb.append("# 6. CONSTRAINTS\n")
            spec.constraints.forEach { sb.append("- ").append(it).append('\n') }
        }
        sb.append("# 7. ACCEPTANCE CRITERIA\n")
        spec.acceptanceCriteria.forEach { sb.append("- ").append(it).append('\n') }
        sb.append("# 8. REQUIRED EVIDENCE\n")
        spec.requiredEvidence.forEach { sb.append("- ").append(it).append('\n') }
        if (SECTION_REFS !in dropped && knowledge.isNotEmpty()) {
            sb.append("# 9. TASK-SPECIFIC REFERENCES\n")
            knowledge.forEach { sb.append("- ").append(it.id).append(": ").append(it.text).append('\n') }
        }
        if (SECTION_PREVIOUS !in dropped && !spec.previousResult.isNullOrBlank()) {
            sb.append("# 10. PREVIOUS RELEVANT RESULT\n")
            sb.append(spec.previousResult).append('\n')
        }
        return sb.toString()
    }

    private const val SECTION_RULES = "1"
    private const val SECTION_IDENTITY = "2"
    private const val SECTION_FACTS = "3"
    private const val SECTION_PROCEDURE = "4"
    private const val SECTION_EFFECTS = "5"
    private const val SECTION_CONSTRAINTS = "6"
    private const val SECTION_ACCEPTANCE = "7"
    private const val SECTION_EVIDENCE = "8"
    private const val SECTION_REFS = "9"
    private const val SECTION_PREVIOUS = "10"
}
