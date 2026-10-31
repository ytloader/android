package io.github.ytloader.util

object UrlExtractor {
    private val url = Regex("""https?://\S+""")
    private const val TRAILING = ".,!?;:)]}>\"'"
    private val youtubeHost = Regex("""^https?://([a-z0-9-]+\.)*(youtube\.com|youtu\.be)(/|$|\?)""", RegexOption.IGNORE_CASE)

    /** 공유 텍스트에서 첫 http(s) URL. 끝에 붙은 문장부호·괄호는 뗀다. 없으면 null. */
    fun extract(text: String?): String? {
        val match = url.find(text ?: return null)?.value ?: return null
        return match.trimEnd { it in TRAILING }.takeIf { url.matches(it) }
    }

    /** youtube.com(하위 도메인 포함) 또는 youtu.be 링크인지. 클립보드 제안에 쓴다. */
    fun isYouTube(url: String): Boolean = youtubeHost.containsMatchIn(url)
}
