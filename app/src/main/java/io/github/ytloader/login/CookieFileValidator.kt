package io.github.ytloader.login

/** 가져온 cookies.txt 검증 (ARCHITECTURE 9장). */
object CookieFileValidator {
    const val MAX_BYTES = 1024 * 1024
    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"

    /** Netscape 형식이고 youtube.com 도메인 줄이 1개 이상이면 true. 크기 제한은 호출 측이 [MAX_BYTES]로 확인. */
    fun isValid(content: String): Boolean = content.lineSequence().any { raw ->
        val line = raw.removePrefix(HTTP_ONLY_PREFIX)
        val fields = line.split('\t')
        if (fields.size != 7) return@any false
        val domain = fields[0]
        domain == "youtube.com" || domain.endsWith(".youtube.com")
    }
}
