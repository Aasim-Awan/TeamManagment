package com.teamfund.manager.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.teamfund.manager.R

object NotificationHelper {

    private const val CHANNEL_ID_TEAMFUND = "teamfund_reminders_channel"
    private const val CHANNEL_NAME = "TeamFund Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TEAMFUND,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for team contributions, settlements, and upcoming matches"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showLocalReminder(context: Context, title: String, message: String, notificationId: Int = 101) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TEAMFUND)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
