package io.github.dev1ck.ytloader

import android.app.Application
import android.util.Log
import io.github.dev1ck.ytloader.download.DownloadNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YTLoaderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DownloadNotifications(this).createChannels()
        container.appScope.launch(Dispatchers.IO) {
            // 프로세스가 막 시작됐으므로 서비스는 돌고 있지 않다 → 지난 작업 폴더를 정리한다.
            container.workRoot.deleteRecursively()
            try {
                container.engine.init()
                container.engineStatus.value = EngineStatus.Ready
                container.ytDlpVersion.value = container.engine.version()
            } catch (e: Exception) {
                Log.e("YTLoader", "engine init failed", e)
                container.engineStatus.value = EngineStatus.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
