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
import com.eyecare.pro.domain.cff.CffObservation
import java.util.Locale

@Composable
fun ResultsScreen(
    lux: Float?,
    lastCffObservation: CffObservation?,
    lastDistanceCm: Float?,
    task: LightTask = LightTask.SCREEN_WORK
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ringkasan pemeriksaan", style = MaterialTheme.typography.headlineSmall)
        if (lux == null) {
            Text("Belum ada pembacaan cahaya. Buka modul Pemeriksaan pencahayaan dan tunggu sensor memberikan nilai.")
        } else {
            val condition = LightConditionEvaluator.evaluate(lux, task)
            val status = when (condition) {
                LightCondition.TOO_DARK -> "Cahaya terlalu redup"
                LightCondition.IDEAL -> "Cahaya berada dalam rentang panduan aktivitas"
                LightCondition.TOO_BRIGHT -> "Cahaya terlalu terang"
            }
            val advice = when (condition) {
                LightCondition.TOO_DARK -> "Tambahkan lampu ruangan atau lampu meja yang menyinari bidang kerja."
                LightCondition.IDEAL -> "Pembacaan berada pada rentang panduan untuk " +
                    task.label.lowercase(Locale.getDefault()) + ". Tetap perhatikan silau dan bayangan."
                LightCondition.TOO_BRIGHT -> "Kurangi cahaya langsung, silau, atau pantulan layar."
            }
            Text("Aktivitas: " + task.label + " (" + task.lowerLux.toInt() + "–" + task.upperLux.toInt() + " lux)")
            Text(
                "Pembacaan terakhir: ${String.format(Locale.getDefault(), "%.0f", lux)} lux",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                status,
                style = MaterialTheme.typography.titleLarge,
                color = when (condition) {
                    LightCondition.TOO_DARK -> Color(0xFFC62828)
                    LightCondition.IDEAL -> Color(0xFF2E7D32)
                    LightCondition.TOO_BRIGHT -> Color(0xFFEF6C00)
                }
            )
            Text(advice)
        }

        Text("Catatan pengamatan", style = MaterialTheme.typography.titleMedium)
        Text(
            lastCffObservation?.let {
                "CFF/FFF: target ${String.format(Locale.getDefault(), "%.1f", it.requestedFrequencyHz)} Hz; " +
                    "${it.perception.displayLabel.lowercase(Locale.getDefault())}; refresh rate yang dilaporkan " +
                    "${String.format(Locale.getDefault(), "%.1f", it.reportedDisplayRefreshRateHz)} Hz. " +
                    "Ini respons subjektif, bukan ambang klinis."
            } ?: "CFF/FFF: belum ada catatan pengamatan."
        )
        Text(
            lastDistanceCm?.let {
                "Estimasi jarak wajah terakhir: ${String.format(Locale.getDefault(), "%.1f", it)} cm."
            } ?: "Estimasi jarak wajah: belum tersedia; lakukan kalibrasi kamera."
        )

        Text("Status modul", style = MaterialTheme.typography.titleMedium)
        Text("• Pemeriksaan cahaya: aktif jika perangkat memiliki sensor cahaya.")
        Text("• Ishihara: alur jawaban dan rekap respons tersedia secara kode, tetapi terkunci sampai materi resmi, izin, dan validasi edisi tersedia.")
        Text("• CFF/FFF: eksperimen persepsi subjektif; dibatasi secara teoritis oleh refresh rate dan tidak mendiagnosis kelelahan mata.")
        Text("• Jarak wajah: estimasi relatif yang memerlukan kalibrasi pengguna.")
        Text(
            "Ringkasan ini bersifat edukatif. Tidak ada hasil dalam aplikasi yang dapat memastikan atau menyingkirkan penyakit mata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
