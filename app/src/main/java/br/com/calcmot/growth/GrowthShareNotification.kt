package br.com.calcmot.growth

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.calcmot.MainActivity
import br.com.calcmot.R

/**
 * Local opt-in-by-existing-permission reminder: no FCM, alarms, scheduler or background drive trigger.
 * It is posted only on a subsequent voluntary return to the HOME screen.
 */
internal object GrowthShareNotification {
    const val ACTION_SHARE = "br.com.calcmot.growth.SHARE_FROM_NOTIFICATION"
    private const val CHANNEL_ID = "calcmot_growth_invites_v1"
    private const val NOTIFICATION_ID = 24051

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context): Boolean {
        if (!canNotify(context)) return false
        return runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "Convites para compartilhar", NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Convites opcionais e pouco frequentes para indicar o CalcMot"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
                if (manager.getNotificationChannel(CHANNEL_ID)?.importance ==
                    NotificationManager.IMPORTANCE_NONE
                ) return false
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_SHARE
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pending = PendingIntent.getActivity(
                context, 24051, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_growth_share_notification)
                .setContentTitle("Gostando do CalcMot?")
                .setContentText("Se ele está ajudando, compartilhe com outro motorista.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
