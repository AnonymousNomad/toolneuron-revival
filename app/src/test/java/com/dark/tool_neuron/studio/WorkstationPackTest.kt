package com.dark.tool_neuron.studio

import com.dark.tool_neuron.laptop.CapabilityRegistry
import com.dark.tool_neuron.laptop.EdgeHarnessBatteries
import org.junit.Assert.*
import org.junit.Test

class WorkstationPackTest {
    @Test fun packReferencesResolveWithoutGrantingOrQualifyingAnything() {
        for (pack in WorkstationPacks.all) {
            assertTrue(pack.capabilities.all { CapabilityRegistry.lookup(it) != null })
            assertTrue(pack.templates.all { id -> VibeTemplates.all.any { it.id == id } })
            assertTrue(pack.batteries.all { id -> EdgeHarnessBatteries.all.any { it.battery.id == id } })
        }
    }
    @Test fun batteryChecksAcceptOnlyTheirDeclaredDeterministicOutput() {
        for (battery in EdgeHarnessBatteries.all) {
            assertEquals(battery.battery.checkIds, battery.checks.map { it.id }.toSet())
            for (check in battery.checks) {
                assertTrue(check.accepts(check.expected))
                assertFalse(check.accepts("I think this passes"))
                assertFalse(check.accepts("x".repeat(9000)))
            }
        }
    }
}
