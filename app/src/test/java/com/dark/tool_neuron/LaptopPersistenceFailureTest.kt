package com.dark.tool_neuron

import com.dark.tool_neuron.laptop.LaptopCore
import com.dark.tool_neuron.laptop.LaptopPersistence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class LaptopPersistenceFailureTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun failedPermissionSaveDoesNotPublishANondurableRevocation() {
        var payload: ByteArray? = null
        var fail = false
        val store = object : LaptopPersistence {
            override fun load() = payload
            override fun save(bytes: ByteArray) {
                if (fail) throw IOException("storage unavailable")
                payload = bytes
            }
            override fun quarantineCorrupt(bytes: ByteArray) = Unit
        }
        val core = LaptopCore(store, File(temp.root, "workspaces"))
        val capability = "dev.terminal.execute@1"
        core.setPermission(capability, true).getOrThrow()
        fail = true
        assertTrue(core.setPermission(capability, false).isFailure)
        assertTrue(core.hasPermission(capability))
        fail = false
        core.setPermission(capability, false).getOrThrow()
        assertFalse(core.hasPermission(capability))
    }
}
