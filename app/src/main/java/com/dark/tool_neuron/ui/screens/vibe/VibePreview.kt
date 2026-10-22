package com.dark.tool_neuron.ui.screens.vibe

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.dark.tool_neuron.studio.VibePreviewPolicy
import com.dark.tool_neuron.studio.VibeStudio
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun VibePreview(
    studio: VibeStudio,
    workspaceId: String,
    buildId: String,
    modifier: Modifier = Modifier,
    onView: (WebView?) -> Unit,
    onStatus: (String) -> Unit,
    onFatal: () -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    blockNetworkLoads = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    databaseEnabled = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    setGeolocationEnabled(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    mediaPlaybackRequiresUserGesture = true
                    safeBrowsingEnabled = true
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) { request?.deny() }
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                            onStatus("Preview error: ${consoleMessage.message().take(300)}")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val allowed = request?.url?.toString()?.let { VibePreviewPolicy.resolve(it, workspaceId, buildId) } != null
                        if (!allowed) onStatus("Offline preview blocks navigation outside this build.")
                        return !allowed
                    }
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                        val path = request?.url?.toString()?.let { VibePreviewPolicy.resolve(it, workspaceId, buildId) }
                        if (request?.method != "GET" || path == null) return response("text/plain", "Blocked outside this build".toByteArray(), 403)
                        return try { response(mime(path), studio.preview(workspaceId, buildId, path), 200) }
                        catch (_: Exception) { response("text/plain", "Snapshot unavailable or changed. Close preview and rebuild.".toByteArray(), 409) }
                    }
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        onStatus("Preview process stopped. Reopen preview to recover.")
                        onFatal()
                        return true
                    }
                }
                onView(this)
                loadUrl(VibePreviewPolicy.baseUrl(workspaceId, buildId) + "index.html")
            }
        },
        onRelease = { view -> onView(null); view.stopLoading(); view.destroy() },
    )
}

private fun response(mime: String, bytes: ByteArray, status: Int) = WebResourceResponse(
    mime, "UTF-8", status, if (status == 200) "OK" else "Blocked",
    mapOf("Content-Security-Policy" to VibePreviewPolicy.CONTENT_SECURITY_POLICY,
        "Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff", "Referrer-Policy" to "no-referrer"),
    ByteArrayInputStream(bytes),
)
private fun mime(path: String): String = when (path.substringAfterLast('.').lowercase()) {
    "html" -> "text/html"; "css" -> "text/css"; "js" -> "text/javascript"; "json" -> "application/json"
    "svg" -> "image/svg+xml"; "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "webp" -> "image/webp"; "gif" -> "image/gif"
    else -> "text/plain"
}
fun captureVisiblePreview(view: WebView): ByteArray {
    check(view.width > 0 && view.height > 0) { "Wait for the preview to lay out before capturing" }
    val scale = min(1f, min(1280f / view.width, 1280f / view.height))
    val bitmap = Bitmap.createBitmap(max(1, (view.width * scale).toInt()), max(1, (view.height * scale).toInt()), Bitmap.Config.ARGB_8888)
    return try {
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        view.draw(canvas)
        ByteArrayOutputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() }
    } finally { bitmap.recycle() }
}
