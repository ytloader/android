package io.github.ytloader.download

import java.io.Serializable

/** 사용자 선택값. [format]·[quality]는 카탈로그의 값 문자열. */
data class DownloadOptions(
    val mode: Mode,
    val format: String,
    val quality: String,
    val embedThumbnail: Boolean,
    val embedMetadata: Boolean,
    val playlist: Boolean,
) : Serializable
