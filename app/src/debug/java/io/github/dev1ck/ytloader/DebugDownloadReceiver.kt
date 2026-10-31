package io.github.dev1ck.ytloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.dev1ck.ytloader.download.DownloadOptions
import io.github.dev1ck.ytloader.download.DownloadRequest
import io.github.dev1ck.ytloader.download.DownloadService
import io.github.dev1ck.ytloader.download.Mode

/**
 * debug 빌드 전용 검증 훅: adb로 옵션을 지정해 다운로드를 시작한다. 앱이 포그라운드일 때만 쓴다.
 * adb shell am broadcast -n io.github.dev1ck.ytloader/.DebugDownloadReceiver --es url URL --es mode VIDEO --es format mkv --es quality 720
 */
class DebugDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as YTLoaderApp).container
        val options = DownloadOptions(
            mode = Mode.valueOf(intent.getStringExtra("mode") ?: "AUDIO"),
            format = intent.getStringExtra("format") ?: "mp3",
            quality = intent.getStringExtra("quality") ?: "",
            embedThumbnail = intent.getBooleanExtra("thumbnail", true),
            embedMetadata = intent.getBooleanExtra("metadata", true),
            playlist = intent.getBooleanExtra("playlist", false),
        )
        if (!container.downloadRepository.tryBegin()) return
        DownloadService.start(context, DownloadRequest(intent.getStringExtra("url")!!, options, intent.getStringExtra("cookies")))
    }
}
