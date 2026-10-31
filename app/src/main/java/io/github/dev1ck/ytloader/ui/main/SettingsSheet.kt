package io.github.dev1ck.ytloader.ui.main

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dev1ck.ytloader.BuildConfig
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.download.FileActions
import io.github.dev1ck.ytloader.login.LoginState

/** ⚙ 설정: 로그인(FR-9·10), yt-dlp 버전·업데이트(FR-8), 저장 위치 안내. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: MainUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importCookies)
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::setSaveFolder)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            SectionLabel(stringResource(R.string.login_section_title))
            SettingsCard {
                val enabled = state.loginEnabled
                when (val login = state.loginState) {
                    LoginState.LoggedOut -> {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.login_state_logged_out)) },
                            supportingContent = { Text(stringResource(R.string.login_hint)) },
                            leadingContent = { Icon(Icons.Filled.AccountCircle, null) },
                            colors = transparentListItem(),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    viewModel.openLogin()
                                },
                                enabled = enabled,
                            ) { Text(stringResource(R.string.login)) }
                            OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) }, enabled = enabled) {
                                Text(stringResource(R.string.import_cookies))
                            }
                        }
                    }
                    is LoginState.LoggedIn -> {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.login_state_logged_in, login.date)) },
                            leadingContent = { Icon(Icons.Filled.AccountCircle, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = transparentListItem(),
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        OptionRow(
                            title = stringResource(R.string.use_cookies),
                            subtitle = stringResource(R.string.use_cookies_description),
                            checked = state.useCookies,
                            enabled = enabled,
                            onChange = viewModel::setUseCookies,
                        )
                        Row(Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = viewModel::requestLogout, enabled = enabled) { Text(stringResource(R.string.logout)) }
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.section_engine))
            SettingsCard {
                ListItem(
                    headlineContent = { Text("yt-dlp") },
                    supportingContent = {
                        Text(state.ytDlpVersion?.let { stringResource(R.string.version_value, it) } ?: stringResource(R.string.version_unknown))
                    },
                    trailingContent = {
                        if (state.updating) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            FilledTonalButton(onClick = viewModel::updateYtDlp, enabled = state.updateEnabled) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.update))
                            }
                        }
                    },
                    colors = transparentListItem(),
                )
            }

            SectionLabel(stringResource(R.string.section_storage))
            SettingsCard {
                val context = LocalContext.current
                val location = FileActions.locationLabel(state.saveFolder)
                val noFilesApp = stringResource(R.string.error_no_files_app, location)
                ListItem(
                    headlineContent = { Text(location) },
                    supportingContent = { Text(stringResource(R.string.storage_desc)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_folder), null) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_open), stringResource(R.string.folder)) },
                    colors = transparentListItem(),
                    modifier = Modifier.clickable {
                        val intent = FileActions.folderIntent(context, state.saveFolder, null)
                        try {
                            if (intent != null) context.startActivity(intent) else viewModel.showMessage(noFilesApp)
                        } catch (e: ActivityNotFoundException) {
                            viewModel.showMessage(noFilesApp)
                        }
                    },
                )
                // 바꾼 위치는 다음 다운로드부터 적용된다 (진행 중인 다운로드는 시작할 때의 위치에 저장).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    OutlinedButton(onClick = { folderLauncher.launch(null) }) { Text(stringResource(R.string.storage_change)) }
                    if (state.saveFolder != null) {
                        TextButton(onClick = { viewModel.setSaveFolder(null) }) { Text(stringResource(R.string.storage_reset)) }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.about_line, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        // 시트 배경이 surfaceContainerLow라 카드는 한 단계 밝게 해야 구분된다.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun transparentListItem() = ListItemDefaults.colors(containerColor = Color.Transparent)
