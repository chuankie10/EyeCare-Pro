package com.eyecare.pro

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        ReminderNotifications.ensureChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Waktunya istirahatkan mata")
            .setContentText("Lihat objek sekitar 6 meter selama 20 detik. Terapkan aturan 20-20-20.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(ReminderNotifications.NOTIFICATION_ID, notification)
        return Result.success()
    }
}

object ReminderNotifications {
    const val CHANNEL_ID = "eyecare_break_reminders"
    const val NOTIFICATION_ID = 2020

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat istirahat mata",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat aturan 20-20-20 setiap 20 menit"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
