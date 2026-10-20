package com.niranjan.auroralivepaper.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * Extracts direct downloadable video URLs (.mp4) for Instagram Reels, Posts, and web pages.
 * Runs safely in a headless WebView on the Main thread and guarantees zero lingering resource
 * or battery consumption by immediately destroying the WebView upon completion or timeout.
 */
object WebVideoExtractor {

    private const val TIMEOUT_MS = 15000L

    /**
     * Extracts an Instagram shortcode from Reel or Post links.
     */
    fun extractInstagramCode(url: String): String? {
        val regex = Regex("""instagram\.com/(?:reel|reels|p|tv)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
        return regex.find(url)?.groupValues?.get(1)
    }

    /**
     * Resolves the direct video MP4 URL from an Instagram or web video link.
     * Uses lightweight fallback HTTP scraping first, then headless WebView if needed.
     */
    suspend fun resolveVideoUrl(context: Context, rawUrl: String): String? {
        val instaCode = extractInstagramCode(rawUrl)
        if (instaCode != null) {
            // First attempt: lightweight HTTP scrape of embed page
            val embedScraped = withContext(Dispatchers.IO) {
                tryScrapeInstagramEmbed(instaCode)
            }
            if (!embedScraped.isNullOrBlank()) {
                return embedScraped
            }

            // Second attempt: headless native WebView extraction on Main thread
            val targetUrl = "https://www.instagram.com/reel/$instaCode/"
            val webViewResult = withContext(Dispatchers.Main) {
                extractFromHeadlessWebView(context, targetUrl)
            }
            if (!webViewResult.isNullOrBlank()) {
                return webViewResult
            }

            // Third attempt: embed URL in headless WebView
            val embedUrl = "https://www.instagram.com/p/$instaCode/embed/captioned/"
            val embedWebViewResult = withContext(Dispatchers.Main) {
                extractFromHeadlessWebView(context, embedUrl)
            }
            if (!embedWebViewResult.isNullOrBlank()) {
                return embedWebViewResult
            }
        }

        return null
    }

    private fun tryScrapeInstagramEmbed(code: String): String? {
        return try {
            val url = URL("https://www.instagram.com/p/$code/embed/captioned/")
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            )
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                extractVideoUrlFromHtml(html)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVideoUrlFromHtml(html: String): String? {
        // Check for video_url in JSON / scripts
        val patterns = listOf(
            Regex(""""video_url"\s*:\s*"([^"]+)""""),
            Regex(""""video_versions"\s*:\s*\[\{"url"\s*:\s*"([^"]+)""""),
            Regex("""<video[^>]*src="([^"]+)""""),
            Regex("""property="og:video"\s*content="([^"]+)""""),
            Regex("""content="([^"]+)"\s*property="og:video"""")
        )

        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                var found = match.groupValues[1]
                // Unescape JSON unicode and backslashes
                found = found.replace("\\u0026", "&")
                    .replace("\\/", "/")
                    .replace("&amp;", "&")
                if (found.startsWith("http") && (found.contains(".mp4") || found.contains("cdninstagram.com") || found.contains("fbcdn.net"))) {
                    return found
                }
            }
        }
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractFromHeadlessWebView(context: Context, urlToLoad: String): String? =
        suspendCancellableCoroutine { continuation ->
            var webView: WebView? = null
            var isFinished = false
            val mainHandler = Handler(Looper.getMainLooper())

            fun cleanupAndReturn(resultUrl: String?) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacksAndMessages(null)
                try {
                    webView?.stopLoading()
                    webView?.clearCache(true)
                    webView?.destroy()
                } catch (_: Exception) {}
                webView = null

                if (continuation.isActive) {
                    continuation.resume(resultUrl)
                }
            }

            val timeoutRunnable = Runnable {
                cleanupAndReturn(null)
            }
            mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

            try {
                val wv = WebView(context.applicationContext)
                webView = wv

                val settings = wv.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.databaseEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

                wv.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: ""
                        if (isVideoMediaUrl(reqUrl)) {
                            mainHandler.post {
                                cleanupAndReturn(reqUrl)
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // In addition to network interception, query the DOM for video tag or open graph tags
                        wv.evaluateJavascript(
                            """
                            (function() {
                                var v = document.querySelector('video');
                                if (v && v.src && v.src.startsWith('http')) return v.src;
                                var source = document.querySelector('video > source');
                                if (source && source.src && source.src.startsWith('http')) return source.src;
                                var og = document.querySelector('meta[property="og:video"]');
                                if (og && og.content && og.content.startsWith('http')) return og.content;
                                return null;
                            })();
                            """.trimIndent()
                        ) { result ->
                            if (!result.isNullOrBlank() && result != "null") {
                                val clean = result.removeSurrounding("\"")
                                    .replace("\\u0026", "&")
                                    .replace("\\/", "/")
                                if (clean.startsWith("http")) {
                                    cleanupAndReturn(clean)
                                }
                            }
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        cleanupAndReturn(null)
                    }
                }

                wv.loadUrl(urlToLoad)

            } catch (e: Exception) {
                cleanupAndReturn(null)
            }
        }

    private fun isVideoMediaUrl(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""
        val path = uri.path?.lowercase() ?: ""

        val isCdn = host.contains("cdninstagram.com") || host.contains("fbcdn.net") || host.contains("instagram.")
        val isMp4 = path.endsWith(".mp4") || url.contains(".mp4") || url.contains("mime_type=video_mp4")

        return isCdn && isMp4
    }
}

