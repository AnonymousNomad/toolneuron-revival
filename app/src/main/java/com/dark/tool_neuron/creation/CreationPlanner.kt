package com.dark.tool_neuron.creation

import com.dark.tool_neuron.laptop.WorkspaceRef
import kotlinx.serialization.Serializable

@Serializable enum class CreationJobKind { KEYFRAME, VIDEO, VOICE, MUSIC, SFX, LIPSYNC, ASSEMBLY }
@Serializable enum class CreationJobState { BLOCKED }
@Serializable data class CreationJob(val id: String, val kind: CreationJobKind, val subjectId: String, val dependencies: List<String>,
    val state: CreationJobState = CreationJobState.BLOCKED, val reason: String = "No qualified renderer configured; no media generated")
@Serializable data class CreationCheck(val id: String, val passed: Boolean, val detail: String)
@Serializable data class CreationManifest(val workspaceId: String, val title: String, val plan: CreationPlan, val plannedDurationMs: Long,
    val jobs: List<CreationJob>, val checks: List<CreationCheck>, val format: String = "toolneuron-creation-manifest/1",
    val stage: String = "PLANNING_ONLY", val assetStatus: String = "Registry references only; no referenced media downloaded or rendered")
object CreationPlanner {
    fun manifest(workspace: WorkspaceRef): CreationManifest {
        val plan = workspace.creation ?: error("Not a creation project")
        plan.validate()
        val jobs = mutableListOf<CreationJob>()
        val checks = mutableListOf<CreationCheck>()
        plan.assets.forEach { asset ->
            checks += CreationCheck("rights:${asset.id}", asset.rights != AssetRights.UNKNOWN && asset.license.isNotBlank(), "${asset.name}: rights=${asset.rights}; author-declared license=${asset.license.ifBlank { "unknown" }}; declaration is not independent verification")
            checks += CreationCheck("source:${asset.id}", asset.sourceRef.isNotBlank(), "${asset.name}: reference locator ${if (asset.sourceRef.isBlank()) "missing" else "recorded; media remains unmaterialized"}")
        }
        plan.characters.forEach { character -> checks += CreationCheck("character:${character.id}", character.bible.isNotBlank() && character.assetIds.any { id -> plan.assets.any { it.id == id && it.kind in setOf(CreationAssetKind.IMAGE, CreationAssetKind.VIDEO) && it.sourceRef.isNotBlank() } }, "${character.name}: declared visual reference metadata and bible; media remains unmaterialized") }
        plan.audioSlots.forEach { slot ->
            jobs += CreationJob("${slot.id}:audio", CreationJobKind.valueOf(slot.kind.name), slot.id, emptyList())
            checks += CreationCheck("audio:${slot.id}", slot.script.isNotBlank(), "${slot.name}: ${slot.kind} slot is planned, not synthesized")
        }
        val episodeFinals = mutableListOf<String>()
        plan.episodes().forEach { episode ->
            val finals = mutableListOf<String>()
            val backgroundAudio = linkedSetOf<String>()
            checks += CreationCheck("episode:${episode.id}", episode.scenes.isNotEmpty(), "${episode.title}: at least one scene is needed")
            episode.scenes.forEach { scene ->
                checks += CreationCheck("scene:${scene.id}", scene.shots.isNotEmpty(), "${scene.title}: at least one shot is needed")
                scene.shots.forEach { shot ->
                    checks += CreationCheck("prompt:${shot.id}", shot.prompt.isNotBlank(), "${scene.title} shot ${shot.ordinal}: bounded prompt and referenced character/asset identities")
                    val keyframe = "${shot.id}:keyframe"
                    val video = "${shot.id}:video"
                    jobs += CreationJob(keyframe, CreationJobKind.KEYFRAME, shot.id, emptyList())
                    jobs += CreationJob(video, CreationJobKind.VIDEO, shot.id, listOf(keyframe))
                    val audio = shot.audioSlotIds.filter { ref -> plan.audioSlots.single { it.id == ref }.kind == CreationAudioKind.VOICE }.map { "$it:audio" }.toMutableList()
                    backgroundAudio += shot.audioSlotIds.filter { ref -> plan.audioSlots.single { it.id == ref }.kind != CreationAudioKind.VOICE }.map { "$it:audio" }
                    if (shot.dialogue.isNotBlank() && audio.isEmpty()) {
                        val voice = "${shot.id}:voice"
                        jobs += CreationJob(voice, CreationJobKind.VOICE, shot.id, emptyList())
                        audio += voice
                    }
                    if (audio.isNotEmpty()) {
                        val final = "${shot.id}:lipsync"
                        jobs += CreationJob(final, CreationJobKind.LIPSYNC, shot.id, listOf(video) + audio)
                        finals += final
                    } else finals += video
                }
            }
            val assembly = "${episode.id}:assembly"
            jobs += CreationJob(assembly, CreationJobKind.ASSEMBLY, episode.id, finals + backgroundAudio)
            episodeFinals += assembly
        }
        jobs += CreationJob("${workspace.id}:assembly", CreationJobKind.ASSEMBLY, workspace.id, episodeFinals)
        require(jobs.map { it.id }.distinct().size == jobs.size && jobs.all { job -> job.dependencies.all { ref -> jobs.any { it.id == ref } } })
        return CreationManifest(workspace.id, workspace.name, plan, plan.shots().sumOf { it.durationMs }, jobs, checks)
    }
}
data class CreationProviderRequest(val workspaceId: String, val shotId: String, val prompt: String, val referenceIds: List<String>)
data class CreationProviderOutput(val artifactRef: String, val sha256: String, val provenance: CreationProvenance)
interface CreationProviderAdapter {
    val id: String
    val available: Boolean
    val reason: String
    suspend fun generate(request: CreationProviderRequest): Result<CreationProviderOutput>
}
object CreationProviders {
    val all: List<CreationProviderAdapter> = listOf("image", "video", "voice", "music").map { kind ->
        object : CreationProviderAdapter {
            override val id = "creation.$kind"
            override val available = false
            override val reason = "UNCONFIGURED: no credential-backed or qualified local $kind renderer is connected"
            override suspend fun generate(request: CreationProviderRequest) = Result.failure<CreationProviderOutput>(IllegalStateException("$id: $reason; no fallback"))
        }
    }
}
