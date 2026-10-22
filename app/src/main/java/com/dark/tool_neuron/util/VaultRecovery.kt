package com.dark.tool_neuron.util

import java.io.File
import java.util.UUID

object VaultRecovery {
    fun preserve(directory: File): File {
        require(directory.isDirectory && directory.canonicalFile == directory.absoluteFile) { "Vault path is missing or aliased" }
        val destination = File(directory.parentFile, "${directory.name}.quarantine-${UUID.randomUUID()}")
        check(directory.renameTo(destination)) { "Cannot preserve failed vault; refusing replacement" }
        check(destination.isDirectory && !directory.exists()) { "Vault preservation incomplete" }
        return destination
    }
}
