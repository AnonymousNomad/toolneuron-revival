package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.KnowledgeKind
import com.dark.tool_neuron.laptop.KnowledgeRef
import com.dark.tool_neuron.laptop.PacketError
import com.dark.tool_neuron.laptop.PacketResolver
import com.dark.tool_neuron.laptop.PacketTaskIdentity
import com.dark.tool_neuron.laptop.ProcedureRef
import com.dark.tool_neuron.laptop.ToolKind
import com.dark.tool_neuron.laptop.ToolRef
import com.dark.tool_neuron.laptop.ToolStatus
import com.dark.tool_neuron.laptop.WorkerPacketBuilder
import com.dark.tool_neuron.laptop.WorkerPacketSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapResolver(
    private val procedures: Map<String, ProcedureRef> = emptyMap(),
    private val knowledge: Map<String, KnowledgeRef> = emptyMap(),
    private val effects: Map<String, ToolRef> = emptyMap(),
) : PacketResolver {
    override fun resolveProcedure(ref: String): ProcedureRef? = procedures[ref]
    override fun resolveKnowledge(ref: String): KnowledgeRef? = knowledge[ref]
    override fun resolveEffect(ref: String): ToolRef? = effects[ref]
}

class WorkerPacketTest {
    private val proc = ProcedureRef("P.test", "Test procedure", listOf("step one", "step two"))
    private val know1 = KnowledgeRef("K.one", KnowledgeKind.SOP, "One", "knowledge one text")
    private val know2 = KnowledgeRef("K.two", KnowledgeKind.SOP, "Two", "knowledge two text")
    private val effect = ToolRef("filesystem.workspace", ToolKind.FILESYSTEM, "Workspace files", ToolStatus.AVAILABLE)

    private fun resolver() = MapResolver(
        procedures = mapOf(proc.id to proc),
        knowledge = mapOf(know1.id to know1, know2.id to know2),
        effects = mapOf(effect.id to effect),
    )

    private fun fullSpec(budget: Int = 8192) = WorkerPacketSpec(
        governingRules = listOf("Never execute unbounded shell text", "Record evidence for every effect"),
        taskIdentity = PacketTaskIdentity("task-1", "inspect the workspace", "dev.workspace.inspect@1"),
        facts = listOf("workspace ws-1 active", "one file present"),
        procedureRef = proc.id,
        effectRefs = listOf(effect.id),
        constraints = listOf("stay inside the workspace"),
        acceptanceCriteria = listOf("file listing matches directory state"),
        requiredEvidence = listOf("recorded listing output"),
        knowledgeRefs = listOf(know1.id),
        previousResult = "previous turn produced nothing",
        budgetBytes = budget,
    )

    @Test
    fun deterministicOutput() {
        val a = WorkerPacketBuilder.build(fullSpec(), resolver())
        val b = WorkerPacketBuilder.build(fullSpec(), resolver())
        assertEquals(a.text, b.text)
        assertEquals(a.bytes, b.bytes)
        assertTrue(a.droppedSections.isEmpty())
    }

    @Test
    fun hierarchyOrderIsFixed() {
        val packet = WorkerPacketBuilder.build(fullSpec(), resolver())
        val order = listOf(
            "# 1. GOVERNING RULES",
            "# 2. TASK IDENTITY",
            "# 3. CURRENT FACTS",
            "# 4. PROCEDURE",
            "# 5. AVAILABLE EFFECTS",
            "# 6. CONSTRAINTS",
            "# 7. ACCEPTANCE CRITERIA",
            "# 8. REQUIRED EVIDENCE",
            "# 9. TASK-SPECIFIC REFERENCES",
            "# 10. PREVIOUS RELEVANT RESULT",
        )
        var last = -1
        order.forEach { header ->
            val idx = packet.text.indexOf(header)
            assertTrue("missing $header", idx >= 0)
            assertTrue("$header out of order", idx > last)
            last = idx
        }
    }

    @Test
    fun irrelevantContextIsExcluded() {
        val spec = fullSpec().copy(knowledgeRefs = listOf(know1.id))
        val packet = WorkerPacketBuilder.build(spec, resolver())
        assertTrue(packet.text.contains("knowledge one text"))
        assertTrue(!packet.text.contains("knowledge two text"))
    }

    @Test
    fun hardSectionsSurviveBudgetReduction() {
        val full = WorkerPacketBuilder.build(fullSpec(), resolver())
        val packet = WorkerPacketBuilder.build(fullSpec(budget = full.bytes - 1), resolver())
        assertTrue(packet.droppedSections.isNotEmpty())
        assertTrue(packet.text.contains("# 1. GOVERNING RULES"))
        assertTrue(packet.text.contains("Never execute unbounded shell text"))
        assertTrue(packet.text.contains("# 2. TASK IDENTITY"))
        assertTrue(packet.text.contains("# 7. ACCEPTANCE CRITERIA"))
        assertTrue(packet.text.contains("# 8. REQUIRED EVIDENCE"))
        assertTrue(packet.bytes <= full.bytes - 1)
    }

    @Test
    fun optionalSectionsDroppedLowestPriorityFirst() {
        val full = WorkerPacketBuilder.build(fullSpec(), resolver())
        val packet = WorkerPacketBuilder.build(fullSpec(budget = full.bytes - 1), resolver())
        assertEquals(listOf("10"), packet.droppedSections)
    }

    @Test
    fun overBudgetFailsTyped() {
        val e = runCatching { WorkerPacketBuilder.build(fullSpec(budget = 40), resolver()) }.exceptionOrNull()
        assertTrue(e is PacketError.OverBudget)
    }

    @Test
    fun unresolvedReferenceFailsTyped() {
        val e1 = runCatching {
            WorkerPacketBuilder.build(fullSpec().copy(procedureRef = "P.missing"), resolver())
        }.exceptionOrNull()
        assertTrue(e1 is PacketError.UnresolvedReference)

        val e2 = runCatching {
            WorkerPacketBuilder.build(fullSpec().copy(knowledgeRefs = listOf("K.missing")), resolver())
        }.exceptionOrNull()
        assertTrue(e2 is PacketError.UnresolvedReference)

        val e3 = runCatching {
            WorkerPacketBuilder.build(fullSpec().copy(effectRefs = listOf("effect.missing")), resolver())
        }.exceptionOrNull()
        assertTrue(e3 is PacketError.UnresolvedReference)
    }

    @Test
    fun emptyRequiredSectionsFailTyped() {
        val e1 = runCatching { WorkerPacketBuilder.build(fullSpec().copy(governingRules = emptyList()), resolver()) }.exceptionOrNull()
        assertTrue(e1 is PacketError.EmptyRequiredSection)
        val e2 = runCatching { WorkerPacketBuilder.build(fullSpec().copy(acceptanceCriteria = emptyList()), resolver()) }.exceptionOrNull()
        assertTrue(e2 is PacketError.EmptyRequiredSection)
        val e3 = runCatching { WorkerPacketBuilder.build(fullSpec().copy(requiredEvidence = emptyList()), resolver()) }.exceptionOrNull()
        assertTrue(e3 is PacketError.EmptyRequiredSection)
        val e4 = runCatching {
            WorkerPacketBuilder.build(fullSpec().copy(taskIdentity = PacketTaskIdentity("", "x", null)), resolver())
        }.exceptionOrNull()
        assertTrue(e4 is PacketError.EmptyRequiredSection)
    }
}
