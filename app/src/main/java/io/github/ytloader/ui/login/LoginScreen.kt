package io.github.ytloader.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import io.github.ytloader.BuildConfig
import io.github.ytloader.R
import io.github.ytloader.login.NetscapeCookieWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "YTLoader"

/** 데스크톱판과 같은 시작 URL. */
private const val LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Dko%26next%3Dhttps%253A%252F%252Fwww.youtube.com%252F"
private const val YOUTUBE_URL = "https://www.youtube.com"
private val LOGIN_COOKIES = setOf("SAPISID", "__Secure-3PAPISID")

/** SPEC O-1: Google이 임베디드 브라우저 로그인을 막으면 true로 바꿔 UA에서 `; wv`를 뺀다. */
private const val STRIP_WV_FROM_USER_AGENT = false

/** 로그인 화면에서 열어 둘 호스트. 그 외 페이지는 외부 브라우저로 넘긴다. */
private val allowedHost = Regex("""(^|\.)((google|youtube)\.[a-z.]+|gstatic\.com|googleusercontent\.com)$""")

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onCookiesDetected: (String) -> Unit, onCancel: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var detected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val onDetected by rememberUpdatedState(onCookiesDetected)
    BackHandler(onBack = onCancel)

    fun checkLogin() {
        if (detected) return
        val header = CookieManager.getInstance().getCookie(YOUTUBE_URL)
        val names = NetscapeCookieWriter.names(header)
        if (names.none { it in LOGIN_COOKIES }) return
        detected = true
        if (BuildConfig.DEBUG) Log.d(TAG, "login cookies detected: ${names.joinToString()}")
        scope.launch {
            CookieManager.getInstance().flush()
            delay(2000) // 리다이렉트 마무리
            onDetected(CookieManager.getInstance().getCookie(YOUTUBE_URL) ?: header)
        }
    }

    LaunchedEffect(Unit) {
        while (!detected) {
            delay(1000)
            checkLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(R.string.login_banner),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CookieManager.getInstance().setAcceptCookie(true)
                    WebView(context).apply {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        if (STRIP_WV_FROM_USER_AGENT) {
                            settings.userAgentString = WebSettings.getDefaultUserAgent(context).replace("; wv", "")
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                                checkLogin()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val host = request.url.host.orEmpty()
                                if (!request.isForMainFrame || allowedHost.containsMatchIn(host)) return false
                                runCatching {
                                    view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url.toString().toUri()))
                                }
                                return true
                            }
                        }
                        loadUrl(LOGIN_URL)
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}
