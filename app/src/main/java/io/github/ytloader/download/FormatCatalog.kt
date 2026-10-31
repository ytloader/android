package io.github.ytloader.download

enum class Mode { AUDIO, VIDEO }

/** [hasQuality]: 품질(음질/해상도) 선택이 의미 있는 포맷. */
data class FormatOption(
    val value: String,
    val label: String,
    val hasQuality: Boolean,
    val supportsThumbnail: Boolean,
)

data class QualityOption(val value: String, val label: String)

/** SPEC 6장 카탈로그. 값은 yt-dlp에 그대로 전달한다. */
object FormatCatalog {
    val audioFormats = listOf(
        FormatOption("mp3", "MP3 (범용)", hasQuality = true, supportsThumbnail = true),
        FormatOption("m4a", "M4A (AAC)", hasQuality = true, supportsThumbnail = true),
        FormatOption("opus", "OPUS (고효율)", hasQuality = true, supportsThumbnail = true),
        FormatOption("flac", "FLAC (무손실)", hasQuality = false, supportsThumbnail = true),
        FormatOption("wav", "WAV (무손실)", hasQuality = false, supportsThumbnail = false),
        FormatOption("original", "원본 (변환 없음)", hasQuality = false, supportsThumbnail = false),
    )

    val audioQualities = listOf(
        QualityOption("0", "최고 (VBR)"),
        QualityOption("320K", "320 kbps"),
        QualityOption("256K", "256 kbps"),
        QualityOption("192K", "192 kbps"),
        QualityOption("128K", "128 kbps"),
    )

    val videoFormats = listOf(
        FormatOption("mp4", "MP4 (호환성 높음)", hasQuality = true, supportsThumbnail = true),
        FormatOption("mkv", "MKV", hasQuality = true, supportsThumbnail = true),
        FormatOption("webm", "WEBM", hasQuality = true, supportsThumbnail = false),
        FormatOption("original", "원본 (변환 없음)", hasQuality = true, supportsThumbnail = false),
    )

    val videoResolutions = listOf(
        QualityOption("", "최고 화질"),
        QualityOption("2160", "2160p (4K)"),
        QualityOption("1440", "1440p"),
        QualityOption("1080", "1080p"),
        QualityOption("720", "720p"),
        QualityOption("480", "480p"),
        QualityOption("360", "360p"),
    )

    fun formats(mode: Mode): List<FormatOption> = if (mode == Mode.AUDIO) audioFormats else videoFormats

    fun qualities(mode: Mode): List<QualityOption> = if (mode == Mode.AUDIO) audioQualities else videoResolutions

    /** 알 수 없는 값이면 첫 항목. */
    fun format(mode: Mode, value: String?): FormatOption =
        formats(mode).firstOrNull { it.value == value } ?: formats(mode).first()

    /** 알 수 없는 값이면 첫 항목. */
    fun quality(mode: Mode, value: String?): QualityOption =
        qualities(mode).firstOrNull { it.value == value } ?: qualities(mode).first()
}
