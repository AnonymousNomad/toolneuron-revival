package com.dark.tool_neuron

import com.dark.tool_neuron.util.GgufHeader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GgufHeaderTest {

    @Test
    fun isGguf_acceptsGgufMagic() {
        assertTrue(GgufHeader.isGguf("GGUF".toByteArray()))
        assertTrue(GgufHeader.isGguf(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x03, 0x00)))
    }

    @Test
    fun isGguf_rejectsOtherBytes() {
        assertFalse(GgufHeader.isGguf(byteArrayOf(0x47, 0x47, 0x55, 0x00)))
        assertFalse(GgufHeader.isGguf(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
        assertFalse(GgufHeader.isGguf("gguf".toByteArray()))
    }

    @Test
    fun isGguf_rejectsShortArray() {
        assertFalse(GgufHeader.isGguf(byteArrayOf(0x47, 0x47, 0x55)))
        assertFalse(GgufHeader.isGguf(ByteArray(0)))
    }

    @Test
    fun fileHasGgufHeader_trueForGgufFile() {
        val f = File.createTempFile("gguf", ".gguf")
        try {
            f.writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x03, 0x00))
            assertTrue(GgufHeader.fileHasGgufHeader(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun fileHasGgufHeader_falseForInvalidFile() {
        val f = File.createTempFile("invalid", ".bin")
        try {
            f.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00))
            assertFalse(GgufHeader.fileHasGgufHeader(f))
        } finally {
            f.delete()
        }
    }

    @Test
    fun fileHasGgufHeader_falseForMissingFile() {
        assertFalse(GgufHeader.fileHasGgufHeader(File("does-not-exist-12345.gguf")))
    }
}
