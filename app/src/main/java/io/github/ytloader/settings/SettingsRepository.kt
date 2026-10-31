package io.github.ytloader.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.ytloader.download.FormatCatalog
import io.github.ytloader.download.Mode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** FR-11. 카탈로그의 값 문자열로 저장한다 (인덱스 아님). */
data class Settings(
    val mode: Mode = Mode.AUDIO,
    val audioFormat: String = FormatCatalog.audioFormats.first().value,
    val audioQuality: String = FormatCatalog.audioQualities.first().value,
    val videoFormat: String = FormatCatalog.videoFormats.first().value,
    val videoResolution: String = FormatCatalog.videoResolutions.first().value,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val playlist: Boolean = false,
    val useCookies: Boolean = true,
    val notificationPermissionAsked: Boolean = false,
    /** 저장 위치 SAF 트리 URI. null이면 기본 위치(Download/YTLoader). */
    val saveFolder: String? = null,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val mode = stringPreferencesKey("mode")
        val audioFormat = stringPreferencesKey("audio_format")
        val audioQuality = stringPreferencesKey("audio_quality")
        val videoFormat = stringPreferencesKey("video_format")
        val videoResolution = stringPreferencesKey("video_resolution")
        val embedThumbnail = booleanPreferencesKey("embed_thumbnail")
        val embedMetadata = booleanPreferencesKey("embed_metadata")
        val playlist = booleanPreferencesKey("playlist")
        val useCookies = booleanPreferencesKey("use_cookies")
        val notificationPermissionAsked = booleanPreferencesKey("notification_permission_asked")
        val saveFolder = stringPreferencesKey("save_folder")
    }

    val settings = context.dataStore.data.map { it.toSettings() }

    suspend fun load(): Settings = settings.first()

    suspend fun save(s: Settings) {
        context.dataStore.edit {
            it[Keys.mode] = s.mode.name
            it[Keys.audioFormat] = s.audioFormat
            it[Keys.audioQuality] = s.audioQuality
            it[Keys.videoFormat] = s.videoFormat
            it[Keys.videoResolution] = s.videoResolution
            it[Keys.embedThumbnail] = s.embedThumbnail
            it[Keys.embedMetadata] = s.embedMetadata
            it[Keys.playlist] = s.playlist
            it[Keys.useCookies] = s.useCookies
            it[Keys.notificationPermissionAsked] = s.notificationPermissionAsked
            if (s.saveFolder == null) it.remove(Keys.saveFolder) else it[Keys.saveFolder] = s.saveFolder
        }
    }

    // 알 수 없는 값이면 카탈로그 첫 항목으로.
    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            mode = Mode.entries.firstOrNull { it.name == this[Keys.mode] } ?: defaults.mode,
            audioFormat = FormatCatalog.format(Mode.AUDIO, this[Keys.audioFormat]).value,
            audioQuality = FormatCatalog.quality(Mode.AUDIO, this[Keys.audioQuality]).value,
            videoFormat = FormatCatalog.format(Mode.VIDEO, this[Keys.videoFormat]).value,
            videoResolution = FormatCatalog.quality(Mode.VIDEO, this[Keys.videoResolution]).value,
            embedThumbnail = this[Keys.embedThumbnail] ?: defaults.embedThumbnail,
            embedMetadata = this[Keys.embedMetadata] ?: defaults.embedMetadata,
            playlist = this[Keys.playlist] ?: defaults.playlist,
            useCookies = this[Keys.useCookies] ?: defaults.useCookies,
            notificationPermissionAsked = this[Keys.notificationPermissionAsked] ?: defaults.notificationPermissionAsked,
            saveFolder = this[Keys.saveFolder],
        )
    }
}
