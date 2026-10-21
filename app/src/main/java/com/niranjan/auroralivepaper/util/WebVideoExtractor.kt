package com.niranjan.auroralivepaper.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
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

sealed class InstagramExtractResult {
    data class Success(val url: String) : InstagramExtractResult()
    object PrivateOrLogin : InstagramExtractResult()
    object NotFound : InstagramExtractResult()
    object Blocked : InstagramExtractResult()
    object NetworkError : InstagramExtractResult()
    object Unknown : InstagramExtractResult()
}

/**
 * Robust extractor for Instagram Reels/Posts and direct video links.
 * Uses high-priority crawler scraping (Facebook/Googlebot) which returns SSR JSON containing
 * signed progressive MP4 video streams, with Headless WebView as a fallback.
 */
object WebVideoExtractor {

    private const val TAG = "WebVideoExtractor"
    private const val TIMEOUT_MS = 12000L

    fun extractInstagramCode(url: String): String? {
        val regex = Regex("""instagram\.com/(?:[a-zA-Z0-9_.]+/)?(?:reel|reels|p|tv|share/reel)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
        val code = regex.find(url)?.groupValues?.get(1)
        return code?.trimEnd('/')
    }

    suspend fun resolveVideoUrl(context: Context, rawUrl: String): InstagramExtractResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return InstagramExtractResult.Unknown

        val instaCode = extractInstagramCode(trimmed)
        if (instaCode == null) {
            return InstagramExtractResult.Success(trimmed)
        }

        // Strategy 1: Facebook OpenGraph ExternalHit on reel URL
        val ogResult = withContext(Dispatchers.IO) {
            scrapePage(
                targetUrl = "https://www.instagram.com/reel/$instaCode/",
                userAgent = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
                strategyName = "FacebookOG"
            )
        }
        if (ogResult is InstagramExtractResult.Success) {
            Log.d(TAG, "Extraction succeeded via Strategy 1 (FacebookOG)")
            return ogResult
        }

        // Strategy 2: Googlebot Crawler on reel URL
        val googlebotResult = withContext(Dispatchers.IO) {
            scrapePage(
                targetUrl = "https://www.instagram.com/reel/$instaCode/",
                userAgent = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                strategyName = "Googlebot"
            )
        }
        if (googlebotResult is InstagramExtractResult.Success) {
            Log.d(TAG, "Extraction succeeded via Strategy 2 (Googlebot)")
            return googlebotResult
        }

        // Strategy 3: Facebook OpenGraph on /p/ post URL
        val postResult = withContext(Dispatchers.IO) {
            scrapePage(
                targetUrl = "https://www.instagram.com/p/$instaCode/",
                userAgent = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
                strategyName = "FacebookPost"
            )
        }
        if (postResult is InstagramExtractResult.Success) {
            Log.d(TAG, "Extraction succeeded via Strategy 3 (FacebookPost)")
            return postResult
        }

        // Strategy 4: Headless WebView
        val webViewResult = withContext(Dispatchers.Main) {
            extractFromHeadlessWebView(context, "https://www.instagram.com/reel/$instaCode/")
        }
        if (webViewResult is InstagramExtractResult.Success) {
            Log.d(TAG, "Extraction succeeded via Strategy 4 (HeadlessWebView)")
            return webViewResult
        }

        // If all strategies failed, evaluate the most descriptive error
        val results = listOf(ogResult, googlebotResult, postResult, webViewResult)
        val finalResult = when {
            results.any { it is InstagramExtractResult.NotFound } -> InstagramExtractResult.NotFound
            results.any { it is InstagramExtractResult.Blocked } -> InstagramExtractResult.Blocked
            results.any { it is InstagramExtractResult.PrivateOrLogin } -> InstagramExtractResult.PrivateOrLogin
            results.all { it is InstagramExtractResult.NetworkError } -> InstagramExtractResult.NetworkError
            else -> InstagramExtractResult.Unknown
        }

        Log.d(TAG, "All strategies completed. Final result: $finalResult")
        return finalResult
    }

    private fun scrapePage(targetUrl: String, userAgent: String, strategyName: String): InstagramExtractResult {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            val responseCode = conn.responseCode
            val finalUrl = conn.url?.toString() ?: ""
            Log.d(TAG, "Strategy [$strategyName]: HTTP $responseCode, finalUrl=$finalUrl")

            if (finalUrl.contains("/accounts/login") || responseCode == 401) {
                return InstagramExtractResult.PrivateOrLogin
            }
            if (responseCode == 404) {
                return InstagramExtractResult.NotFound
            }
            if (responseCode == 429 || responseCode == 403) {
                return InstagramExtractResult.Blocked
            }

            val html = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else ""

            if (html.isBlank()) {
                return InstagramExtractResult.Unknown
            }

            val foundUrl = extractVideoUrlFromHtml(html)
            if (!foundUrl.isNullOrBlank()) {
                Log.d(TAG, "Strategy [$strategyName]: Video URL found, length=${foundUrl.length}")
                return InstagramExtractResult.Success(foundUrl)
            }

            if (html.contains("Sorry, this page isn't available") ||
                html.contains("The link you followed may be broken") ||
                html.contains("page may have been removed")) {
                return InstagramExtractResult.NotFound
            }

            return InstagramExtractResult.Unknown
        } catch (e: Exception) {
            Log.w(TAG, "Strategy [$strategyName] error: ${e.message}")
            return InstagramExtractResult.NetworkError
        } finally {
            conn?.disconnect()
        }
    }

    fun extractVideoUrlFromHtml(html: String): String? {
        val unescaped = html.replace("\\/", "/").replace("\\u0026", "&").replace("&amp;", "&")

        // 1. High priority: Extract from "video_versions": [...] array in SSR JSON
        val vvRegex = Regex(""""video_versions"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
        val vvMatch = vvRegex.find(unescaped)
        if (vvMatch != null) {
            val arrayContent = vvMatch.groupValues[1]
            val urlRegex = Regex(""""url"\s*:\s*"([^"]+)"""")
            val matches = urlRegex.findAll(arrayContent)
            for (m in matches) {
                val clean = m.groupValues[1]
                    .replace("\\/", "/")
                    .replace("\\u0026", "&")
                    .replace("&amp;", "&")
                if (clean.startsWith("http") && clean.contains(".mp4") && !clean.contains("_audio")) {
                    return clean
                }
            }
        }

        // 2. Direct "video_url" field
        val vuRegex = Regex(""""video_url"\s*:\s*"([^"]+)"""")
        val vuMatch = vuRegex.find(unescaped)
        if (vuMatch != null) {
            val clean = vuMatch.groupValues[1]
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("&amp;", "&")
            if (clean.startsWith("http") && !clean.contains("_audio")) {
                return clean
            }
        }

        // 3. Direct "playback_url" field
        val puRegex = Regex(""""playback_url"\s*:\s*"([^"]+)"""")
        val puMatch = puRegex.find(unescaped)
        if (puMatch != null) {
            val clean = puMatch.groupValues[1]
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("&amp;", "&")
            if (clean.startsWith("http") && !clean.contains("_audio")) {
                return clean
            }
        }

        // 4. Signed CDN MP4 URL (must contain oe= or oh= token to avoid 403 Forbidden on DASH BaseURLs)
        val signedRegex = Regex("""https://[^\s"<>\\]+\.mp4\?[^\s"<>\\]*(?:oe=|oh=)[^\s"<>\\]*""", RegexOption.IGNORE_CASE)
        for (m in signedRegex.findAll(unescaped)) {
            val url = m.value
            if (!url.contains("_audio") &&
                (url.contains("cdninstagram.com") || url.contains("fbcdn.net") || url.contains("instagram"))) {
                return url
            }
        }

        // 5. Fallback generic CDN MP4
        val genericRegex = Regex("""https://[^\s"<>\\]+\.mp4[^\s"<>\\]*""", RegexOption.IGNORE_CASE)
        for (m in genericRegex.findAll(unescaped)) {
            val url = m.value
            if (!url.contains("_audio") &&
                (url.contains("cdninstagram.com") || url.contains("fbcdn.net") || url.contains("instagram"))) {
                return url
            }
        }

        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractFromHeadlessWebView(context: Context, urlToLoad: String): InstagramExtractResult =
        suspendCancellableCoroutine { continuation ->
            var webView: WebView? = null
            var isFinished = false
            var hitLoginWall = false
            val mainHandler = Handler(Looper.getMainLooper())

            fun cleanupAndReturn(result: InstagramExtractResult) {
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
                    continuation.resume(result)
                }
            }

            val timeoutRunnable = Runnable {
                Log.d(TAG, "Strategy [HeadlessWebView]: Timeout")
                if (hitLoginWall) {
                    cleanupAndReturn(InstagramExtractResult.PrivateOrLogin)
                } else {
                    cleanupAndReturn(InstagramExtractResult.Unknown)
                }
            }
            mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

            try {
                val wv = WebView(context.applicationContext)
                webView = wv

                val settings = wv.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

                val checkDomRunnable = object : Runnable {
                    var attempts = 0
                    override fun run() {
                        if (isFinished || webView == null) return
                        attempts++
                        wv.evaluateJavascript(
                            """
                            (function() {
                                var v = document.querySelector('video');
                                if (v && v.src && v.src.startsWith('http')) return v.src;
                                var sources = document.querySelectorAll('video source');
                                for (var i = 0; i < sources.length; i++) {
                                    if (sources[i].src && sources[i].src.startsWith('http')) return sources[i].src;
                                }
                                var html = document.documentElement.innerHTML;
                                var m = html.match(/"video_versions":\[\{.*?"url":"([^"]+)"/);
                                if (m && m[1]) return m[1];
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
                                    .replace("&amp;", "&")
                                if (clean.startsWith("http") && !clean.contains("_audio")) {
                                    Log.d(TAG, "Strategy [HeadlessWebView]: Video found from DOM evaluation")
                                    cleanupAndReturn(InstagramExtractResult.Success(clean))
                                    return@evaluateJavascript
                                }
                            }
                            if (attempts < 12 && !isFinished) {
                                mainHandler.postDelayed(this, 500)
                            }
                        }
                    }
                }

                wv.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString() ?: ""
                        if (reqUrl.contains("/accounts/login") || reqUrl.contains("login/")) {
                            hitLoginWall = true
                            return true
                        }
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: ""
                        if (isVideoMediaUrl(reqUrl)) {
                            mainHandler.post {
                                Log.d(TAG, "Strategy [HeadlessWebView]: Video intercepted via network request")
                                cleanupAndReturn(InstagramExtractResult.Success(reqUrl))
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        mainHandler.post(checkDomRunnable)
                    }
                }

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        cleanupAndReturn(InstagramExtractResult.Unknown)
                    }
                }

                wv.loadUrl(urlToLoad)

            } catch (e: Exception) {
                Log.w(TAG, "Strategy [HeadlessWebView] exception: ${e.message}")
                cleanupAndReturn(InstagramExtractResult.Unknown)
            }
        }

    private fun isVideoMediaUrl(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        if (url.contains("audio", ignoreCase = true) && !url.contains("video", ignoreCase = true)) return false

        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""
        val path = uri.path?.lowercase() ?: ""

        val isCdn = host.contains("cdninstagram.com") || host.contains("fbcdn.net") || host.contains("instagram")
        val isMp4 = path.endsWith(".mp4") || url.contains(".mp4") || url.contains("mime_type=video_mp4")

        return isCdn && isMp4
    }
}
