package com.eyecare.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IshiharaScreen() {
    var plateNumber by remember { mutableStateOf("1") }
    var visibleAnswer by remember { mutableStateOf("") }
    val entries = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Skrining persepsi warna merah–hijau", style = MaterialTheme.typography.headlineSmall)
        Text("Status: gambar pelat Ishihara belum tersedia. Tes Ishihara yang sah memerlukan pelat asli berlisensi, urutan dan kunci penilaian dari edisi resmi. Kami tidak akan membuat gambar tiruan atau menghitung skor dari gambar yang tidak tervalidasi.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Text("Panduan penggunaan pelat resmi", style = MaterialTheme.typography.titleMedium)
        Text("Jika Anda memiliki buku Ishihara resmi atau sedang menjalani tes dengan tenaga kesehatan: ikuti petunjuk edisi tersebut. Salah satu protokol yang dipakai otoritas penerbangan menggunakan jarak sekitar 75 cm, pelat tegak lurus terhadap garis pandang, pencahayaan daylight/daylight-simulated sekitar 6500 K, dan waktu sekitar 3 detik per pelat. Protokol ini bukan pengganti petunjuk produsen dan tidak menjadikan tampilan HP setara buku tes.")
        Text("Catat jawaban dari pelat resmi", style = MaterialTheme.typography.titleMedium)
        Text("Kolom berikut hanya untuk mencatat jawaban Anda ketika melihat pelat pada buku resmi atau saat pemeriksaan. Aplikasi belum menilai benar/salah dan tidak menghasilkan diagnosis.")
        OutlinedTextField(
            value = plateNumber,
            onValueChange = { plateNumber = it.filter(Char::isDigit).take(3) },
            label = { Text("Nomor pelat") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = visibleAnswer,
            onValueChange = { visibleAnswer = it.take(40) },
            label = { Text("Apa angka/bentuk yang Anda lihat? (atau tulis tidak terlihat)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = plateNumber.isNotBlank() && visibleAnswer.isNotBlank(),
            onClick = {
                entries.add("Pelat " + plateNumber + ": " + visibleAnswer.trim())
                plateNumber = (plateNumber.toIntOrNull()?.plus(1) ?: 1).toString()
                visibleAnswer = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan jawaban") }
        if (entries.isNotEmpty()) {
            Text("Catatan jawaban", style = MaterialTheme.typography.titleMedium)
            entries.forEach { Text("• $it") }
            Button(onClick = { entries.clear(); plateNumber = "1" }) { Text("Hapus semua catatan") }
        }
        Text("Bagaimana memahami hasil?", style = MaterialTheme.typography.titleMedium)
        Text("Kesalahan atau kesulitan mengenali beberapa pelat dapat menjadi alasan untuk mengulang pemeriksaan dengan prosedur resmi dan berkonsultasi dengan dokter mata/optometris. Jangan menyimpulkan jenis atau tingkat buta warna dari catatan manual ini. Ishihara terutama menilai gangguan merah–hijau dan tidak mencakup semua gangguan penglihatan warna.")
        Text("Segera periksakan diri jika perubahan penglihatan warna muncul mendadak, terutama bila disertai penurunan penglihatan, nyeri, atau gejala pada satu mata. Jangan menunda pemeriksaan hanya karena hasil skrining ponsel tampak normal.")
        Text("Referensi protokol: UK Civil Aviation Authority, Colour Vision Guidance Material. Aset tes resmi tetap harus diperoleh dengan izin pemegang hak sebelum tes digital diaktifkan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
