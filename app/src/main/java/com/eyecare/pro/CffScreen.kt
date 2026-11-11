package com.eyecare.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CffScreen(onResult: (Float) -> Unit = {}) {
    var frequency by remember { mutableFloatStateOf(5f) }
    var running by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var observation by remember { mutableStateOf("BELUM_DIPILIH") }
    var recordedHz by remember { mutableFloatStateOf(0f) }
    val halfCycleMs = CffStimulusTiming.halfCycleMillis(frequency)

    LaunchedEffect(running, halfCycleMs) {
        if (!running) {
            flashOn = false
        } else {
            while (true) {
                flashOn = !flashOn
                delay(halfCycleMs)
            }
        }
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Eksperimen kedip (CFF)", style = MaterialTheme.typography.headlineSmall)
        Text("Tujuan: mengamati kapan kedipan mulai tampak menyatu menjadi cahaya stabil. Ini eksperimen persepsi, bukan pemeriksaan penyakit atau tes baku klinis.")
        Text("Panduan mulai dari awal", style = MaterialTheme.typography.titleMedium)
        Text("1. Istirahatkan mata dan duduk nyaman. Jangan lakukan bila sensitif terhadap cahaya, memiliki riwayat kejang yang dipicu kilatan, atau merasa tidak nyaman.\n2. Mulai dari 5 Hz. Amati bidang di bawah.\n3. Naikkan frekuensi perlahan, misalnya 1–2 Hz setiap langkah. Jangan mengejar angka tertentu.\n4. Catat saat kedipan terasa menyatu, masih berkedip, atau Anda tidak yakin. Hentikan jika pusing, sakit kepala, mual, atau mata tidak nyaman.")
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp)
                .background(if (running && flashOn) Color.White else Color.Black, RoundedCornerShape(16.dp))
        )
        Text("Frekuensi target: ${String.format(Locale.getDefault(), "%.0f", frequency)} Hz", style = MaterialTheme.typography.titleLarge)
        Slider(value = frequency, onValueChange = { frequency = it }, valueRange = 5f..30f, steps = 24, enabled = !running)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { running = !running }) {
                Text(if (running) "Hentikan" else "Mulai pada frekuensi ini")
            }
            OutlinedButton(enabled = !running && frequency < 30f, onClick = { frequency = (frequency + 1f).coerceAtMost(30f) }) { Text("+1 Hz") }
            OutlinedButton(enabled = !running && frequency > 5f, onClick = { frequency = (frequency - 1f).coerceAtLeast(5f) }) { Text("-1 Hz") }
        }
        Text("Apa yang Anda lihat saat frekuensi ini?", style = MaterialTheme.typography.titleMedium)
        listOf(
            "BERKEDIP" to "Masih terlihat berkedip",
            "MENYATU" to "Terlihat menyatu / stabil",
            "TIDAK_YAKIN" to "Tidak yakin / sulit diamati"
        ).forEach { (value, label) ->
            OutlinedButton(
                enabled = !running,
                onClick = { observation = value },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (observation == value) "✓ $label" else label) }
        }
        Button(
            enabled = !running && observation != "BELUM_DIPILIH",
            onClick = { recordedHz = frequency; onResult(frequency) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan catatan pengamatan") }
        if (recordedHz > 0f) {
            Text("Catatan terakhir: ${String.format(Locale.getDefault(), "%.0f", recordedHz)} Hz — " + when (observation) {
                "MENYATU" -> "dilaporkan tampak stabil"
                "BERKEDIP" -> "dilaporkan masih berkedip"
                else -> "pengamatan tidak pasti"
            })
        }
        Text("Cara memahami hasil", style = MaterialTheme.typography.titleMedium)
        Text("Jika masih berkedip pada frekuensi yang lebih tinggi, itu hanya berarti Anda masih melihat kedipan pada kondisi layar dan saat itu. Jika tampak menyatu pada frekuensi yang lebih rendah, catat sebagai pengamatan pribadi—bukan bukti gangguan. Tidak ada target Hz universal yang dapat dipakai aplikasi ponsel untuk menyatakan mata normal atau sakit.")
        Text("Ponsel tidak dapat menjamin frekuensi kilatan aktual: refresh rate, frame pacing Android, PWM, kecerahan, dan panel layar memengaruhi stimulus. Karena itu angka ini tidak boleh dipakai untuk diagnosis, terapi, atau keputusan rujukan sendirian. Bila ada kilatan cahaya baru di penglihatan, banyak floaters baru, bayangan seperti tirai, atau kehilangan penglihatan mendadak, cari pertolongan mata segera; jangan mengulang tes.")
    }
}
