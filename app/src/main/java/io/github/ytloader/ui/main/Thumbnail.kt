package io.github.ytloader.ui.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val THUMBNAIL_MAX_WIDTH = 480
private const val TIMEOUT_MS = 10_000

/** 미리보기 썸네일. 이미지 라이브러리 없이 직접 받아 줄여서 표시하고, 받기 전·실패 시 [placeholder]. */
@Composable
internal fun Thumbnail(url: String, modifier: Modifier, placeholder: @Composable () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, url) {
        value = withContext(Dispatchers.IO) { runCatching { load(url) }.getOrNull() }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        // YouTube 썸네일은 4:3 레터박스(hqdefault)도 있으므로 잘라서 채운다.
        if (image != null) Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        else placeholder()
    }
}

private fun load(url: String): ImageBitmap? {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    try {
        val bytes = connection.inputStream.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= THUMBNAIL_MAX_WIDTH) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    } finally {
        connection.disconnect()
    }
}
