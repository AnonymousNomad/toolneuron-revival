package com.dark.tool_neuron.util

import android.content.Context
import android.net.Uri
import java.io.File

object GgufHeader {

    fun isGguf(header: ByteArray): Boolean =
        header.size >= 4 &&
            header[0] == 'G'.code.toByte() &&
            header[1] == 'G'.code.toByte() &&
            header[2] == 'U'.code.toByte() &&
            header[3] == 'F'.code.toByte()

    fun fileHasGgufHeader(file: File): Boolean =
        try {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                stream.read(header) == 4 && isGguf(header)
            }
        } catch (_: Exception) {
            false
        }

    fun uriHasGgufHeader(context: Context, uri: Uri): Boolean =
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                stream.read(header) == 4 && isGguf(header)
            } ?: false
        } catch (_: Exception) {
            false
        }
}
