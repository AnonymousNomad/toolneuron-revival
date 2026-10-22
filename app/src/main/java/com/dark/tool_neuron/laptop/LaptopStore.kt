package com.dark.tool_neuron.laptop

import android.content.Context
import com.dark.hxs.HexStorage
import com.dark.hxs.HxsRecord
import com.dark.hxs_encryptor.HxsEncryptor
import com.dark.tool_neuron.data.AppKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

interface LaptopPersistence {
    fun load(): ByteArray?
    fun save(bytes: ByteArray)
    fun quarantineCorrupt(bytes: ByteArray)
}

class HxsLaptopPersistence @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val keyStore: AppKeyStore,
    private val encryptor: HxsEncryptor,
) : LaptopPersistence {
    private val storage = HexStorage()
    private var newlyCreated = false

    init {
        val secureDir = File(context.filesDir, SECURE_DIR).apply { mkdirs() }
        val secureBase = secureDir.absolutePath

        val dek = keyStore.unwrapOrCreateDek()
        val signerHash = keyStore.installSignerHash()
        val userKey = encryptor.deriveKey(ikm = dek, salt = signerHash, info = USER_KEY_INFO)

        val opened = openOrRebuild(secureBase, dek, userKey)
        if (!opened) throw SecurityException("Failed to open encrypted laptop vault")

        check(storage.ensureCollection(COLLECTION)) { "Encrypted collection could not be read; original bytes preserved" }
    }

    private fun openOrRebuild(base: String, dek: ByteArray, userKey: ByteArray): Boolean {
        if (File(base).listFiles()?.isNotEmpty() == true) {
            if (storage.exists(base) && storage.openEncrypted(base, dek, userKey, encryptor)) return true
            com.dark.tool_neuron.util.VaultRecovery.preserve(File(base))
            check(File(base).mkdirs()) { "Cannot create replacement vault" }
        }
        newlyCreated = true
        return storage.createEncrypted(base, dek, userKey, encryptor)
    }

    override fun load(): ByteArray? {
        val rec = storage.getAll(COLLECTION).firstOrNull { it.getString(TAG_RECORD_ID) == RECORD_STATE }
            ?: if (newlyCreated) return null else error("Existing laptop vault has no state record; original bytes preserved")
        return rec.getString(TAG_PAYLOAD).toByteArray(Charsets.UTF_8)
    }

    override fun save(bytes: ByteArray) {
        storage.getAll(COLLECTION)
            .filter { it.getString(TAG_RECORD_ID) == RECORD_STATE }
            .forEach { storage.delete(COLLECTION, it.id) }
        storage.put(COLLECTION, HxsRecord.build {
            putString(TAG_RECORD_ID, RECORD_STATE)
            putString(TAG_PAYLOAD, bytes.toString(Charsets.UTF_8))
        })
        storage.flush(COLLECTION)
    }

    override fun quarantineCorrupt(bytes: ByteArray) {
        storage.put(COLLECTION, HxsRecord.build {
            putString(TAG_RECORD_ID, "$RECORD_QUARANTINE-${java.util.UUID.randomUUID()}")
            putString(TAG_PAYLOAD, bytes.toString(Charsets.UTF_8))
        })
        storage.flush(COLLECTION)
    }

    private companion object {
        const val COLLECTION = "laptop_state"
        const val SECURE_DIR = "laptop_v1"
        const val USER_KEY_INFO = "tn.laptop.user_key.v2"
        const val RECORD_STATE = "state"
        const val RECORD_QUARANTINE = "corrupt_quarantine"
        const val MAX_QUARANTINE_BYTES = 512 * 1024
        const val TAG_RECORD_ID = 1
        const val TAG_PAYLOAD = 2
    }
}
