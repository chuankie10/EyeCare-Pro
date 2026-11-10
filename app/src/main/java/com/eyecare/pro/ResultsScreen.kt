package com.eyecare.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ResultsScreen(lux: Float?) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ringkasan pemeriksaan", style = MaterialTheme.typography.headlineSmall)
        if (lux == null) {
            Text("Belum ada pembacaan cahaya. Buka modul Pemeriksaan pencahayaan dan tunggu sensor memberikan nilai.")
        } else {
            val condition = LightConditionEvaluator.evaluate(lux)
            val status = when (condition) {
                LightCondition.TOO_DARK -> "Cahaya terlalu redup"
                LightCondition.IDEAL -> "Cahaya berada dalam rentang operasional"
                LightCondition.TOO_BRIGHT -> "Cahaya terlalu terang"
            }
            val advice = when (condition) {
                LightCondition.TOO_DARK -> "Tambahkan pencahayaan ruangan dan hindari melakukan tes pada kondisi gelap."
                LightCondition.IDEAL -> "Kondisi pencahayaan memenuhi ambang operasional aplikasi. Lanjutkan hanya jika Anda merasa nyaman."
                LightCondition.TOO_BRIGHT -> "Kurangi cahaya langsung, silau, atau pantulan layar."
            }
            Text("Pembacaan terakhir: ${String.format(Locale.getDefault(), "%.0f", lux)} lux", style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.titleLarge, color = when (condition) {
                LightCondition.TOO_DARK -> Color(0xFFC62828)
                LightCondition.IDEAL -> Color(0xFF2E7D32)
                LightCondition.TOO_BRIGHT -> Color(0xFFEF6C00)
            })
            Text(advice)
        }
        Text("Status modul", style = MaterialTheme.typography.titleMedium)
        Text("• Pemeriksaan cahaya: aktif jika perangkat memiliki sensor cahaya.")
        Text("• Ishihara: menunggu pelat resmi berlisensi dan tervalidasi.")
        Text("• CFF: eksperimen visual non-klinis, dipengaruhi karakteristik layar.")
        Text("• Jarak wajah: estimasi relatif yang memerlukan kalibrasi pengguna.")
        Text(
            "Ringkasan ini bersifat edukatif. Tidak ada hasil dalam aplikasi yang dapat memastikan atau menyingkirkan penyakit mata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
