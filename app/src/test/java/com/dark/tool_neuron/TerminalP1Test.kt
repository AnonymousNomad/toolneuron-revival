package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.TerminalResult
import com.dark.tool_neuron.laptop.terminal.TerminalAvailability
import com.dark.tool_neuron.laptop.terminal.TerminalOutput
import com.dark.tool_neuron.laptop.terminal.TerminalPolicy
import com.dark.tool_neuron.laptop.terminal.TerminalProviderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TerminalP1Test {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun sanitizeStripsAnsiAndNormalizesNewlines() {
        val bytes = "hello \u001B[31mred\u001B[0m\r\nworld\rtail".toByteArray()
        assertEquals("hello red\nworld\ntail", TerminalOutput.sanitize(bytes))
    }

    @Test
    fun sanitizeDropsControlCharsKeepsTabs() {
        val bytes = "a\u0007b\tc\u0000d".toByteArray()
        assertEquals("ab\tcd", TerminalOutput.sanitize(bytes))
    }

    @Test
    fun boundTruncatesExplicitly() {
        val data = ByteArray(100) { 'x'.code.toByte() }
        val b = TerminalOutput.bound(data, maxBytes = 40, totalBytes = 100)
        assertTrue(b.truncated)
        assertEquals(100, b.totalBytes)
        assertEquals(40, b.text.length)
        val ok = TerminalOutput.bound(ByteArray(10) { 'y'.code.toByte() }, 40)
        assertFalse(ok.truncated)
    }

    @Test
    fun boundDetectsBinary() {
        val data = ByteArray(100) { 0 }
        val b = TerminalOutput.bound(data, 100)
        assertTrue(b.binarySuspected)
        assertTrue(b.text.contains("binary output"))
    }

    @Test
    fun evidenceSummarySeparatesStreamsAndBounds() {
        val r = TerminalResult.Completed(
            provider = "p", sessionId = "s", exitCode = 0,
            stdout = "out-line", stderr = "err-line",
            stdoutTruncated = false, stderrTruncated = false, binarySuspected = false,
            totalStdoutBytes = 8, totalStderrBytes = 8,
            startedAtMs = 0, finishedAtMs = 1, cancelled = false, timedOut = false,
        )
        val s = TerminalOutput.evidenceSummary(r)
        assertTrue(s.contains("exit=0"))
        assertTrue(s.contains("stdout:\nout-line"))
        assertTrue(s.contains("stderr:\nerr-line"))
        assertTrue(s.length <= 2000)
    }

    @Test
    fun commandValidation() {
        assertNotNull(TerminalPolicy.validateCommand(""))
        assertNotNull(TerminalPolicy.validateCommand("   "))
        assertNotNull(TerminalPolicy.validateCommand("x".repeat(5000)))
        assertNull(TerminalPolicy.validateCommand("echo ok"))
    }

    @Test
    fun timeoutClamped() {
        assertEquals(TerminalPolicy.MIN_TIMEOUT_MS, TerminalPolicy.clampTimeout(1))
        assertEquals(TerminalPolicy.MAX_TIMEOUT_MS, TerminalPolicy.clampTimeout(999_999))
        assertEquals(5_000, TerminalPolicy.clampTimeout(5_000))
    }

    @Test
    fun cwdContainment() {
        val root = File(tmp.root, "workspaces")
        val ws = File(root, "ws-1").apply { mkdirs() }
        File(ws, "sub").mkdirs()
        assertNotNull(TerminalPolicy.containedCwd(root, ws, null))
        assertNotNull(TerminalPolicy.containedCwd(root, ws, "sub"))
        assertNull(TerminalPolicy.containedCwd(root, ws, "../ws-2"))
        assertNull(TerminalPolicy.containedCwd(root, ws, "/etc"))
        assertNull(TerminalPolicy.containedCwd(root, ws, "missing-dir"))
        val outside = File(tmp.root, "outside").apply { mkdirs() }
        assertNull(TerminalPolicy.containedCwd(root, outside, null))
    }

    @Test
    fun argvValidation() {
        assertNotNull(TerminalPolicy.validateArgv(emptyList()))
        assertNotNull(TerminalPolicy.validateArgv(listOf("")))
        assertNull(TerminalPolicy.validateArgv(listOf("/system/bin/sh", "-c", "echo ok")))
    }

    @Test
    fun envPinsPathAndCarriesNoSecrets() {
        val ws = File(tmp.root, "ws").apply { mkdirs() }
        val env = TerminalPolicy.buildEnv(ws, tmp.root)
        assertTrue(env.contains("PATH=/system/bin:/system/xbin"))
        assertTrue(env.any { it.startsWith("HOME=") })
        assertTrue(env.any { it.startsWith("TMPDIR=") })
        assertFalse(env.any { it.contains("TOKEN", ignoreCase = true) || it.contains("SECRET", ignoreCase = true) })
    }

    @Test
    fun providerStateStartsUnknownNeverReady() {
        val s = TerminalProviderState()
        assertEquals(TerminalAvailability.UNKNOWN, s.availability)
        assertNull(s.failureReason)
        assertEquals(0L, s.footprintBytes)
        assertTrue(s.bootstrap.contains("NONE"))
        assertTrue(s.limitations.isNotEmpty())
    }

    @Test
    fun providerFailureCarriesReason() {
        val s = TerminalProviderState(availability = TerminalAvailability.FAILED, failureReason = "spawn failed")
        assertEquals(TerminalAvailability.FAILED, s.availability)
        assertEquals("spawn failed", s.failureReason)
    }
}
