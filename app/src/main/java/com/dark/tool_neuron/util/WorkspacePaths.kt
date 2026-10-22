package com.dark.tool_neuron.util

import java.io.File

object WorkspacePaths {
    fun root(base: File, id: String, registered: Set<String>): File {
        require(id in registered && id.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Unknown or invalid workspace" }
        val expected = File(base.canonicalFile, id).absoluteFile
        require(expected.canonicalFile == expected && expected.isDirectory) { "Workspace is missing or aliased" }
        return expected
    }
    fun unaliased(file: File): Boolean = file.absoluteFile == file.canonicalFile
}
