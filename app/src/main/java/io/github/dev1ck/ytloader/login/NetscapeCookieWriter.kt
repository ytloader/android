package io.github.dev1ck.ytloader.login

/** `CookieManager.getCookie()`의 `이름=값; ...` 문자열 → Netscape cookies.txt 본문. */
object NetscapeCookieWriter {
    const val HEADER = "# Netscape HTTP Cookie File"
    private const val DOMAIN = ".youtube.com"

    fun write(cookieHeader: String, expiresEpochSec: Long): String = buildString {
        appendLine(HEADER)
        for (pair in cookieHeader.split(';')) {
            val trimmed = pair.trim()
            val eq = trimmed.indexOf('=')
            val name = if (eq < 0) trimmed else trimmed.substring(0, eq)
            val value = if (eq < 0) "" else trimmed.substring(eq + 1)
            if (name.isEmpty()) continue
            appendLine(listOf(DOMAIN, "TRUE", "/", "TRUE", expiresEpochSec.toString(), name, value).joinToString("\t"))
        }
    }

    /** 이름 목록만 (로그·감지용 — 값은 다루지 않는다). */
    fun names(cookieHeader: String?): List<String> =
        cookieHeader.orEmpty().split(';').map { it.trim().substringBefore('=') }.filter { it.isNotEmpty() }
}
