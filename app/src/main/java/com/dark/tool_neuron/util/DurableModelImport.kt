package com.dark.tool_neuron.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object DurableModelImport {
    fun publish(file: File, register: (File) -> Unit, rollback: (File) -> Unit) {
        try {
            register(file)
        } catch (failure: Throwable) {
            try {
                rollback(file)
                file.delete()
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
    }

    fun copy(root: File, open: () -> InputStream, onProgress: (Long) -> Unit = {}): File {
        check(root.isDirectory || root.mkdirs()) { "Cannot create model storage" }
        val partial = File.createTempFile("import-", ".partial", root)
        val destination = File(root, partial.name.removeSuffix(".partial") + ".gguf")
        try {
            open().use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        copied += count
                        onProgress(copied)
                    }
                    output.fd.sync()
                }
            }
            require(GgufHeader.fileHasGgufHeader(partial)) { "Selected file is not a GGUF model" }
            check(partial.renameTo(destination)) { "Cannot finish model import" }
            return destination
        } finally {
            partial.delete()
        }
    }
}
