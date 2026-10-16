package com.dark.tool_neuron

import com.dark.tool_neuron.util.GgufMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufMetaTest {

    private fun u32(v: Long): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v.toInt()).array()

    private fun u64(v: Long): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array()

    private fun ggufString(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        return u64(bytes.size.toLong()) + bytes
    }

    private fun kvString(key: String, value: String): ByteArray =
        ggufString(key) + u32(8) + ggufString(value)

    private fun kvU32(key: String, value: Long): ByteArray =
        ggufString(key) + u32(4) + u32(value)

    private fun header(kvCount: Long, vararg kv: ByteArray): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("GGUF".toByteArray(Charsets.US_ASCII))
        body.write(u32(3))
        body.write(u64(0))
        body.write(u64(kvCount))
        kv.forEach { body.write(it) }
        return body.toByteArray()
    }

    private fun writeTemp(bytes: ByteArray): File {
        val f = File.createTempFile("gguf-meta", ".gguf")
        f.writeBytes(bytes)
        return f
    }

    @Test
    fun readsArchitectureFromValidHeader() {
        val f = writeTemp(header(2, kvString("general.name", "test"), kvString("general.architecture", "llama")))
        try {
            assertEquals("llama", GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun readsBertArchitecture() {
        val f = writeTemp(header(1, kvString("general.architecture", "bert")))
        try {
            assertEquals("bert", GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun skipsNonStringValuesBeforeArchitecture() {
        val f = writeTemp(header(3,
            kvU32("general.quantization_version", 2),
            kvString("general.name", "x"),
            kvString("general.architecture", "qwen2"),
        ))
        try {
            assertEquals("qwen2", GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun skipsArrayValuesBeforeArchitecture() {
        val array = ggufString("tokenizer.ggml.tokens") + u32(9) + u32(8) + u64(2) +
            ggufString("a") + ggufString("b")
        val f = writeTemp(header(2, array, kvString("general.architecture", "lfm2")))
        try {
            assertEquals("lfm2", GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun returnsNullWhenArchitectureAbsent() {
        val f = writeTemp(header(1, kvString("general.name", "no-arch")))
        try {
            assertNull(GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun returnsNullForWrongMagic() {
        val bytes = header(1, kvString("general.architecture", "llama"))
        bytes[0] = 'X'.code.toByte()
        val f = writeTemp(bytes)
        try {
            assertNull(GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun returnsNullForTruncatedFile() {
        val full = header(2, kvString("general.name", "x"), kvString("general.architecture", "llama"))
        val f = writeTemp(full.copyOf(full.size - 5))
        try {
            assertNull(GgufMeta.readArchitecture(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun returnsNullForMissingFile() {
        assertNull(GgufMeta.readArchitecture(File("does-not-exist-98765.gguf")))
    }

    private fun tensorInfo(name: String, dims: List<Long>, type: Long): ByteArray {
        val body = ByteArrayOutputStream()
        body.write(ggufString(name))
        body.write(u32(dims.size.toLong()))
        dims.forEach { body.write(u64(it)) }
        body.write(u32(type))
        body.write(u64(0))
        return body.toByteArray()
    }

    private fun headerWithTensors(
        kvCount: Long,
        tensorCount: Long,
        kv: List<ByteArray>,
        tensors: List<ByteArray>,
    ): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("GGUF".toByteArray(Charsets.US_ASCII))
        body.write(u32(3))
        body.write(u64(tensorCount))
        body.write(u64(kvCount))
        kv.forEach { body.write(it) }
        tensors.forEach { body.write(it) }
        return body.toByteArray()
    }

    @Test
    fun readsTensorTypeIdsIncludingQ1_0() {
        val f = writeTemp(headerWithTensors(
            kvCount = 1,
            tensorCount = 2,
            kv = listOf(kvString("general.architecture", "qwen3")),
            tensors = listOf(
                tensorInfo("token_embd.weight", listOf(4096, 128256), 41),
                tensorInfo("blk.0.attn_q.weight", listOf(4096, 4096), 12),
            ),
        ))
        try {
            assertEquals(setOf(41L, 12L), GgufMeta.readTensorTypeIds(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun readsTensorTypeIdsWithoutQ1_0() {
        val f = writeTemp(headerWithTensors(
            kvCount = 2,
            tensorCount = 2,
            kv = listOf(
                kvU32("general.quantization_version", 2),
                kvString("general.architecture", "llama"),
            ),
            tensors = listOf(
                tensorInfo("token_embd.weight", listOf(4096, 128256), 12),
                tensorInfo("blk.0.ffn_down.weight", listOf(4096, 11008), 8),
            ),
        ))
        try {
            assertEquals(setOf(12L, 8L), GgufMeta.readTensorTypeIds(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun returnsNullForTruncatedTensorSection() {
        val full = headerWithTensors(
            kvCount = 1,
            tensorCount = 2,
            kv = listOf(kvString("general.architecture", "llama")),
            tensors = listOf(
                tensorInfo("token_embd.weight", listOf(4096, 128256), 41),
                tensorInfo("blk.0.attn_q.weight", listOf(4096, 4096), 12),
            ),
        )
        val f = writeTemp(full.copyOf(full.size - 7))
        try {
            assertNull(GgufMeta.readTensorTypeIds(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun tensorTypeIdsNullForMissingFile() {
        assertNull(GgufMeta.readTensorTypeIds(File("does-not-exist-98765.gguf")))
    }

    @Test
    fun readsTensorTypeIdsAcrossLargeTokenizerSizedHeaders() {
        val big = "x".repeat(5 * 1024 * 1024)
        val f = writeTemp(headerWithTensors(
            kvCount = 2,
            tensorCount = 1,
            kv = listOf(
                kvString("tokenizer.ggml.tokens", big),
                kvString("general.architecture", "qwen3"),
            ),
            tensors = listOf(tensorInfo("output.weight", listOf(4096, 128256), 41)),
        ))
        try {
            assertEquals(setOf(41L), GgufMeta.readTensorTypeIds(f))
        } finally {
            f.delete()
        }
    }
}
