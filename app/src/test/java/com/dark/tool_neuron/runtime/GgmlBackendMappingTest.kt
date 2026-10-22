package com.dark.tool_neuron.runtime

import com.dark.gguf_lib.models.GenerationEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class GgmlBackendMappingTest {
    @Test fun existingEngineEventsKeepTheirMeaningAcrossTheBoundary() {
        assertEquals(LocalGenerationEvent.Token("actual token"), GgmlBackend.mapEvent(GenerationEvent.Token("actual token")))
        assertEquals(LocalGenerationEvent.Error("actual error"), GgmlBackend.mapEvent(GenerationEvent.Error("actual error")))
        assertEquals(LocalGenerationEvent.Progress(0.25f), GgmlBackend.mapEvent(GenerationEvent.Progress(0.25f)))
        assertEquals(LocalGenerationEvent.Done, GgmlBackend.mapEvent(GenerationEvent.Done))
    }
}
