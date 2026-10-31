package io.github.ytloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ytloader.ui.login.LoginScreen
import io.github.ytloader.ui.main.MainScreen
import io.github.ytloader.ui.main.MainViewModel
import io.github.ytloader.ui.main.Screen
import io.github.ytloader.ui.theme.YTLoaderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 화면 재생성(회전 등)이나 최근 앱에서 다시 열 때는 같은 공유를 다시 처리하지 않는다.
        if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) {
            handleShare(intent)
        }
        setContent {
            YTLoaderTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                // 알림 권한은 첫 다운로드 때 한 번만 묻는다. 결과와 무관하게 다운로드는 진행.
                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    viewModel.startDownload()
                }
                when (state.screen) {
                    Screen.Main -> MainScreen(
                        state = state,
                        viewModel = viewModel,
                        onDownloadClick = {
                            if (viewModel.validateUrl()) {
                                if (needsNotificationPermission() && viewModel.consumeNotificationPermissionAsk()) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.startDownload()
                                }
                            }
                        },
                    )
                    Screen.Login -> LoginScreen(
                        onCookiesDetected = viewModel::onLoginCookies,
                        onCancel = viewModel::onLoginCancelled,
                    )
                }
            }
        }
    }

    // singleTask: 이미 열려 있으면 같은 화면에서 URL만 바꾼다 (FR-2).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            viewModel.onSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}
