package com.dark.tool_neuron.creation

import com.dark.tool_neuron.laptop.LaptopCore
import java.util.UUID

private fun id(prefix: String) = "$prefix-" + UUID.randomUUID().toString().replace("-", "")
fun LaptopCore.createCreation(name: String, synopsis: String, kind: CreationKind) = runCatching {
    check(hasPermission(CreationPlan.CAPABILITY)) { "Grant Creator planning in Abilities first" }
    createWorkspace(name, mapOf("studio.kind" to "creator"), creation = CreationPlan.seed(kind, synopsis)).getOrThrow()
}
fun LaptopCore.registerCreationAsset(workspaceId: String, name: String, kind: CreationAssetKind, sourceRef: String, rights: AssetRights, license: String, prompt: String): Result<CreationAsset> {
    val record = CreationAsset(id("asset"), name.trim(), kind, sourceRef.trim(), rights, license.trim(), CreationProvenance(prompt))
    return updateCreationPlan(workspaceId) { it.copy(assets = it.assets + record) }.map { record }
}
fun LaptopCore.addCreationCharacter(workspaceId: String, name: String, bible: String, assetIds: List<String>): Result<CreationCharacter> {
    val record = CreationCharacter(id("character"), name.trim(), bible, assetIds)
    return updateCreationPlan(workspaceId) { it.copy(characters = it.characters + record) }.map { record }
}
fun LaptopCore.addCreationAudio(workspaceId: String, kind: CreationAudioKind, name: String, script: String, assetId: String?): Result<CreationAudioSlot> {
    val record = CreationAudioSlot(id("audio"), kind, name.trim(), script, assetId)
    return updateCreationPlan(workspaceId) { it.copy(audioSlots = it.audioSlots + record) }.map { record }
}
fun LaptopCore.addCreationSeason(workspaceId: String, title: String): Result<CreationSeason> {
    val recordId = id("season")
    return updateCreationPlan(workspaceId) { it.copy(seasons = it.seasons + CreationSeason(recordId, it.seasons.size + 1, title.trim())) }.map { it.seasons.single { item -> item.id == recordId } }
}
fun LaptopCore.addCreationEpisode(workspaceId: String, seasonId: String, title: String): Result<CreationEpisode> {
    val recordId = id("episode")
    return updateCreationPlan(workspaceId) { plan ->
        require(plan.seasons.any { it.id == seasonId }) { "Unknown season in this project" }
        plan.copy(seasons = plan.seasons.map { if (it.id != seasonId) it else it.copy(episodes = it.episodes + CreationEpisode(recordId, it.episodes.size + 1, title.trim())) })
    }.map { it.episodes().single { item -> item.id == recordId } }
}
fun LaptopCore.addCreationScene(workspaceId: String, episodeId: String, title: String): Result<CreationScene> {
    val recordId = id("scene")
    return updateCreationPlan(workspaceId) { plan ->
        require(plan.episodes().any { it.id == episodeId }) { "Unknown episode in this project" }
        plan.copy(seasons = plan.seasons.map { season -> season.copy(episodes = season.episodes.map { if (it.id != episodeId) it else it.copy(scenes = it.scenes + CreationScene(recordId, it.scenes.size + 1, title.trim())) }) })
    }.map { it.scenes().single { item -> item.id == recordId } }
}
fun LaptopCore.addCreationShot(workspaceId: String, sceneId: String, prompt: String, durationMs: Long, dialogue: String = "", characterIds: List<String> = emptyList(), assetIds: List<String> = emptyList(), audioSlotIds: List<String> = emptyList()): Result<CreationShot> {
    val recordId = id("shot")
    return updateCreationPlan(workspaceId) { plan ->
        require(plan.scenes().any { it.id == sceneId }) { "Unknown scene in this project" }
        plan.mapScenes { scene -> if (scene.id != sceneId) scene else scene.copy(shots = scene.shots + CreationShot(recordId, scene.shots.size + 1, CreationProvenance(prompt), durationMs, dialogue, characterIds, assetIds, audioSlotIds)) }
    }.map { it.shots().single { item -> item.id == recordId } }
}
fun LaptopCore.editCreationShot(workspaceId: String, shotId: String, prompt: String, durationMs: Long, dialogue: String, characterIds: List<String>, assetIds: List<String>, audioSlotIds: List<String>): Result<CreationShot> = updateCreationPlan(workspaceId) { plan ->
    require(plan.shots().any { it.id == shotId }) { "Unknown shot in this project" }
    plan.mapScenes { scene -> scene.copy(shots = scene.shots.map { if (it.id != shotId) it else it.copy(provenance = CreationProvenance(prompt), durationMs = durationMs, dialogue = dialogue, characterIds = characterIds, assetIds = assetIds, audioSlotIds = audioSlotIds) }) }
}.map { it.shots().single { shot -> shot.id == shotId } }
fun LaptopCore.creationManifest(workspaceId: String): Result<CreationManifest> = runCatching {
    check(hasPermission(CreationPlan.CAPABILITY)) { "Creator planning permission denied" }
    CreationPlanner.manifest(state.value.workspaces.single { it.id == workspaceId })
}
private fun CreationPlan.mapScenes(edit: (CreationScene) -> CreationScene) = copy(seasons = seasons.map { season -> season.copy(episodes = season.episodes.map { episode -> episode.copy(scenes = episode.scenes.map(edit)) }) })

fun LaptopCore.editCreationAsset(workspaceId: String, assetId: String, name: String, kind: CreationAssetKind, sourceRef: String, rights: AssetRights, license: String, prompt: String): Result<CreationAsset> = updateCreationPlan(workspaceId) { plan ->
    require(plan.assets.any { it.id == assetId }) { "Unknown asset in this project" }
    plan.copy(assets = plan.assets.map { if (it.id != assetId) it else it.copy(name = name.trim(), kind = kind, sourceRef = sourceRef.trim(), rights = rights, license = license.trim(), provenance = CreationProvenance(prompt)) })
}.map { it.assets.single { asset -> asset.id == assetId } }
fun LaptopCore.editCreationCharacter(workspaceId: String, characterId: String, name: String, bible: String, assetIds: List<String>): Result<CreationCharacter> = updateCreationPlan(workspaceId) { plan ->
    require(plan.characters.any { it.id == characterId }) { "Unknown character in this project" }
    plan.copy(characters = plan.characters.map { if (it.id != characterId) it else it.copy(name = name.trim(), bible = bible, assetIds = assetIds) })
}.map { it.characters.single { character -> character.id == characterId } }
fun LaptopCore.editCreationAudio(workspaceId: String, audioId: String, kind: CreationAudioKind, name: String, script: String, assetId: String?): Result<CreationAudioSlot> = updateCreationPlan(workspaceId) { plan ->
    require(plan.audioSlots.any { it.id == audioId }) { "Unknown audio slot in this project" }
    plan.copy(audioSlots = plan.audioSlots.map { if (it.id != audioId) it else it.copy(kind = kind, name = name.trim(), script = script, assetId = assetId) })
}.map { it.audioSlots.single { slot -> slot.id == audioId } }
fun LaptopCore.renameCreationNode(workspaceId: String, nodeId: String, title: String): Result<CreationPlan> = updateCreationPlan(workspaceId) { plan ->
    require(plan.seasons.any { it.id == nodeId } || plan.episodes().any { it.id == nodeId } || plan.scenes().any { it.id == nodeId }) { "Unknown hierarchy node in this project" }
    plan.copy(seasons = plan.seasons.map { season -> season.copy(title = if (season.id == nodeId) title.trim() else season.title,
        episodes = season.episodes.map { episode -> episode.copy(title = if (episode.id == nodeId) title.trim() else episode.title,
            scenes = episode.scenes.map { if (it.id == nodeId) it.copy(title = title.trim()) else it }) }) })
}
