package com.dark.tool_neuron.reporting

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Serializable data class BetaReportManifest(val appVersion: String, val versionCode: Int, val device: String,
    val androidVersion: String, val abis: List<String>, val models: List<String>, val reproduction: String,
    val logs: List<String>, val recoveryCandidates: Int, val sourceRevision: String = "UNRECORDED",
    val format: String = "toolneuron-beta-report/1",
    val limitations: String = "Local diagnostic snapshot. No transcripts, project source, model paths, keys or device identifiers are collected automatically. Native Covert R0 source unavailable; physical acceptance NOT TESTED.")
object BetaReports {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    fun preview(manifest: BetaReportManifest): String {
        require(manifest.appVersion.length <= 100 && manifest.device.length <= 256 && manifest.androidVersion.length <= 100 && manifest.sourceRevision.length <= 100)
        require(manifest.abis.size <= 10 && manifest.abis.all { it.length <= 100 })
        require(manifest.models.size <= 100 && manifest.models.all { it.length <= 200 })
        require(manifest.reproduction.length <= 8000 && manifest.logs.size <= 100 && manifest.logs.all { it.length <= 400 })
        require(manifest.recoveryCandidates >= 0)
        return json.encodeToString(BetaReportManifest.serializer(), manifest)
    }
    fun bundle(manifest: BetaReportManifest, screenshot: ByteArray?): ByteArray {
        require(screenshot == null || screenshot.size <= 4 * 1024 * 1024)
        val body = preview(manifest).toByteArray(Charsets.UTF_8)
        return ByteArrayOutputStream().also { bytes -> ZipOutputStream(bytes).use { zip ->
            fun entry(name: String, content: ByteArray) {
                zip.putNextEntry(ZipEntry(name).apply { time = 0 })
                zip.write(content)
                zip.closeEntry()
            }
            entry("report.json", body)
            screenshot?.let { entry("screenshot.bin", it) }
        } }.toByteArray()
    }
}
