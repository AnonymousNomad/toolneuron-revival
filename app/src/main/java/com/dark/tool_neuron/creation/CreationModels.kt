package com.dark.tool_neuron.creation

import kotlinx.serialization.Serializable

@Serializable enum class CreationKind { STORY, SERIES }
@Serializable enum class CreationAssetKind { IMAGE, VIDEO, AUDIO, REFERENCE }
@Serializable enum class AssetRights { UNKNOWN, OWNED, LICENSED, PUBLIC_DOMAIN }
@Serializable enum class CreationAudioKind { VOICE, MUSIC, SFX }
@Serializable data class CreationProvenance(val prompt: String, val author: String = "user", val providerId: String? = null, val modelRef: String? = null)
@Serializable data class CreationAsset(val id: String, val name: String, val kind: CreationAssetKind, val sourceRef: String,
    val rights: AssetRights, val license: String, val provenance: CreationProvenance, val referenceOnly: Boolean = true)
@Serializable data class CreationCharacter(val id: String, val name: String, val bible: String, val assetIds: List<String> = emptyList())
@Serializable data class CreationAudioSlot(val id: String, val kind: CreationAudioKind, val name: String, val script: String, val assetId: String? = null)
@Serializable data class CreationShot(val id: String, val ordinal: Int, val provenance: CreationProvenance, val durationMs: Long,
    val dialogue: String = "", val characterIds: List<String> = emptyList(), val assetIds: List<String> = emptyList(), val audioSlotIds: List<String> = emptyList()) {
    val prompt: String get() = provenance.prompt
}
@Serializable data class CreationScene(val id: String, val ordinal: Int, val title: String, val shots: List<CreationShot> = emptyList())
@Serializable data class CreationEpisode(val id: String, val ordinal: Int, val title: String, val scenes: List<CreationScene> = emptyList())
@Serializable data class CreationSeason(val id: String, val ordinal: Int, val title: String, val episodes: List<CreationEpisode> = emptyList())
@Serializable data class CreationPlan(val kind: CreationKind, val synopsis: String, val seasons: List<CreationSeason>,
    val characters: List<CreationCharacter> = emptyList(), val assets: List<CreationAsset> = emptyList(), val audioSlots: List<CreationAudioSlot> = emptyList()) {
    fun episodes() = seasons.flatMap { it.episodes }
    fun scenes() = episodes().flatMap { it.scenes }
    fun shots() = scenes().flatMap { it.shots }
    fun validate() {
        require(synopsis.length in 1..4000 && seasons.size in 1..10 && episodes().size <= 20 && scenes().size <= 100 && shots().size <= 500)
        require(characters.size <= 100 && assets.size <= 200 && audioSlots.size <= 200)
        val ids = seasons.map { it.id } + episodes().map { it.id } + scenes().map { it.id } + shots().map { it.id } + characters.map { it.id } + assets.map { it.id } + audioSlots.map { it.id }
        require(ids.distinct().size == ids.size && ids.all { it.matches(Regex("[a-zA-Z0-9_-]{1,80}")) })
        fun ordered(values: List<Pair<Int, String>>) {
            require(values.map { it.first }.distinct().size == values.size && values.all { it.first > 0 && it.second.isNotBlank() && it.second.length <= 128 })
        }
        ordered(seasons.map { it.ordinal to it.title })
        seasons.forEach { ordered(it.episodes.map { episode -> episode.ordinal to episode.title }) }
        episodes().forEach { ordered(it.scenes.map { scene -> scene.ordinal to scene.title }) }
        scenes().forEach { scene -> require(scene.shots.map { it.ordinal }.distinct().size == scene.shots.size && scene.shots.all { it.ordinal > 0 }) }
        val assetIds = assets.map { it.id }.toSet()
        val characterIds = characters.map { it.id }.toSet()
        val audioIds = audioSlots.map { it.id }.toSet()
        characters.forEach { require(it.name.isNotBlank() && it.name.length <= 128 && it.bible.length <= 4000 && it.assetIds.distinct().size == it.assetIds.size && assetIds.containsAll(it.assetIds)) }
        assets.forEach { require(it.name.isNotBlank() && it.name.length <= 128 && it.sourceRef.length <= 2048 && it.license.length <= 1000 && it.referenceOnly); provenance(it.provenance) }
        audioSlots.forEach { require(it.name.isNotBlank() && it.name.length <= 128 && it.script.length <= 4000 && (it.assetId == null || it.assetId in assetIds)); require(it.assetId == null || assets.single { asset -> asset.id == it.assetId }.kind in setOf(CreationAssetKind.AUDIO, CreationAssetKind.REFERENCE)) }
        shots().forEach { shot ->
            provenance(shot.provenance)
            require(shot.durationMs in 1000..600_000 && shot.dialogue.length <= 4000)
            require(shot.characterIds.distinct().size == shot.characterIds.size && characterIds.containsAll(shot.characterIds))
            require(shot.assetIds.distinct().size == shot.assetIds.size && assetIds.containsAll(shot.assetIds))
            require(shot.audioSlotIds.distinct().size == shot.audioSlotIds.size && audioIds.containsAll(shot.audioSlotIds))
        }
    }
    companion object {
        const val CAPABILITY = "media.creation.plan@1"
        fun seed(kind: CreationKind, synopsis: String): CreationPlan {
            fun id(prefix: String) = "$prefix-" + java.util.UUID.randomUUID().toString().replace("-", "")
            val shot = CreationShot(id("shot"), 1, CreationProvenance(synopsis.trim()), 6000)
            val scene = CreationScene(id("scene"), 1, "Opening", listOf(shot))
            val episode = CreationEpisode(id("episode"), 1, "Episode 1", listOf(scene))
            return CreationPlan(kind, synopsis.trim(), listOf(CreationSeason(id("season"), 1, "Season 1", listOf(episode)))).also { it.validate() }
        }
        private fun provenance(value: CreationProvenance) {
            require(value.prompt.length <= 4000 && value.author.isNotBlank() && value.author.length <= 128)
            require(value.providerId == null && value.modelRef == null) { "Provider outputs require an implemented verified adapter; current plans are manual" }
        }
    }
}
