package io.github.dev1ck.ytloader

import android.content.Context
import io.github.dev1ck.ytloader.download.DownloadRepository
import io.github.dev1ck.ytloader.engine.YoutubeDlAndroidEngine
import io.github.dev1ck.ytloader.engine.YtDlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

sealed interface EngineStatus {
    data object Initializing : EngineStatus
    data object Ready : EngineStatus
    data class Failed(val message: String) : EngineStatus
}

/** 수동 DI. Application에서 한 번 만든다. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val engine: YtDlpEngine = YoutubeDlAndroidEngine(appContext)
    val engineStatus = MutableStateFlow<EngineStatus>(EngineStatus.Initializing)
    val ytDlpVersion = MutableStateFlow<String?>(null)
    val downloadRepository = DownloadRepository()
    val workRoot = File(appContext.filesDir, "work")
}
