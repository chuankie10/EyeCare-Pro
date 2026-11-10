package com.eyecare.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "eyecare-20-20-20-reminder"

@Composable
fun ReminderScreen() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("eyecare_settings", android.content.Context.MODE_PRIVATE) }
    var scheduled by remember { mutableStateOf(preferences.getBoolean("reminder_enabled", false)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scheduleEyeBreakReminder(context)
            scheduled = true
            preferences.edit().putBoolean("reminder_enabled", true).apply()
        }
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pengingat 20-20-20", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Setiap 20 menit, alihkan pandangan ke objek sekitar 6 meter selama 20 detik. Pengingat dijalankan oleh sistem dan dapat tertunda mengikuti kebijakan baterai Android.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = {
            val hasPermission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                scheduleEyeBreakReminder(context)
                scheduled = true
                preferences.edit().putBoolean("reminder_enabled", true).apply()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }) {
            Text("Aktifkan pengingat")
        }
        Button(onClick = {
            WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
            scheduled = false
            preferences.edit().putBoolean("reminder_enabled", false).apply()
        }) {
            Text("Nonaktifkan pengingat")
        }
        Text(
            if (scheduled) "Pengingat telah dijadwalkan. Notifikasi pertama mengikuti interval WorkManager."
            else "Pengingat belum diaktifkan dari layar ini.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Catatan: Android dapat menunda pekerjaan berkala demi efisiensi baterai. Jadwal ini bukan alarm dengan ketepatan menit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun scheduleEyeBreakReminder(context: android.content.Context) {
    ReminderNotifications.ensureChannel(context)
    val request = PeriodicWorkRequestBuilder<ReminderWorker>(20, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        REMINDER_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}
