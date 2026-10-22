package com.dark.tool_neuron.creation

import com.dark.tool_neuron.laptop.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CreationTest {
    @get:Rule val temp = TemporaryFolder()
    private class Store : LaptopPersistence {
        var bytes: ByteArray? = null
        var reject = false
        override fun load() = bytes
        override fun save(bytes: ByteArray) { check(!reject); this.bytes = bytes }
        override fun quarantineCorrupt(bytes: ByteArray) = Unit
    }
    private fun core(store: Store = Store()) = LaptopCore(store, temp.root).also { it.setPermission(CreationPlan.CAPABILITY, true).getOrThrow() }
    @Test fun storyHierarchyLivesInOneCanonicalWorkspaceAcrossRestart() {
        val store = Store()
        val core = core(store)
        val project = core.createCreation("Series", "A crew explores a quiet station.", CreationKind.SERIES).getOrThrow()
        val plan = project.creation!!
        assertEquals(1, plan.seasons.size)
        assertEquals(1, plan.seasons.single().episodes.single().scenes.single().shots.size)
        val resumed = LaptopCore(store, temp.root).state.value
        assertEquals(1, resumed.workspaces.size)
        assertEquals(plan, resumed.workspaces.single().creation)
        assertEquals(project.id, resumed.activeWorkspaceId)
    }
    @Test fun assetsCharactersAudioAndAdditionalEpisodeProduceAnHonestManifest() {
        val core = core()
        val project = core.createCreation("Story", "A pilot returns home.", CreationKind.SERIES).getOrThrow()
        val asset = core.registerCreationAsset(project.id, "Pilot reference", CreationAssetKind.IMAGE, "owner-reference:pilot", AssetRights.OWNED, "Original artwork", "manual reference").getOrThrow()
        val character = core.addCreationCharacter(project.id, "Pilot", "Blue coat; patient navigator", listOf(asset.id)).getOrThrow()
        val audio = core.addCreationAudio(project.id, CreationAudioKind.VOICE, "Pilot voice", "We are home.", null).getOrThrow()
        val episode = core.addCreationEpisode(project.id, project.creation!!.seasons.single().id, "Homecoming").getOrThrow()
        val scene = core.addCreationScene(project.id, episode.id, "Dock").getOrThrow()
        core.addCreationShot(project.id, scene.id, "Pilot in blue coat enters dock", 7000, "We are home.", listOf(character.id), listOf(asset.id), listOf(audio.id)).getOrThrow()
        val manifest = core.creationManifest(project.id).getOrThrow()
        assertEquals(project.id, manifest.workspaceId)
        assertEquals(13000L, manifest.plannedDurationMs)
        assertTrue(manifest.jobs.isNotEmpty())
        assertTrue(manifest.jobs.all { it.state == CreationJobState.BLOCKED })
        assertTrue(manifest.plan.assets.single().provenance.prompt == "manual reference")
        assertTrue(manifest.jobs.all { job -> job.dependencies.all { ref -> manifest.jobs.any { it.id == ref } } })
    }
    @Test fun crossProjectReferencesAndFailedSavesCannotChangeThePlan() {
        val store = Store()
        val core = core(store)
        val first = core.createCreation("One", "First plan", CreationKind.STORY).getOrThrow()
        val second = core.createCreation("Two", "Second plan", CreationKind.STORY).getOrThrow()
        val foreignScene = second.creation!!.seasons.single().episodes.single().scenes.single()
        assertTrue(core.addCreationShot(first.id, foreignScene.id, "wrong project", 1000).isFailure)
        assertTrue(core.addCreationCharacter(first.id, "Invalid", "notes", listOf("foreign-asset")).isFailure)
        val before = core.state.value
        store.reject = true
        assertTrue(core.addCreationCharacter(first.id, "Name", "notes", emptyList()).isFailure)
        assertEquals(before, core.state.value)
    }
    @Test fun absentRenderProvidersNeverGenerateOrFallBack() = runBlocking {
        for (provider in CreationProviders.all) {
            assertFalse(provider.available)
            assertTrue(provider.generate(CreationProviderRequest("project", "shot", "prompt", emptyList())).isFailure)
        }
    }
    @Test fun planningIsDeniedByDefaultAndContinuityDoesNotInventRights() {
        val core = LaptopCore(Store(), temp.root)
        assertTrue(core.createCreation("Denied", "No grant", CreationKind.STORY).isFailure)
        assertTrue(core.state.value.workspaces.isEmpty())
        core.setPermission(CreationPlan.CAPABILITY, true).getOrThrow()
        val project = core.createCreation("Draft", "A small story", CreationKind.STORY).getOrThrow()
        core.registerCreationAsset(project.id, "Unknown reference", CreationAssetKind.REFERENCE, "", AssetRights.UNKNOWN, "", "").getOrThrow()
        val manifest = core.creationManifest(project.id).getOrThrow()
        assertTrue(manifest.checks.any { it.id.startsWith("rights:") && !it.passed })
        assertTrue(manifest.jobs.none { it.state.name == "SUCCEEDED" })
    }
    @Test fun musicAndEffectsDoNotCreateLipSyncJobs() {
        val core = core()
        val project = core.createCreation("Music", "A quiet landscape", CreationKind.STORY).getOrThrow()
        val slot = core.addCreationAudio(project.id, CreationAudioKind.MUSIC, "Score", "Quiet instrumental", null).getOrThrow()
        val shot = project.creation!!.shots().single()
        core.editCreationShot(project.id, shot.id, shot.prompt, shot.durationMs, "", emptyList(), emptyList(), listOf(slot.id)).getOrThrow()
        val manifest = core.creationManifest(project.id).getOrThrow()
        assertTrue(manifest.jobs.none { it.kind == CreationJobKind.LIPSYNC })
        assertTrue(manifest.jobs.filter { it.kind == CreationJobKind.ASSEMBLY }.any { "${slot.id}:audio" in it.dependencies })
    }
    @Test fun editingRegistryAndHierarchyPreservesReferencedIdentities() {
        val core = core()
        val project = core.createCreation("Edit", "A small story", CreationKind.STORY).getOrThrow()
        val asset = core.registerCreationAsset(project.id, "Reference", CreationAssetKind.IMAGE, "owner:ref", AssetRights.UNKNOWN, "", "first").getOrThrow()
        val character = core.addCreationCharacter(project.id, "Name", "Old bible", listOf(asset.id)).getOrThrow()
        core.editCreationAsset(project.id, asset.id, "Approved ref", CreationAssetKind.IMAGE, "owner:ref", AssetRights.OWNED, "Original", "revised provenance").getOrThrow()
        core.editCreationCharacter(project.id, character.id, "New name", "New bible", listOf(asset.id)).getOrThrow()
        val scene = project.creation!!.scenes().single()
        core.renameCreationNode(project.id, scene.id, "New scene title").getOrThrow()
        val plan = core.state.value.workspaces.single().creation!!
        assertEquals(character.id, plan.characters.single().id)
        assertEquals(listOf(asset.id), plan.characters.single().assetIds)
        assertEquals("New scene title", plan.scenes().single().title)
        assertEquals("revised provenance", plan.assets.single().provenance.prompt)
    }

    @Test fun residentCreationLookupUsesOnlyItsPinnedProjectAndMarksItAsProjectData() {
        val core = core()
        val first = core.createCreation("Alpha", "Alpha private story", CreationKind.STORY).getOrThrow()
        core.createCreation("Beta", "Beta private story", CreationKind.STORY).getOrThrow()
        val lookup = com.dark.tool_neuron.laptop.resident.LaptopLookupService(core, { emptyList() }, { _, _ -> null }, { emptyList() })
        val result = lookup.execute(com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.CREATION_CURRENT), first.id)
        assertEquals(com.dark.tool_neuron.laptop.resident.LookupTrust.PROJECT_DATA, result.trust)
        assertTrue(result.summary.contains("Alpha private story"))
        assertFalse(result.summary.contains("Beta"))
        assertTrue(first.id in result.refs)
        assertTrue(lookup.execute(com.dark.tool_neuron.laptop.resident.LaptopLookupRequest(com.dark.tool_neuron.laptop.resident.LookupKind.CREATION_CURRENT), null).refs.isEmpty())
    }

    @Test fun explicitVoiceSlotOverridesAutomaticDialogueVoice() {
        val core = core()
        val project = core.createCreation("Voice", "A quiet arrival", CreationKind.STORY).getOrThrow()
        val slot = core.addCreationAudio(project.id, CreationAudioKind.VOICE, "Narrator", "Welcome home", null).getOrThrow()
        val shot = project.creation!!.shots().single()
        core.editCreationShot(project.id, shot.id, shot.prompt, 6000, "Welcome home", emptyList(), emptyList(), listOf(slot.id)).getOrThrow()
        val manifest = core.creationManifest(project.id).getOrThrow()
        assertEquals(1, manifest.jobs.count { it.kind == CreationJobKind.VOICE })
        assertEquals(listOf("${shot.id}:video", "${slot.id}:audio"), manifest.jobs.single { it.kind == CreationJobKind.LIPSYNC }.dependencies)
    }
    @Test fun audioReferenceCannotPassAVisualCharacterContinuityCheck() {
        val core = core()
        val project = core.createCreation("Character", "A story", CreationKind.STORY).getOrThrow()
        val audio = core.registerCreationAsset(project.id, "Voice ref", CreationAssetKind.AUDIO, "owner:voice", AssetRights.OWNED, "Original", "manual").getOrThrow()
        val character = core.addCreationCharacter(project.id, "Narrator", "Consistent appearance", listOf(audio.id)).getOrThrow()
        assertFalse(core.creationManifest(project.id).getOrThrow().checks.single { it.id == "character:${character.id}" }.passed)
    }

}
