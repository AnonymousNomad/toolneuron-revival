package com.dark.tool_neuron.laptop.terminal

object TerminalOutput {
    const val MAX_EFFECT_STDOUT = 64 * 1024
    const val MAX_EFFECT_STDERR = 16 * 1024
    const val MAX_INTERACTIVE_BUFFER = 256 * 1024

    data class Bounded(
        val text: String,
        val truncated: Boolean,
        val binarySuspected: Boolean,
        val totalBytes: Int,
    )

    private val OSC = Regex("\u001B\\].*?(\u0007|\u001B\\\\)", RegexOption.DOT_MATCHES_ALL)
    private val CSI = Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]")

    fun sanitize(bytes: ByteArray): String {
        var s = bytes.toString(Charsets.UTF_8)
        s = OSC.replace(s, "")
        s = CSI.replace(s, "")
        s = s.replace("\r\n", "\n").replace('\r', '\n')
        val sb = StringBuilder(s.length)
        for (c in s) {
            when {
                c == '\n' || c == '\t' -> sb.append(c)
                c.code < 0x20 -> Unit
                c == '\u007F' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun bound(bytes: ByteArray, maxBytes: Int, totalBytes: Int = bytes.size): Bounded {
        val truncated = totalBytes > maxBytes
        val slice = if (bytes.size > maxBytes) bytes.copyOfRange(0, maxBytes) else bytes
        val text = sanitize(slice)
        val replacementCount = text.count { it == '\uFFFD' }
        val nulCount = slice.count { it == 0.toByte() }
        val binarySuspected = (text.isNotEmpty() && replacementCount * 10 > text.length) ||
            (slice.isNotEmpty() && nulCount * 20 > slice.size)
        val shown = if (binarySuspected) "binary output ($totalBytes bytes, not rendered)" else text
        return Bounded(shown, truncated, binarySuspected, totalBytes)
    }

    fun evidenceSummary(result: com.dark.tool_neuron.laptop.TerminalResult.Completed, maxChars: Int = 2000): String {
        val head = buildString {
            append("terminal exit=").append(result.exitCode)
            if (result.cancelled) append(" cancelled=true")
            if (result.timedOut) append(" timedOut=true")
            append(" stdout_bytes=").append(result.totalStdoutBytes)
            append(" stderr_bytes=").append(result.totalStderrBytes)
        }
        val body = buildString {
            if (result.stdout.isNotBlank()) {
                append("\nstdout:\n").append(result.stdout.trim())
                if (result.stdoutTruncated) append("\n[stdout truncated to ").append(TerminalOutput.MAX_EFFECT_STDOUT).append(" bytes]")
            }
            if (result.stderr.isNotBlank()) {
                append("\nstderr:\n").append(result.stderr.trim())
                if (result.stderrTruncated) append("\n[stderr truncated to ").append(TerminalOutput.MAX_EFFECT_STDERR).append(" bytes]")
            }
        }
        val full = head + body
        return if (full.length <= maxChars) full else full.take(maxChars) + "\n[evidence summary truncated]"
    }
}
