package io.github.dev1ck.ytloader.engine

data class YtOption(val name: String, val value: String? = null)

/**
 * [exitCode]: 성공 시 0, 실패 시 라이브러리가 알려주지 않아 null.
 * [lastError]: 마지막 `ERROR:` 줄, 없으면 예외 메시지의 마지막 줄.
 */
data class ExecResult(val exitCode: Int?, val lastError: String?, val cancelled: Boolean) {
    val succeeded: Boolean get() = exitCode == 0 && !cancelled
}

sealed interface UpdateResult {
    data class Updated(val version: String?) : UpdateResult
    data class AlreadyUpToDate(val version: String?) : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

interface YtDlpEngine {
    /** 실패 시 예외. IO 스레드에서 호출. */
    suspend fun init()

    /** `--version` 실행. 블로킹. */
    fun version(): String?

    /** 블로킹. 예외를 던지지 않고 항상 [ExecResult]로 돌려준다. */
    fun execute(
        url: String,
        options: List<YtOption>,
        processId: String,
        onLine: (progress: Float, etaSec: Long, line: String) -> Unit,
    ): ExecResult

    /** 제목 미리보기용으로 yt-dlp를 실행하고 stdout을 돌려준다. 실패·취소면 null. 블로킹. */
    fun query(url: String, options: List<YtOption>, processId: String): String?

    fun cancel(processId: String): Boolean

    /** STABLE 채널. 블로킹. */
    fun update(): UpdateResult
}
