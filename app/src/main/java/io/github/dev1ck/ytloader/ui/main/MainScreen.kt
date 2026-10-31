package io.github.dev1ck.ytloader.ui.main

import android.content.ActivityNotFoundException
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.download.DownloadState
import io.github.dev1ck.ytloader.download.FileActions
import io.github.dev1ck.ytloader.download.FormatCatalog
import io.github.dev1ck.ytloader.download.MediaStoreExporter
import io.github.dev1ck.ytloader.download.Mode
import io.github.dev1ck.ytloader.download.SavedFile
import io.github.dev1ck.ytloader.download.ShareRules
import io.github.dev1ck.ytloader.login.LoginState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    onDownloadClick: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    ClipboardWatcher(enabled = state.url.isBlank(), onText = viewModel::onClipboardText)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.loginState is LoginState.LoggedIn) {
                        AssistChip(
                            onClick = { showSettings = true },
                            label = { Text(stringResource(R.string.chip_logged_in)) },
                            leadingIcon = {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(AssistChipDefaults.IconSize))
                            },
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = { BottomActionArea(state, viewModel, onDownloadClick) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            UrlSection(state, viewModel)
            ModeSelector(state, viewModel)
            ChipSection(
                title = stringResource(R.string.label_format),
                options = FormatCatalog.formats(state.mode).map { it.value to it.label },
                selected = state.format.value,
                enabled = state.inputsEnabled,
                onSelect = viewModel::setFormat,
            )
            ChipSection(
                title = stringResource(if (state.mode == Mode.AUDIO) R.string.label_audio_quality else R.string.label_resolution),
                options = FormatCatalog.qualities(state.mode).map { it.value to it.label },
                selected = state.quality.value,
                enabled = state.qualityEnabled,
                onSelect = viewModel::setQuality,
                helper = if (!state.format.hasQuality) stringResource(R.string.helper_no_quality) else null,
            )
            OptionsCard(state, viewModel)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSettings) SettingsSheet(state, viewModel, onDismiss = { showSettings = false })
    Dialogs(state, viewModel)
}

// ---- 입력 ----

@Composable
private fun UrlSection(state: MainUiState, viewModel: MainViewModel) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateContentSize()) {
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::setUrl,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            placeholder = { Text(stringResource(R.string.url_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_link), null) },
            trailingIcon = {
                if (state.url.isEmpty()) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }
                                    ?.getItemAt(0)?.coerceToText(context)?.toString()
                                if (!text.isNullOrEmpty()) viewModel.setUrl(text.trim())
                                focusManager.clearFocus()
                            }
                        },
                        enabled = state.inputsEnabled,
                    ) { Icon(painterResource(R.drawable.ic_paste), stringResource(R.string.paste)) }
                } else {
                    IconButton(onClick = { viewModel.setUrl("") }, enabled = state.inputsEnabled) {
                        Icon(Icons.Filled.Clear, stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            enabled = state.inputsEnabled,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = state.clipboardSuggestion != null && state.url.isBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {
                        viewModel.acceptClipboardSuggestion()
                        focusManager.clearFocus()
                    },
                    label = {
                        Text(
                            stringResource(R.string.clipboard_suggestion, shortUrl(state.clipboardSuggestion.orEmpty())),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_paste), null, Modifier.size(AssistChipDefaults.IconSize)) },
                    modifier = Modifier.weight(1f, fill = false),
                )
                IconButton(onClick = viewModel::dismissClipboardSuggestion) {
                    Icon(Icons.Filled.Close, stringResource(R.string.close))
                }
            }
        }

        TitlePreviewRow(state)
    }
}

@Composable
private fun TitlePreviewRow(state: MainUiState) {
    when (val preview = state.titlePreview) {
        TitleState.Idle -> Unit
        TitleState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.title_loading), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TitleState.Failed -> Text(
            stringResource(R.string.title_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        is TitleState.Loaded -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val playlistTitle = preview.info.playlistTitle.takeIf { state.playlist }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                Icon(
                    painterResource(
                        when {
                            playlistTitle != null -> R.drawable.ic_playlist
                            state.mode == Mode.AUDIO -> R.drawable.ic_music
                            else -> R.drawable.ic_movie
                        },
                    ),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        playlistTitle ?: preview.info.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (playlistTitle != null) {
                        Text(
                            stringResource(R.string.title_playlist),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(state: MainUiState, viewModel: MainViewModel) {
    val modes = listOf(
        Triple(Mode.AUDIO, R.string.mode_audio, R.drawable.ic_music),
        Triple(Mode.VIDEO, R.string.mode_video, R.drawable.ic_movie),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = state.mode == mode,
                onClick = { viewModel.setMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                enabled = state.inputsEnabled,
                icon = { Icon(painterResource(icon), null, Modifier.size(18.dp)) },
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun ChipSection(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    helper: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(title)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            for ((value, label) in options) {
                val isSelected = value == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    enabled = enabled,
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Done, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
        }
        if (helper != null) {
            Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OptionsCard(state: MainUiState, viewModel: MainViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        OptionRow(
            title = stringResource(R.string.option_thumbnail),
            subtitle = stringResource(
                if (state.format.supportsThumbnail) R.string.option_thumbnail_desc else R.string.option_thumbnail_unsupported,
            ),
            checked = state.embedThumbnail && state.format.supportsThumbnail,
            enabled = state.thumbnailEnabled,
            onChange = viewModel::setEmbedThumbnail,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        OptionRow(
            title = stringResource(R.string.option_metadata),
            subtitle = stringResource(R.string.option_metadata_desc),
            checked = state.embedMetadata,
            enabled = state.inputsEnabled,
            onChange = viewModel::setEmbedMetadata,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        OptionRow(
            title = stringResource(R.string.option_playlist),
            subtitle = stringResource(R.string.option_playlist_desc),
            checked = state.playlist,
            enabled = state.inputsEnabled,
            onChange = viewModel::setPlaylist,
        )
    }
}

@Composable
internal fun OptionRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---- 하단 동작 영역 ----

@Composable
private fun BottomActionArea(state: MainUiState, viewModel: MainViewModel, onDownloadClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val download = state.downloadState
            if (download.isActive) {
                ProgressPanel(download, viewModel)
            } else {
                CompletionCard(state, viewModel)
                Button(
                    onClick = onDownloadClick,
                    enabled = state.downloadEnabled,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    when {
                        state.updating || state.engineStatus == EngineStatus.Initializing -> {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(if (state.updating) R.string.status_updating else R.string.status_engine_initializing))
                        }
                        else -> {
                            Icon(painterResource(R.drawable.ic_download), null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.download), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPanel(download: DownloadState, viewModel: MainViewModel) {
    val (headline, detail, progress) = when (download) {
        is DownloadState.Running -> Triple("${download.progress.toInt()}%", download.text, download.progress / 100f)
        is DownloadState.PostProcessing -> Triple(stringResource(R.string.progress_postprocessing), download.text, null)
        else -> Triple(stringResource(R.string.progress_preparing), stringResource(R.string.status_preparing), null)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = viewModel::cancelDownload) { Text(stringResource(R.string.cancel)) }
    }
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
    }
}

/** 완료·실패·취소 결과와 저장한 파일의 [열기]·[공유]·[폴더] (FR-7, FR-12). */
@Composable
private fun CompletionCard(state: MainUiState, viewModel: MainViewModel) {
    val download = state.downloadState
    val files = when (download) {
        is DownloadState.Succeeded -> download.files
        is DownloadState.Failed -> download.saved
        is DownloadState.Cancelled -> download.saved
        else -> return
    }
    val context = LocalContext.current
    val msgNotFound = stringResource(R.string.error_file_not_found)
    val msgNoApp = stringResource(R.string.error_no_app_to_open)
    val msgNoFilesApp = stringResource(R.string.error_no_files_app)
    val shareTitle = stringResource(R.string.share)

    // 사용자가 그사이 파일을 지웠을 수 있으므로 누를 때마다 남은 파일만 쓴다.
    fun withExisting(block: (List<SavedFile>) -> Unit) {
        val existing = FileActions.existing(context, files)
        if (existing.isEmpty()) viewModel.showMessage(msgNotFound) else block(existing)
    }
    fun launch(intent: Intent?, failure: String) {
        if (intent == null) return viewModel.showMessage(failure)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.showMessage(failure)
        }
    }

    val isError = download is DownloadState.Failed
    val (icon, title) = when (download) {
        is DownloadState.Succeeded -> Icons.Filled.CheckCircle to stringResource(R.string.result_succeeded)
        is DownloadState.Failed -> Icons.Filled.Warning to stringResource(R.string.status_failed_short)
        else -> Icons.Filled.Close to stringResource(R.string.status_cancelled)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (isError) TextButton(onClick = viewModel::showFailureDetail) { Text(stringResource(R.string.details)) }
                IconButton(onClick = viewModel::dismissResult) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
            }
            if (files.isNotEmpty()) {
                Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileSummary(files, partial = download !is DownloadState.Succeeded)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (download is DownloadState.Succeeded) {
                            FilledTonalButton(
                                onClick = { withExisting { launch(FileActions.openIntent(it.first()), msgNoApp) } },
                                contentPadding = ActionPadding,
                                modifier = Modifier.weight(1f),
                            ) { ActionLabel(painterResource(R.drawable.ic_open), stringResource(R.string.open)) }
                        }
                        OutlinedButton(
                            onClick = { withExisting { launch(FileActions.shareIntent(it, shareTitle), msgNoApp) } },
                            contentPadding = ActionPadding,
                            modifier = Modifier.weight(1f),
                        ) { ActionLabel(rememberVectorPainter(Icons.Filled.Share), shareTitle) }
                        OutlinedButton(
                            onClick = {
                                val subDir = ShareRules.commonSubDir(files.map { it.subDir })
                                launch(FileActions.folderIntent(context, subDir), msgNoFilesApp)
                            },
                            contentPadding = ActionPadding,
                            modifier = Modifier.weight(1f),
                        ) { ActionLabel(painterResource(R.drawable.ic_folder), stringResource(R.string.folder)) }
                    }
                }
            }
        }
    }
}

private val ActionPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
private fun ActionLabel(icon: Painter, label: String) {
    Icon(icon, null, Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(label, maxLines = 1)
}

/** 파일 하나면 이름·크기, 여러 개면 폴더 이름·개수. [partial]: 실패·취소 중 저장된 일부. */
@Composable
private fun FileSummary(files: List<SavedFile>, partial: Boolean) {
    val context = LocalContext.current
    val subDir = ShareRules.commonSubDir(files.map { it.subDir })
    val location = if (subDir == null) MediaStoreExporter.ROOT else "${MediaStoreExporter.ROOT}/$subDir"
    val totalSize = Formatter.formatShortFileSize(context, files.sumOf { it.size })
    val single = files.singleOrNull()
    val (iconRes, name, meta) = when {
        single != null -> Triple(
            when {
                single.mimeType.startsWith("audio/") -> R.drawable.ic_music
                single.mimeType.startsWith("video/") -> R.drawable.ic_movie
                else -> R.drawable.ic_download
            },
            single.name,
            "$totalSize · $location",
        )
        else -> Triple(
            R.drawable.ic_playlist,
            subDir ?: MediaStoreExporter.ROOT,
            stringResource(R.string.files_count, files.size) + " · $totalSize",
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (partial) stringResource(R.string.saved_partial, files.size) + " · $meta" else meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** `https://www.youtube.com/watch?v=x` → `youtube.com/watch?v=x` */
private fun shortUrl(url: String) = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")

/**
 * 화면이 포커스를 얻을 때 클립보드를 확인한다(Android 10+는 포커스가 있어야 읽을 수 있음).
 * 클립보드가 바뀌었을 때만 내용을 읽어, Android 12+의 "붙여넣음" 알림이 반복해서 뜨지 않게 한다.
 */
@Composable
private fun ClipboardWatcher(enabled: Boolean, onText: (String?) -> Unit) {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var lastTimestamp by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(focused, enabled) {
        if (!focused || !enabled) return@LaunchedEffect
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return@LaunchedEffect
        val description = manager.primaryClipDescription ?: return@LaunchedEffect
        if (description.timestamp == lastTimestamp) return@LaunchedEffect
        lastTimestamp = description.timestamp
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !description.hasMimeType("text/*")) return@LaunchedEffect
        onText(manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString())
    }
}

// ---- 대화상자 ----

@Composable
private fun Dialogs(state: MainUiState, viewModel: MainViewModel) {
    val failure = state.downloadState as? DownloadState.Failed
    if (failure != null && !failure.acknowledged) {
        val body = buildString {
            append(stringResource(R.string.dialog_exit_code, failure.exitCode?.toString() ?: stringResource(R.string.exit_code_unknown)))
            append("\n\n").append(failure.lastError)
            if (failure.hint.isNotEmpty()) append("\n\n").append(failure.hint)
            if (failure.savedCount > 0) append("\n\n").append(stringResource(R.string.status_saved_suffix, failure.savedCount))
        }
        AlertDialog(
            onDismissRequest = viewModel::acknowledgeFailure,
            icon = { Icon(Icons.Filled.Warning, null) },
            title = { Text(stringResource(R.string.dialog_download_failed_title)) },
            text = { Text(body) },
            confirmButton = { TextButton(onClick = viewModel::acknowledgeFailure) { Text(stringResource(R.string.ok)) } },
        )
    }
    when (val dialog = state.dialog) {
        is MainDialog.EngineFailed -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.dialog_error_title)) },
            text = { Text(stringResource(R.string.error_engine_init, dialog.message)) },
            confirmButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.ok)) } },
        )
        MainDialog.LogoutConfirm -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = { TextButton(onClick = viewModel::confirmLogout) { Text(stringResource(R.string.logout)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.cancel)) } },
        )
        is MainDialog.UpdateResult -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.update_ytdlp)) },
            text = { Text(dialog.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.ok)) } },
        )
        null -> Unit
    }
}
