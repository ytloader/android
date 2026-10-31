package io.github.dev1ck.ytloader.util

object UrlExtractor {
    private val url = Regex("""https?://\S+""")
    private const val TRAILING = ".,!?;:)]}>\"'"

    /** 공유 텍스트에서 첫 http(s) URL. 끝에 붙은 문장부호·괄호는 뗀다. 없으면 null. */
    fun extract(text: String?): String? {
        val match = url.find(text ?: return null)?.value ?: return null
        return match.trimEnd { it in TRAILING }.takeIf { url.matches(it) }
    }
}
