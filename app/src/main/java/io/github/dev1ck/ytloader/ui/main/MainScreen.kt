package io.github.dev1ck.ytloader.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.dev1ck.ytloader.EngineStatus
import io.github.dev1ck.ytloader.R
import io.github.dev1ck.ytloader.download.DownloadState
import io.github.dev1ck.ytloader.download.FormatCatalog
import io.github.dev1ck.ytloader.download.Mode
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UrlRow(state, viewModel)
            ModeRow(state, viewModel)
            Dropdown(
                label = stringResource(R.string.label_format),
                selected = state.format.label,
                options = FormatCatalog.formats(state.mode).map { it.value to it.label },
                enabled = state.inputsEnabled,
                onSelect = viewModel::setFormat,
            )
            Dropdown(
                label = stringResource(if (state.mode == Mode.AUDIO) R.string.label_audio_quality else R.string.label_resolution),
                selected = state.quality.label,
                options = FormatCatalog.qualities(state.mode).map { it.value to it.label },
                enabled = state.qualityEnabled,
                onSelect = viewModel::setQuality,
            )
            Row {
                CheckRow(stringResource(R.string.option_thumbnail), state.embedThumbnail, state.thumbnailEnabled, viewModel::setEmbedThumbnail)
                Spacer(Modifier.width(16.dp))
                CheckRow(stringResource(R.string.option_metadata), state.embedMetadata, state.inputsEnabled, viewModel::setEmbedMetadata)
            }
            CheckRow(stringResource(R.string.option_playlist), state.playlist, state.inputsEnabled, viewModel::setPlaylist)

            ProgressArea(state)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::cancelDownload, enabled = state.cancelEnabled, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onDownloadClick, enabled = state.downloadEnabled, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.download))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.ytDlpVersion?.let { "v$it" } ?: stringResource(R.string.version_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = viewModel::updateYtDlp, enabled = state.updateEnabled) {
                    Text(stringResource(R.string.update_ytdlp))
                }
            }
        }
    }

    Dialogs(state, viewModel)
}

@Composable
private fun UrlRow(state: MainUiState, viewModel: MainViewModel) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::setUrl,
            label = { Text(stringResource(R.string.label_url)) },
            singleLine = true,
            enabled = state.inputsEnabled,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(context)?.toString()
                    if (!text.isNullOrEmpty()) viewModel.setUrl(text)
                }
            },
            enabled = state.inputsEnabled,
        ) { Text(stringResource(R.string.paste)) }
    }
}

@Composable
private fun ModeRow(state: MainUiState, viewModel: MainViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.label_mode), modifier = Modifier.width(56.dp))
        for ((mode, label) in listOf(Mode.AUDIO to R.string.mode_audio, Mode.VIDEO to R.string.mode_video)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(state.mode == mode, enabled = state.inputsEnabled, role = Role.RadioButton) { viewModel.setMode(mode) }
                    .padding(end = 12.dp),
            ) {
                RadioButton(selected = state.mode == mode, onClick = null, enabled = state.inputsEnabled)
                Text(stringResource(label), color = contentColor(state.inputsEnabled))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            for ((value, text) in options) {
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(value)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, color = contentColor(enabled))
    }
}

@Composable
private fun contentColor(enabled: Boolean) =
    if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

@Composable
private fun ProgressArea(state: MainUiState) {
    val text = statusText(state) ?: return
    val progress = when (val d = state.downloadState) {
        is DownloadState.Running -> d.progress / 100f
        is DownloadState.PostProcessing, is DownloadState.Succeeded -> 1f
        else -> 0f
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.downloadState !is DownloadState.Idle) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun statusText(state: MainUiState): String? {
    val savedCount = when (val d = state.downloadState) {
        is DownloadState.Failed -> d.savedCount
        is DownloadState.Cancelled -> d.savedCount
        else -> 0
    }
    val saved = if (savedCount > 0) " " + stringResource(R.string.status_saved_suffix, savedCount) else ""
    if (state.updating) return stringResource(R.string.status_updating)
    return when (val d = state.downloadState) {
        DownloadState.Idle ->
            if (state.engineStatus == EngineStatus.Initializing) stringResource(R.string.status_engine_initializing) else null
        DownloadState.Preparing -> stringResource(R.string.status_preparing)
        is DownloadState.Running -> d.text
        is DownloadState.PostProcessing -> d.text
        is DownloadState.Succeeded -> stringResource(R.string.status_succeeded)
        is DownloadState.Failed -> stringResource(R.string.status_failed) + saved
        is DownloadState.Cancelled -> stringResource(R.string.status_cancelled) + saved
    }
}

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
        is MainDialog.UpdateResult -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.update_ytdlp)) },
            text = { Text(dialog.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissDialog) { Text(stringResource(R.string.ok)) } },
        )
        null -> Unit
    }
}
