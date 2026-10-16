package com.dark.tool_neuron.util

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

object GgufMeta {
    private const val MAX_KV_SCAN = 512
    private const val MAX_HEADER_BYTES = 4L * 1024 * 1024

    /**
     * The tensor-type scan must cover the whole metadata section. Real files with
     * 128k-token tokenizer arrays carry ~6MB of KV data before the tensor table,
     * so the architecture reader's 4MB bound is too small here (found by device
     * acceptance: Q1_0 guard failed open on Bonsai-8B and the native loader crashed).
     */
    private const val MAX_TENSOR_SCAN_BYTES = 128L * 1024 * 1024
    private const val MAX_TENSOR_SCAN = 100_000

    fun readArchitecture(file: File): String? =
        try {
            file.inputStream().use { readArchitecture(it) }
        } catch (_: Exception) {
            null
        }

    fun readTensorTypeIds(file: File): Set<Long>? =
        try {
            file.inputStream().use { readTensorTypeIds(it) }
        } catch (_: Exception) {
            null
        }

    /**
     * Reads the ggml tensor type ids used by every tensor in a GGUF file.
     * Returns null when the header cannot be parsed (callers must treat that as
     * "unknown" and let the native loader surface its own error).
     */
    fun readTensorTypeIds(raw: InputStream): Set<Long>? {
        val input = BufferedInputStream(raw)
        val magic = ByteArray(4)
        if (input.read(magic) != 4) return null
        if (magic[0] != 'G'.code.toByte() || magic[1] != 'G'.code.toByte() ||
            magic[2] != 'U'.code.toByte() || magic[3] != 'F'.code.toByte()
        ) return null

        val version = readU32(input) ?: return null
        if (version !in 1L..3L) return null
        val tensorCount = readU64(input) ?: return null
        if (tensorCount <= 0 || tensorCount > MAX_TENSOR_SCAN) return null
        val kvCount = readU64(input) ?: return null
        if (kvCount < 0 || kvCount > 100_000) return null

        var consumed = 4L + 4 + 8 + 8
        var scanned = 0
        while (scanned < kvCount) {
            val keyLen = readU64(input) ?: return null
            if (keyLen < 0 || keyLen > 4096) return null
            if (skipFully(input, keyLen) == null) return null
            consumed += 8 + keyLen
            if (consumed > MAX_TENSOR_SCAN_BYTES) return null
            val type = readU32(input) ?: return null
            consumed += 4
            val skipped = skipValue(input, type, 0) ?: return null
            consumed += skipped
            if (consumed > MAX_TENSOR_SCAN_BYTES) return null
            scanned++
        }

        val types = HashSet<Long>()
        var i = 0L
        while (i < tensorCount) {
            val nameLen = readU64(input) ?: return null
            if (nameLen < 0 || nameLen > 4096) return null
            if (skipFully(input, nameLen) == null) return null
            consumed += 8 + nameLen
            val nDims = readU32(input) ?: return null
            if (nDims > 8) return null
            consumed += 4
            if (skipFully(input, nDims * 8L) == null) return null
            consumed += nDims * 8L
            val tensorType = readU32(input) ?: return null
            consumed += 4
            if (skipFully(input, 8L) == null) return null
            consumed += 8
            if (consumed > MAX_TENSOR_SCAN_BYTES) return null
            types.add(tensorType)
            i++
        }
        return types
    }

    fun readArchitecture(raw: InputStream): String? {
        val input = BufferedInputStream(raw)
        val magic = ByteArray(4)
        if (input.read(magic) != 4) return null
        if (magic[0] != 'G'.code.toByte() || magic[1] != 'G'.code.toByte() ||
            magic[2] != 'U'.code.toByte() || magic[3] != 'F'.code.toByte()
        ) return null

        val version = readU32(input) ?: return null
        if (version !in 1L..3L) return null
        readU64(input) ?: return null
        val kvCount = readU64(input) ?: return null
        if (kvCount <= 0 || kvCount > 100_000) return null

        var consumed = 4L + 4 + 8 + 8
        var scanned = 0
        while (scanned < kvCount && scanned < MAX_KV_SCAN) {
            val keyLen = readU64(input) ?: return null
            if (keyLen < 0 || keyLen > 4096) return null
            consumed += 8 + keyLen
            if (consumed > MAX_HEADER_BYTES) return null
            val keyBytes = ByteArray(keyLen.toInt())
            if (readFully(input, keyBytes) != keyBytes.size) return null

            val type = readU32(input) ?: return null
            consumed += 4
            if (type == 8L) {
                val len = readU64(input) ?: return null
                if (len < 0 || len > 65536) return null
                consumed += 8 + len
                if (consumed > MAX_HEADER_BYTES) return null
                val valueBytes = ByteArray(len.toInt())
                if (readFully(input, valueBytes) != valueBytes.size) return null
                if (keyBytes.toString(Charsets.UTF_8) == "general.architecture") {
                    return valueBytes.toString(Charsets.UTF_8)
                }
            } else {
                val skipped = skipValue(input, type, 0) ?: return null
                consumed += skipped
                if (consumed > MAX_HEADER_BYTES) return null
            }
            scanned++
        }
        return null
    }

    private fun skipValue(input: InputStream, type: Long, depth: Int): Long? {
        if (depth > 4) return null
        return when (type) {
            0L, 1L, 7L -> if (skipFully(input, 1L) == null) null else 1L
            2L, 3L -> if (skipFully(input, 2L) == null) null else 2L
            4L, 5L, 6L -> if (skipFully(input, 4L) == null) null else 4L
            10L, 11L, 12L -> if (skipFully(input, 8L) == null) null else 8L
            8L -> {
                val len = readU64(input) ?: return null
                if (len < 0 || len > 16L * 1024 * 1024) return null
                if (skipFully(input, len) == null) return null
                8 + len
            }
            9L -> {
                val elemType = readU32(input) ?: return null
                val count = readU64(input) ?: return null
                if (count < 0 || count > 1_000_000) return null
                var total = 12L
                var i = 0L
                while (i < count) {
                    val s = skipValue(input, elemType, depth + 1) ?: return null
                    total += s
                    if (total > MAX_HEADER_BYTES) return null
                    i++
                }
                total
            }
            else -> null
        }
    }

    private fun readU32(input: InputStream): Long? {
        val b = ByteArray(4)
        if (readFully(input, b) != 4) return null
        return (b[0].toLong() and 0xFF) or
            ((b[1].toLong() and 0xFF) shl 8) or
            ((b[2].toLong() and 0xFF) shl 16) or
            ((b[3].toLong() and 0xFF) shl 24)
    }

    private fun readU64(input: InputStream): Long? {
        val b = ByteArray(8)
        if (readFully(input, b) != 8) return null
        var v = 0L
        for (i in 0 until 8) v = v or ((b[i].toLong() and 0xFF) shl (8 * i))
        return v
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) break
            off += n
        }
        return off
    }

    private fun skipFully(input: InputStream, count: Long): Long? {
        var remaining = count
        val buf = ByteArray(8192)
        while (remaining > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n < 0) return null
            remaining -= n
        }
        return count
    }
}
