package io.github.dev1ck.ytloader

import android.Manifest
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
import io.github.dev1ck.ytloader.ui.main.MainScreen
import io.github.dev1ck.ytloader.ui.main.MainViewModel
import io.github.dev1ck.ytloader.ui.theme.YTLoaderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTLoaderTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                // 알림 권한은 첫 다운로드 때 한 번만 묻는다. 결과와 무관하게 다운로드는 진행.
                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    viewModel.startDownload()
                }
                MainScreen(
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
            }
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}
