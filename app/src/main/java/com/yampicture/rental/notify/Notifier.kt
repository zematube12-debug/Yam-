package com.yampicture.rental.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yampicture.rental.MainActivity
import com.yampicture.rental.R

object Notifier {
    const val CH_ALERTS = "rental_alerts"
    const val CH_REPORT = "monthly_report"

    const val KIND_DUE = 1
    const val KIND_WARN = 2
    const val KIND_ADDED = 3

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CH_ALERTS,
                ctx.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = ctx.getString(R.string.channel_alerts_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_REPORT,
                ctx.getString(R.string.channel_report),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = ctx.getString(R.string.channel_report_desc) },
        )
    }

    fun notificationId(rentalId: Long, kind: Int): Int = (rentalId * 10L + kind).toInt()

    @SuppressLint("MissingPermission")
    fun post(
        ctx: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        route: String?,
    ) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra("route", route)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(
                if (channel == CH_ALERTS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            val manager = NotificationManagerCompat.from(ctx)
            if (manager.areNotificationsEnabled()) manager.notify(id, n)
        } catch (_: SecurityException) {
            // Notification permission was refused; nothing to do.
        }
    }

    fun cancelFor(ctx: Context, rentalId: Long) {
        val m = NotificationManagerCompat.from(ctx)
        m.cancel(notificationId(rentalId, KIND_DUE))
        m.cancel(notificationId(rentalId, KIND_WARN))
        m.cancel(notificationId(rentalId, KIND_ADDED))
    }
}
