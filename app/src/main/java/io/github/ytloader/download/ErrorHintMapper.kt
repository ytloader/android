package io.github.ytloader.download

/** SPEC 7장: 실패 시 안내 문구. */
object ErrorHintMapper {
    const val UPDATE_HINT = "yt-dlp 업데이트 후 다시 시도해 보세요."
    const val LOGIN_HINT = "비공개 · 연령 제한 · 멤버십 영상이라면 로그인이 필요합니다. 이미 로그인했다면 다시 로그인해 보세요."
    private val loginKeywords = listOf("sign in", "login", "private", "members", "confirm your age")

    fun hint(lastError: String?): String {
        val error = lastError.orEmpty()
        val needsLogin = loginKeywords.any { error.contains(it, ignoreCase = true) }
        return if (needsLogin) "$UPDATE_HINT\n$LOGIN_HINT" else UPDATE_HINT
    }
}
