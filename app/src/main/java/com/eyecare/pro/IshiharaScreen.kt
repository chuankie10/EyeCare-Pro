package com.eyecare.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class DemoPlate(val number: String, val seed: Int)

private val demoPlates = listOf(
    DemoPlate("12", 121),
    DemoPlate("8", 238),
    DemoPlate("6", 346),
    DemoPlate("29", 429),
    DemoPlate("5", 551)
)

private val digitPixels = mapOf(
    '0' to listOf("01110","10001","10011","10101","11001","10001","01110"),
    '1' to listOf("00100","01100","00100","00100","00100","00100","01110"),
    '2' to listOf("01110","10001","00001","00010","00100","01000","11111"),
    '3' to listOf("11110","00001","00001","01110","00001","00001","11110"),
    '4' to listOf("00010","00110","01010","10010","11111","00010","00010"),
    '5' to listOf("11111","10000","10000","11110","00001","00001","11110"),
    '6' to listOf("01110","10000","10000","11110","10001","10001","01110"),
    '7' to listOf("11111","00001","00010","00100","01000","01000","01000"),
    '8' to listOf("01110","10001","10001","01110","10001","10001","01110"),
    '9' to listOf("01110","10001","10001","01111","00001","00001","01110")
)

private fun digitAt(number: String, x: Float, y: Float): Boolean {
    val rows = digitPixels[number.first()] ?: return false
    val columnsPerDigit = 5
    val gap = 1
    val totalColumns = number.length * columnsPerDigit + (number.length - 1) * gap
    val column = (x * totalColumns).toInt()
    val row = (y * 7f).toInt()
    if (row !in 0..6 || column !in 0 until totalColumns) return false
    val digitIndex = column / (columnsPerDigit + gap)
    val localColumn = column % (columnsPerDigit + gap)
    if (digitIndex >= number.length || localColumn >= columnsPerDigit) return false
    val glyph = digitPixels[number[digitIndex]] ?: rows
    return glyph[row][localColumn] == '1'
}

@Composable
private fun DemoColorPlate(plate: DemoPlate, modifier: Modifier = Modifier) {
    val backgroundDots = listOf(Color(0xFF789B50), Color(0xFFA7AD55), Color(0xFF6D8750), Color(0xFFC1B56A), Color(0xFF8E9F59))
    val foregroundDots = listOf(Color(0xFFC96A45), Color(0xFFDA8754), Color(0xFFB95D4D), Color(0xFFD7A05B), Color(0xFFAA654C))
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val diameter = size.minDimension * 0.96f
        val left = (size.width - diameter) / 2f
        val top = (size.height - diameter) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(left, top, left + diameter, top + diameter)) }
        clipPath(circle) {
            val random = Random(plate.seed)
            val spacing = diameter / 22f
            for (row in 0..21) {
                for (column in 0..21) {
                    val jitterX = (random.nextFloat() - 0.5f) * spacing * 0.72f
                    val jitterY = (random.nextFloat() - 0.5f) * spacing * 0.72f
                    val x = left + (column + 0.5f) * spacing + jitterX
                    val y = top + (row + 0.5f) * spacing + jitterY
                    val nx = ((x - left) / diameter).coerceIn(0f, 0.999f)
                    val ny = ((y - top) / diameter).coerceIn(0f, 0.999f)
                    val isNumber = digitAt(plate.number, nx, ny)
                    val palette = if (isNumber) foregroundDots else backgroundDots
                    val color = palette[random.nextInt(palette.size)]
                    val dotRadius = spacing * (0.27f + random.nextFloat() * 0.17f)
                    if ((Offset(x, y) - center).getDistance() < radius) {
                        drawCircle(color, dotRadius, Offset(x, y))
                    }
                }
            }
        }
        drawCircle(Color(0xFF536044), radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun IshiharaScreen() {
    var currentIndex by remember { mutableStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    val responses = remember { mutableStateListOf<String>() }
    val current = demoPlates[currentIndex]

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Simulasi persepsi warna", style = MaterialTheme.typography.headlineSmall)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SIMULASI EDUKATIF — BUKAN TES ISHIHARA RESMI", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Pelat titik berwarna di bawah dibuat khusus untuk memberi gambaran cara kerja tes persepsi warna. Gambar, pola, warna, urutan, dan penilaiannya tidak tervalidasi secara klinis. Hasil tidak dapat menentukan buta warna atau menyatakan penglihatan normal.", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Text("Cara mencoba", style = MaterialTheme.typography.titleMedium)
        Text("Atur kecerahan layar pada tingkat nyaman, matikan filter warna/blue light bila memungkinkan, pegang ponsel pada jarak nyaman, lalu lihat gambar tanpa memperbesar. Kondisi layar dan pengaturan warna tiap ponsel berbeda, jadi jawaban hanya pengalaman pribadi.")
        if (!completed) {
            Text("Pelat simulasi ${currentIndex + 1} dari ${demoPlates.size}", style = MaterialTheme.typography.titleMedium)
            DemoColorPlate(current, Modifier.padding(horizontal = 12.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.take(30) },
                label = { Text("Angka atau bentuk yang Anda lihat") },
                supportingText = { Text("Jika tidak melihat angka, tulis “tidak terlihat” atau “ragu”.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { answer = "Tidak terlihat" }) { Text("Tidak terlihat") }
                OutlinedButton(onClick = { answer = "Ragu-ragu" }) { Text("Ragu-ragu") }
            }
            Button(
                enabled = answer.isNotBlank(),
                onClick = {
                    responses.add(answer.trim())
                    answer = ""
                    if (currentIndex == demoPlates.lastIndex) completed = true else currentIndex++
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (currentIndex == demoPlates.lastIndex) "Selesaikan simulasi" else "Simpan dan lanjut") }
        } else {
            Text("Simulasi selesai", style = MaterialTheme.typography.titleLarge)
            Text("Jawaban Anda telah dicatat untuk sesi ini. Catatan ini bukan skor medis dan tidak akan dikategorikan normal/abnormal.")
            responses.forEachIndexed { index, response ->
                Text("Pelat ${index + 1}: $response")
            }
            Button(onClick = {
                currentIndex = 0
                answer = ""
                completed = false
                responses.clear()
            }, modifier = Modifier.fillMaxWidth()) { Text("Ulangi simulasi") }
        }
        Text("Apa arti hasilnya?", style = MaterialTheme.typography.titleMedium)
        Text("Simulasi ini hanya menunjukkan bahwa tes persepsi warna meminta seseorang mengenali pola atau angka dari titik-titik dengan warna berbeda. Jika Anda kesulitan melihat pola, hasil simulasi ini tidak membuktikan adanya gangguan. Namun, bila Anda memiliki kekhawatiran tentang warna yang sulit dibedakan, sering keliru mengenali warna, atau membutuhkan kepastian (misalnya untuk pekerjaan/pendidikan), konsultasikan pemeriksaan warna resmi kepada dokter mata atau optometris.")
        Text("Ishihara resmi terutama digunakan untuk membantu menilai gangguan merah–hijau. Pemeriksaan profesional dapat memilih tes yang sesuai, dalam pencahayaan dan kondisi terkontrol. Jangan menunda konsultasi hanya karena simulasi ini terasa mudah.")
        Text("Segera cari pertolongan medis bila perubahan penglihatan warna terjadi mendadak, terutama disertai penglihatan menurun, nyeri mata, atau keluhan pada satu mata.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
