package com.dark.tool_neuron

import com.dark.tool_neuron.reporting.*
import org.junit.Assert.*
import org.junit.Test
import java.util.zip.ZipInputStream

class BetaReportTest {
    private fun manifest() = BetaReportManifest("test", 40, "test-device", "35", listOf("arm64-v8a"), emptyList(), "Tap Models, import, then reopen", listOf("native=BLOCKED_LOCAL_SOURCE_UNAVAILABLE"), 0)
    @Test fun reportBundleIsBoundedDeterministicAndContainsOnlyReviewedInputs() {
        val report = manifest()
        val first = BetaReports.bundle(report, null)
        assertArrayEquals(first, BetaReports.bundle(report, null))
        val entries = mutableMapOf<String, String>()
        ZipInputStream(first.inputStream()).use { zip -> while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8) } }
        assertEquals(setOf("report.json"), entries.keys)
        assertTrue(entries.getValue("report.json").contains("Tap Models"))
        assertFalse(entries.getValue("report.json").contains("content://"))
        assertTrue(runCatching { BetaReports.bundle(report.copy(reproduction = "x".repeat(8001)), null) }.isFailure)
        assertTrue(runCatching { BetaReports.bundle(report, ByteArray(4 * 1024 * 1024 + 1)) }.isFailure)
    }
    @Test fun optionalScreenshotRequiresExplicitBytesAndHasNoPathMetadata() {
        val bytes = byteArrayOf(1, 2, 3)
        val zip = ZipInputStream(BetaReports.bundle(manifest(), bytes).inputStream())
        val names = mutableListOf<String>()
        zip.use { while (true) { val entry = it.nextEntry ?: break; names += entry.name } }
        assertEquals(listOf("report.json", "screenshot.bin"), names)
    }
}
