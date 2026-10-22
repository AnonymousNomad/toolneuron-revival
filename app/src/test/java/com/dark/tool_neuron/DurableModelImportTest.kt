package com.dark.tool_neuron

import com.dark.tool_neuron.util.DurableModelImport
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException

class DurableModelImportTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun importedFileSurvivesSourceClosureAndReportsActualSize() {
        val bytes = "GGUF".toByteArray() + ByteArray(100_000) { 42 }
        val progress = mutableListOf<Long>()
        val result = DurableModelImport.copy(temp.root, { ByteArrayInputStream(bytes) }, progress::add)
        assertArrayEquals(bytes, result.readBytes())
        assertEquals(bytes.size.toLong(), result.length())
        assertEquals(result.canonicalFile.parentFile, temp.root.canonicalFile)
        assertEquals(bytes.size.toLong(), progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> a <= b })
        assertEquals(listOf(result.name), temp.root.list()!!.toList())
    }

    @Test fun importsNeverOverwriteAnExistingModel() {
        val open = { ByteArrayInputStream("GGUFexample".toByteArray()) }
        val first = DurableModelImport.copy(temp.root, open)
        val second = DurableModelImport.copy(temp.root, open)
        assertNotEquals(first.absolutePath, second.absolutePath)
        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test fun invalidFileIsNotPublished() {
        assertThrows(IllegalArgumentException::class.java) {
            DurableModelImport.copy(temp.root, { ByteArrayInputStream("not a model".toByteArray()) })
        }
        assertTrue(temp.root.list()!!.isEmpty())
    }

    @Test fun readFailureClosesSourceAndRemovesPartialCopy() {
        var closed = false
        val input = object : InputStream() {
            override fun read(): Int = throw IOException("source disconnected")
            override fun close() { closed = true }
        }
        assertThrows(IOException::class.java) { DurableModelImport.copy(temp.root, { input }) }
        assertTrue(closed)
        assertTrue(temp.root.list()!!.isEmpty())
    }

    @Test fun cancellationRemovesPartialCopy() {
        assertThrows(CancellationException::class.java) {
            DurableModelImport.copy(temp.root, { ByteArrayInputStream("GGUFexample".toByteArray()) }) {
                throw CancellationException("cancelled")
            }
        }
        assertTrue(temp.root.list()!!.isEmpty())
    }

    @Test fun failedPublicationRollsBackPersistedRowBeforeRemovingFile() {
        val file = temp.newFile("model.gguf")
        var persisted = false
        assertThrows(IOException::class.java) {
            DurableModelImport.publish(file, {
                persisted = true
                throw IOException("refresh failed after flush")
            }, { persisted = false })
        }
        assertFalse(persisted)
        assertFalse(file.exists())
    }

    @Test fun failedRollbackKeepsTheBackingFileAndOriginalFailure() {
        val file = temp.newFile("model.gguf")
        val failure = assertThrows(IOException::class.java) {
            DurableModelImport.publish(file, { throw IOException("refresh failed") }, {
                throw IllegalStateException("rollback failed")
            })
        }
        assertEquals("refresh failed", failure.message)
        assertEquals("rollback failed", failure.suppressed.single().message)
        assertTrue(file.exists())
    }
}
