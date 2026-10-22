package com.dark.tool_neuron.studio

import java.net.URI

object VibePreviewPolicy {
    fun baseUrl(workspaceId: String, buildId: String): String {
        require(workspaceId.matches(Regex("ws-[a-f0-9]{10}")) && buildId.matches(Regex("vb-[a-f0-9]{32}")))
        return "https://$buildId.invalid/vibe/$workspaceId/$buildId/"
    }
    fun resolve(url: String, workspaceId: String, buildId: String): String? = runCatching {
        val base = URI(baseUrl(workspaceId, buildId))
        val uri = URI(url)
        if (uri.scheme != "https" || uri.rawAuthority != base.rawAuthority || uri.userInfo != null || !uri.path.startsWith(base.path)) return null
        val path = uri.path.removePrefix(base.path)
        if (path.length !in 1..160 || !path.matches(Regex("[A-Za-z0-9_./-]+")) ||
            path.split('/').any { it.isEmpty() || it.startsWith('.') }) return null
        path
    }.getOrNull()
    const val CONTENT_SECURITY_POLICY = "default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'none'; worker-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"
}
