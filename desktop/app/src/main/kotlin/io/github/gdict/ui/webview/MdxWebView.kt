package io.github.gdict.ui.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.core.GdictLogger
import kotlinx.coroutines.delay
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.IProgressHandler
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefCallback
import org.cef.callback.CefQueryCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.callback.CefSchemeRegistrar
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.awt.BorderLayout
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.Base64
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

private val log = GdictLogger.get()

private var cefApp: CefApp? = null
private var cefClient: CefClient? = null
private var cefInitialized = false
private val cefLock = Any()

fun preInitCef(): Boolean = ensureCefInitialized()

fun preCreateBrowserPanel() {
    if (!ensureCefInitialized()) return
    GlobalBrowserManager.getOrCreatePanel()
}

fun isCefInitialized(): Boolean = synchronized(cefLock) { cefInitialized }

private fun ensureCefInitialized(): Boolean {
    synchronized(cefLock) {
        if (cefInitialized && cefApp != null && cefClient != null) return true

        try {
            log.i("MdxWebView", "Initializing JCEF...")

            val appDir = File(System.getProperty("compose.application.app.dir")
                ?: System.getProperty("user.dir"))
            val bundledJcef = File(appDir, "jcef-bundle")
            val userJcefDir = File(System.getProperty("user.home"), ".gdict${File.separator}jcef-bundle")

            val jcefDir = if (bundledJcef.exists() && File(bundledJcef, "install.lock").exists()) {
                log.i("MdxWebView", "Using bundled JCEF: $bundledJcef")
                bundledJcef
            } else if (File(userJcefDir, "install.lock").exists()) {
                log.i("MdxWebView", "Using user JCEF dir: $userJcefDir")
                userJcefDir
            } else {
                log.i("MdxWebView", "No JCEF bundle found, will download to: $userJcefDir")
                userJcefDir
            }
            jcefDir.mkdirs()

            val builder = CefAppBuilder()
            builder.setInstallDir(jcefDir)
            builder.setProgressHandler(IProgressHandler { state, percent ->
                log.i("MdxWebView", "JCEF setup: $state ($percent%)")
            })

            builder.addJcefArgs(
                "--disable-gpu",
                "--disable-gpu-compositing",
                "--disable-software-rasterizer",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-features=ChromeWhatsNewUI"
            )

            builder.getCefSettings().windowless_rendering_enabled = false
            builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_INFO

            builder.setAppHandler(object : MavenCefAppHandlerAdapter() {
                override fun onRegisterCustomSchemes(registrar: CefSchemeRegistrar) {
                    registrar.addCustomScheme("mdxres", true, true, false, false, true, false, true)
                    registrar.addCustomScheme("entry", true, false, false, false, false, false, false)
                    registrar.addCustomScheme("sound", true, false, false, false, false, false, false)
                }

                override fun stateHasChanged(state: CefApp.CefAppState) {
                    log.i("MdxWebView", "JCEF state changed: $state")
                    if (state == CefApp.CefAppState.TERMINATED) {
                        cefInitialized = false
                        cefApp = null
                        cefClient = null
                    }
                }
            })

            val app: CefApp
            val client: CefClient
            try {
                log.i("MdxWebView", "Calling builder.build()...")
                app = builder.build()
                log.i("MdxWebView", "builder.build() complete, creating client...")
                client = app.createClient()
                log.i("MdxWebView", "client created, registering schemes...")
            } catch (e: Throwable) {
                log.e("MdxWebView", "builder.build/createClient FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                throw e
            }

            app.registerSchemeHandlerFactory("mdxres", null, MdxResourceSchemeFactory)
            app.registerSchemeHandlerFactory("entry", null, EntrySchemeFactory)
            app.registerSchemeHandlerFactory("sound", null, SoundSchemeFactory)

            cefApp = app
            cefClient = client
            cefInitialized = true

            log.i("MdxWebView", "JCEF initialized successfully")
            return true
        } catch (e: Throwable) {
            log.e("MdxWebView", "JCEF initialization FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            return false
        }
    }
}

private var currentRepository: DictionaryRepository? = null
private var currentOnEntryClick: ((String) -> Unit)? = null
private var currentOnPlayAudio: ((String) -> Unit)? = null

private object MdxResourceSchemeFactory : CefSchemeHandlerFactory {
    override fun create(browser: CefBrowser, frame: CefFrame, schemeName: String, request: CefRequest): org.cef.handler.CefResourceHandler {
        return MdxResourceHandler()
    }
}

private object EntrySchemeFactory : CefSchemeHandlerFactory {
    override fun create(browser: CefBrowser, frame: CefFrame, schemeName: String, request: CefRequest): org.cef.handler.CefResourceHandler {
        return EntryResourceHandler()
    }
}

private object SoundSchemeFactory : CefSchemeHandlerFactory {
    override fun create(browser: CefBrowser, frame: CefFrame, schemeName: String, request: CefRequest): org.cef.handler.CefResourceHandler {
        return SoundResourceHandler()
    }
}

private class MdxResourceHandler : CefResourceHandlerAdapter() {
    private var inputStream: InputStream? = null
    private var mimeType: String = "application/octet-stream"
    private var contentLength: Int = 0

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        val url = request.url ?: return false.also { callback.cancel() }
        val repo = currentRepository ?: return false.also { callback.cancel() }

        try {
            val path = URLDecoder.decode(url.removePrefix("mdxres://"), "UTF-8")
            val data = resolveResourcePath(path, repo)

            if (data != null) {
                inputStream = ByteArrayInputStream(data)
                mimeType = mimeTypeForPath(path.lowercase())
                contentLength = data.size
                log.i("MdxWebView", "mdxres:// resolved: $path -> ${data.size} bytes ($mimeType)")
            } else {
                inputStream = ByteArrayInputStream(ByteArray(0))
                mimeType = if (isImageExtension(path.lowercase())) "image/png" else "text/plain"
                contentLength = 0
                log.w("MdxWebView", "mdxres:// not found: $path")
            }
        } catch (e: Throwable) {
            log.e("MdxWebView", "mdxres:// error: ${e.message}")
            inputStream = ByteArrayInputStream(ByteArray(0))
            mimeType = "text/plain"
            contentLength = 0
        }

        callback.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        response.mimeType = mimeType
        response.status = 200
        responseLength.set(contentLength)
    }

    override fun readResponse(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        val stream = inputStream ?: return false.also { bytesRead.set(0) }
        val read = stream.read(dataOut, 0, bytesToRead)
        if (read <= 0) {
            bytesRead.set(0)
            return false
        }
        bytesRead.set(read)
        return true
    }
}

private class EntryResourceHandler : CefResourceHandlerAdapter() {
    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        val url = request.url ?: return false.also { callback.cancel() }
        val entry = URLDecoder.decode(
            url.removePrefix("entry://").removePrefix("bword://"), "UTF-8"
        ).substringBefore("#")

        log.i("MdxWebView", "entry:// fallback: $entry")
        SwingUtilities.invokeLater {
            currentOnEntryClick?.invoke(entry)
        }
        callback.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        response.mimeType = "text/html"
        response.status = 204
        response.statusText = "No Content"
        responseLength.set(0)
    }

    override fun readResponse(data: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        bytesRead.set(0)
        return false
    }
}

private class SoundResourceHandler : CefResourceHandlerAdapter() {
    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        val url = request.url ?: return false.also { callback.cancel() }
        val path = URLDecoder.decode(url.removePrefix("sound://"), "UTF-8")

        log.i("MdxWebView", "sound:// fallback: $path")
        SwingUtilities.invokeLater {
            currentOnPlayAudio?.invoke(path)
        }
        callback.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef) {
        response.mimeType = "text/html"
        response.status = 204
        response.statusText = "No Content"
        responseLength.set(0)
    }

    override fun readResponse(data: ByteArray, bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean {
        bytesRead.set(0)
        return false
    }
}

private fun resolveResourcePath(path: String, repository: DictionaryRepository): ByteArray? {
    val cleanPath = path.replace(Regex("[\\x00-\\x1f\\x7f]"), "").trimEnd('/')
    val normalizedPath = cleanPath.replace("/", "\\")
    val trimmedPath = normalizedPath.trimStart('\\')
    val candidates = buildList {
        add("\\$trimmedPath")
        add("\\\\$trimmedPath")
        val fileName = cleanPath.substringAfterLast("/")
        if (fileName.isNotEmpty()) {
            add("\\$fileName")
        }
        val pathWithForwardSlash = "/" + cleanPath.trimStart('/')
        add(pathWithForwardSlash)
    }
    log.i("MdxWebView", "resolveResourcePath: path='$cleanPath', candidates=$candidates")
    for (candidate in candidates) {
        try {
            val data = repository.getAudioResourceByPathSync(candidate)
            if (data != null) {
                log.i("MdxWebView", "resolveResourcePath: FOUND '$candidate' (${data.size} bytes)")
                return data
            }
        } catch (e: Exception) {
            log.w("MdxWebView", "resolveResourcePath: exception for '$candidate': ${e.message}")
        }
    }
    log.w("MdxWebView", "resolveResourcePath: NOT FOUND for '$cleanPath'")
    return null
}

private fun isImageExtension(lowerPath: String): Boolean {
    return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
            lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
            lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp")
}

private fun mimeTypeForPath(lowerPath: String): String = when {
    lowerPath.endsWith(".png") -> "image/png"
    lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") -> "image/jpeg"
    lowerPath.endsWith(".gif") -> "image/gif"
    lowerPath.endsWith(".svg") -> "image/svg+xml"
    lowerPath.endsWith(".webp") -> "image/webp"
    lowerPath.endsWith(".css") -> "text/css"
    lowerPath.endsWith(".js") -> "application/javascript"
    lowerPath.endsWith(".ttf") -> "font/ttf"
    lowerPath.endsWith(".woff") -> "font/woff"
    lowerPath.endsWith(".woff2") -> "font/woff2"
    lowerPath.endsWith(".mp3") -> "audio/mpeg"
    lowerPath.endsWith(".wav") -> "audio/wav"
    lowerPath.endsWith(".ogg") -> "audio/ogg"
    lowerPath.endsWith(".spx") -> "audio/speex"
    lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") -> "text/html"
    else -> "application/octet-stream"
}

private val BRIDGE_JS = """
<script>
(function(){
document.addEventListener('click', function(e) {
    var el = e.target;
    while (el && el.tagName !== 'A') { el = el.parentElement; }
    if (!el) return;
    var href = el.getAttribute('href') || '';
    if (href.indexOf('sound://') === 0) {
        e.preventDefault(); e.stopPropagation();
        window.location.href = href;
        return false;
    }
}, true);
})();
</script>
"""

private val IMG_SRC_PATTERN = Regex("""(<img[^>]*?src=)(["'])([^"']*?(?:\.png|\.jpg|\.jpeg|\.gif|\.svg|\.webp|\.bmp|\.ico))(["'])""", RegexOption.IGNORE_CASE)
private val SKIP_PREFIXES = setOf("http://", "https://", "data:", "mdxres://", "entry://", "sound://", "#")

private fun preprocessHtmlImages(html: String): String {
    var replaceCount = 0
    val result = IMG_SRC_PATTERN.replace(html) { match ->
        val prefix = match.groupValues[1]
        val quote = match.groupValues[2]
        val src = match.groupValues[3]
        if (SKIP_PREFIXES.any { src.startsWith(it) }) match.value else {
            val fileName = src.substringAfterLast("/").substringAfterLast("\\")
                .substringBefore("?").substringBefore("#")
            replaceCount++
            "${prefix}${quote}mdxres://${fileName}${quote}"
        }
    }
    log.i("MdxWebView", "preprocessHtmlImages: replaced=$replaceCount, output length=${result.length}")
    return result
}

private object GlobalBrowserManager {
    var panel: JPanel? = null
    var browser: CefBrowser? = null
    var messageRouter: CefMessageRouter? = null
    var currentTempFile: File? = null
    @Volatile var browserReady = false
    val lock = Any()

    fun isBrowserReady(): Boolean = browserReady

    fun getOrCreatePanel(): JPanel {
        synchronized(lock) {
            panel?.let { return it }

            log.i("MdxWebView", "GlobalBrowserManager: Creating JCEF browser panel...")

            if (!ensureCefInitialized()) {
                log.e("MdxWebView", "GlobalBrowserManager: JCEF init failed")
                val errorPanel = JPanel(BorderLayout())
                val errorLabel = JLabel("Browser engine failed to initialize.")
                errorLabel.horizontalAlignment = SwingConstants.CENTER
                errorPanel.add(errorLabel, BorderLayout.CENTER)
                return errorPanel
            }

            val client = cefClient!!

            client.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                    if (!browserReady) {
                        browserReady = true
                        log.i("MdxWebView", "Browser is ready (onLoadEnd, httpStatus=$httpStatusCode)")
                    }
                    val js = """
(function(){
  document.addEventListener('click', function(e) {
    var el = e.target;
    while (el && el.tagName !== 'A') { el = el.parentElement; }
    if (!el) return;
    var href = el.getAttribute('href') || '';
    if (!href || href.charAt(0) === '#') return;
    if (href.match(/^(https?:\/\/|data:|mailto:|javascript:|file:\/\/)/)) return;
    if (href.indexOf('sound://') === 0) { return; }
    e.preventDefault(); e.stopPropagation();
    var word;
    if (href.indexOf('entry://') === 0 || href.indexOf('bword://') === 0) {
      var proto = href.indexOf('entry://')===0 ? 'entry://' : 'bword://';
      word = decodeURIComponent(href.substring(proto.length).split('#')[0]).split('?')[0];
    } else {
      word = decodeURIComponent(href.split('#')[0]).split('?')[0];
    }
    if (word) {
      window.__gdictClickedWord = word;
    }
    return false;
  }, true);
  setInterval(function() {
    try {
      if (window.__gdictClickedWord) {
        var w = window.__gdictClickedWord;
        window.__gdictClickedWord = '';
        document.title = 'GDICT_ENTRY:' + w;
      }
    } catch(e) {}
  }, 150);
})();
"""
                    frame.executeJavaScript(js, "gdict_link_handler", 0)
                }
            })

            client.addRequestHandler(object : CefRequestHandlerAdapter() {
                override fun onBeforeBrowse(
                    browser: CefBrowser, frame: CefFrame,
                    request: CefRequest, userGesture: Boolean, isRedirect: Boolean
                ): Boolean {
                    val url = request.url ?: return false
                    if (url.startsWith("entry://") || url.startsWith("bword://")) {
                        val entry = java.net.URLDecoder.decode(
                            url.removePrefix("entry://").removePrefix("bword://"), "UTF-8"
                        ).substringBefore("#")
                        log.i("MdxWebView", "onBeforeBrowse: intercepted '$entry'")
                        SwingUtilities.invokeLater { currentOnEntryClick?.invoke(entry) }
                        return true
                    }
                    return false
                }
            })

            client.addDisplayHandler(object : org.cef.handler.CefDisplayHandlerAdapter() {
                override fun onTitleChange(browser: CefBrowser, title: String) {
                    if (title.startsWith("GDICT_ENTRY:")) {
                        val entry = title.removePrefix("GDICT_ENTRY:")
                        log.i("MdxWebView", "Polled entry click: '$entry'")
                        browser.executeJavaScript("document.title='Gdict';", "", 0)
                        SwingUtilities.invokeLater { currentOnEntryClick?.invoke(entry) }
                    }
                }
            })

            val cefBrowser = client.createBrowser("about:blank", false, false)
            browser = cefBrowser

            val router = CefMessageRouter.create()
            router.addHandler(object : CefMessageRouterHandlerAdapter() {
                override fun onQuery(browser: CefBrowser, frame: CefFrame, queryId: Long, request: String, persistent: Boolean, callback: CefQueryCallback): Boolean {
                    log.i("MdxWebView", "CefMessageRouter.onQuery: request='$request'")
                    when {
                        request.startsWith("entry:") -> {
                            val entry = request.removePrefix("entry:")
                            log.i("MdxWebView", "CefMessageRouter: entry click -> '$entry'")
                            SwingUtilities.invokeLater { currentOnEntryClick?.invoke(entry) }
                            callback.success("")
                            return true
                        }
                        request.startsWith("sound:") -> {
                            val soundPath = request.removePrefix("sound:")
                            log.i("MdxWebView", "CefMessageRouter: sound click -> '$soundPath'")
                            SwingUtilities.invokeLater { currentOnPlayAudio?.invoke(soundPath) }
                            callback.success("")
                            return true
                        }
                    }
                    log.w("MdxWebView", "CefMessageRouter: unknown request='$request'")
                    callback.failure(-1, "Unknown request")
                    return false
                }
                override fun onQueryCanceled(browser: CefBrowser, frame: CefFrame, queryId: Long) {}
            }, true)
            client.addMessageRouter(router)
            messageRouter = router

            val p = JPanel(BorderLayout())
            p.add(cefBrowser.uiComponent, BorderLayout.CENTER)
            panel = p

            log.i("MdxWebView", "GlobalBrowserManager: Panel created successfully")

            val pending = pendingHtml
            if (pending != null) {
                pendingHtml = null
                log.i("MdxWebView", "Loading pending HTML (${pending.length} chars)")
                loadHtmlToBrowser(pending)
            }

            return p
        }
    }

    var pendingHtml: String? = null

    fun loadHtmlToBrowser(html: String) {
        synchronized(lock) {
            val b = browser ?: run {
                pendingHtml = html
                log.w("MdxWebView", "loadHtmlToBrowser: browser is null, pending (${html.length} chars)")
                return
            }

            pendingHtml = null

            with(File(System.getProperty("java.io.tmpdir"), "gdict_html")) {
                mkdirs()
            }
            val tempDir = File(System.getProperty("java.io.tmpdir"), "gdict_html")
            val tempFile = File(tempDir, "content_${System.currentTimeMillis()}.html")
            tempFile.writeText(html, Charsets.UTF_8)

            currentTempFile?.delete()
            currentTempFile = tempFile

            val url = tempFile.toURI().toURL().toString()
            b.loadURL(url)
            log.i("MdxWebView", "Content loaded via file URL ($url, ${html.length} chars)")
        }
    }

    fun shutdown() {
        synchronized(lock) {
            try { currentTempFile?.delete() } catch (_: Throwable) {}
            try { messageRouter?.dispose() } catch (_: Throwable) {}
            try { browser?.close(true) } catch (_: Throwable) {}
            panel = null
            browser = null
            messageRouter = null
            currentTempFile = null
            browserReady = false
            log.i("MdxWebView", "GlobalBrowserManager: Shutdown complete")
        }
    }

    fun setBrowserZoom(zoomLevel: Double) {
        synchronized(lock) {
            val b = browser ?: return
            SwingUtilities.invokeLater {
                try {
                    b.zoomLevel = zoomLevel
                    log.i("MdxWebView", "Browser zoom set to $zoomLevel")
                } catch (e: Throwable) {
                    log.e("MdxWebView", "Failed to set zoom: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun MdxWebView(
    definition: String,
    css: String,
    darkMode: Boolean,
    dictionaryRepository: DictionaryRepository,
    onEntryClick: (String) -> Unit = {},
    onPlayAudio: (String) -> Unit = {},
    webViewVisible: Boolean = true
) {
    currentRepository = dictionaryRepository
    currentOnEntryClick = onEntryClick
    currentOnPlayAudio = onPlayAudio

    val cachedPanel = remember {
        GlobalBrowserManager.getOrCreatePanel()
    }

    LaunchedEffect(webViewVisible) {
        SwingUtilities.invokeLater {
            cachedPanel.isVisible = webViewVisible
        }
    }

    LaunchedEffect(Unit) {
        if (!isCefInitialized()) {
            log.i("MdxWebView", "Waiting for JCEF initialization on AWT thread...")
            while (!isCefInitialized()) {
                delay(200)
            }
            log.i("MdxWebView", "JCEF initialization complete")
        }
    }

    LaunchedEffect(definition, css, darkMode) {
        if (definition.isEmpty()) return@LaunchedEffect

        if (!GlobalBrowserManager.isBrowserReady()) {
            log.i("MdxWebView", "Waiting for browser to be ready...")
            while (!GlobalBrowserManager.isBrowserReady()) {
                delay(100)
            }
            log.i("MdxWebView", "Browser is now ready")
        }

        delay(50)

        val rawHtml = HtmlContentBuilder.build(definition, css, darkMode)
        val mdxresCount = rawHtml.count { it == 'm' }.let { _ -> Regex("mdxres://").findAll(rawHtml).count() }
        val imgCount = Regex("<img[^>]*>").findAll(rawHtml).count()
        val imgSamples = Regex("<img[^>]{0,80}>").findAll(rawHtml).take(3).map { it.value }.toList()
        log.i("MdxWebView", "After HtmlContentBuilder: mdxres://=$mdxresCount, img=$imgCount, samples=$imgSamples")

        val withBridge = rawHtml.replace("</body>", "$BRIDGE_JS\n</body>")
        val finalHtml = preprocessHtmlImages(withBridge)

        log.i("MdxWebView", "Loading content (${finalHtml.length} chars)")

        SwingUtilities.invokeLater {
            GlobalBrowserManager.loadHtmlToBrowser(finalHtml)
        }
    }

    SwingPanel(
        factory = { cachedPanel },
        update = { _ -> },
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    )
}

fun shutdownBrowser() {
    GlobalBrowserManager.shutdown()
}

fun setBrowserZoom(zoomLevel: Double) {
    GlobalBrowserManager.setBrowserZoom(zoomLevel)
}
