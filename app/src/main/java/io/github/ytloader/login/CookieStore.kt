package io.github.ytloader.login

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface LoginState {
    data object LoggedOut : LoginState
    data class LoggedIn(val date: String) : LoginState
}

/** cookies.txt 경로·상태·저장·가져오기·로그아웃 (ARCHITECTURE 9장). 쿠키 값은 어디에도 기록하지 않는다. */
class CookieStore(private val context: Context) {
    val file = File(context.noBackupFilesDir, "cookies.txt")
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    /** 로그인 상태 = 파일 존재. 날짜 = 수정일 (yt-dlp가 실행 후 다시 쓰면 바뀐다). */
    fun refresh() {
        _state.value = readState()
    }

    private fun readState(): LoginState =
        if (file.isFile) {
            LoginState.LoggedIn(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(file.lastModified())))
        } else {
            LoginState.LoggedOut
        }

    /** WebView 쿠키 헤더(`이름=값; ...`)를 Netscape 형식으로 저장. */
    suspend fun saveFromWebView(cookieHeader: String) = withContext(Dispatchers.IO) {
        val expires = System.currentTimeMillis() / 1000 + ONE_YEAR_SEC
        file.writeText(NetscapeCookieWriter.write(cookieHeader, expires))
        refresh()
    }

    /** 검증 통과 시 원문 그대로 저장하고 true. */
    suspend fun import(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readNBytesCompat(CookieFileValidator.MAX_BYTES + 1)
        } ?: return@withContext false
        if (bytes.size > CookieFileValidator.MAX_BYTES) return@withContext false
        val text = bytes.toString(Charsets.UTF_8)
        if (!CookieFileValidator.isValid(text)) return@withContext false
        file.writeBytes(bytes)
        refresh()
        true
    }

    /** WebView 쿠키·저장소와 cookies.txt를 모두 지운다. 메인 스레드에서 호출. */
    fun logout() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        file.delete()
        refresh()
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val ONE_YEAR_SEC = 365L * 24 * 60 * 60
    }
}
