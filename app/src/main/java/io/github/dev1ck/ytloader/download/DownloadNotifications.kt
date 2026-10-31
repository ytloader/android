package io.github.dev1ck.ytloader.download

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.dev1ck.ytloader.MainActivity
import io.github.dev1ck.ytloader.R

/** 알림 채널·빌더 (ARCHITECTURE 7장). */
class DownloadNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_PROGRESS, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.channel_progress)).build(),
                NotificationChannelCompat.Builder(CHANNEL_RESULT, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_result)).build(),
            ),
        )
    }

    /** [progress]: 0–100, null이면 진행률 미정. */
    fun progress(text: String, progress: Float?): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notification_progress_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(1000, ((progress ?: 0f) * 10).toInt().coerceIn(0, 1000), progress == null)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.cancel), cancelIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun notifyProgress(text: String, progress: Float?) = post(ID_PROGRESS, progress(text, progress))

    fun showSucceeded(uris: List<Uri>, mimeOfFirst: String?) {
        val first = uris.firstOrNull()
        val tap = if (first == null) openAppIntent() else {
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(first, mimeOfFirst ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(context, REQ_VIEW, view, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        post(ID_RESULT, result(context.getString(R.string.notification_succeeded_title), context.getString(R.string.status_succeeded), tap))
    }

    fun showFailed(text: String) =
        post(ID_RESULT, result(context.getString(R.string.notification_failed_title), text, openAppIntent()))

    private fun result(title: String, text: String, tap: PendingIntent): Notification =
        NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

    private fun post(id: Int, notification: Notification) {
        // 권한이 없으면 조용히 건너뛴다 (진행은 앱 화면에서 보임 — SPEC 7장).
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        manager.notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, REQ_OPEN,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelIntent(): PendingIntent = PendingIntent.getService(
        context, REQ_CANCEL,
        Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_PROGRESS = "download_progress"
        const val CHANNEL_RESULT = "download_result"
        const val ID_PROGRESS = 1
        const val ID_RESULT = 2
        private const val REQ_OPEN = 0
        private const val REQ_CANCEL = 1
        private const val REQ_VIEW = 2
    }
}
