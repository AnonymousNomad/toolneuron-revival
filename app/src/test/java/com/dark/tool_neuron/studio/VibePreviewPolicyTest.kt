package com.dark.tool_neuron.studio

import org.junit.Assert.*
import org.junit.Test

class VibePreviewPolicyTest {
    private val workspace = "ws-0123456789"
    private val build = "vb-" + "a".repeat(32)
    @Test fun onlyResourcesFromTheSelectedBuildAreAdmitted() {
        val base = VibePreviewPolicy.baseUrl(workspace, build)
        assertEquals("index.html", VibePreviewPolicy.resolve(base + "index.html", workspace, build))
        assertEquals("assets/app.js", VibePreviewPolicy.resolve(base + "assets/app.js?v=1", workspace, build))
        assertNull(VibePreviewPolicy.resolve(base.replace(workspace, "ws-9876543210") + "index.html", workspace, build))
        assertNull(VibePreviewPolicy.resolve(base.replace(build, "vb-" + "b".repeat(32)) + "index.html", workspace, build))
    }
    @Test fun networkFilesystemTraversalAndAuthorityConfusionAreDenied() {
        val base = VibePreviewPolicy.baseUrl(workspace, build)
        val host = java.net.URI(base).host
        for (url in listOf(
            "https://example.com/index.html", "file:///data/user/0/private", "content://documents/1",
            base + "../secret", base + "%2e%2e/secret", base + "assets/%2e%2e/index.html",
            base.replace("https://", "http://") + "index.html",
            base.replace(host, "$host.evil.test") + "index.html",
            base.replace(host, "user@$host") + "index.html",
            base.replace(host, "$host:8080") + "index.html",
        )) assertNull(url, VibePreviewPolicy.resolve(url, workspace, build))
    }
}
