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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

private data class DemoPlate(val number: String, val seed: Int)

private val demoPlates = listOf(
    DemoPlate("12", 121),
    DemoPlate("8", 238),
    DemoPlate("6", 346),
    DemoPlate("29", 429),
    DemoPlate("5", 551)
)

/**
 * Compose rendering layer. Circle packing, mask creation and color mapping
 * happen on Dispatchers.Default, not inside Canvas drawing.
 */
@Composable
private fun RealisticIshiharaPlate(
    plate: DemoPlate,
    palette: IshiharaPlateGenerator.Palette,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density

    val dots by produceState(
        initialValue = emptyList<IshiharaPlateGenerator.Dot>(),
        canvasSize,
        plate.number,
        plate.seed,
        palette,
        density
    ) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            value = withContext(Dispatchers.Default) {
                IshiharaPlateGenerator.generate(
                    width = canvasSize.width,
                    height = canvasSize.height,
                    density = density,
                    number = plate.number,
                    palette = palette,
                    seed = plate.seed
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged { canvasSize = it }
    ) {
        val diameter = size.minDimension * 0.96f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f

        drawCircle(
            color = Color(0xFFEEEBDD),
            radius = radius,
            center = center
        )

        val clip = androidx.compose.ui.graphics.Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                )
            )
        }

        clipPath(clip) {
            dots.forEach { dot ->
                drawCircle(
                    color = Color(dot.color),
                    radius = dot.radius,
                    center = androidx.compose.ui.geometry.Offset(dot.x, dot.y)
                )
            }
        }

        drawCircle(
            color = Color(0xFF536044),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun IshiharaScreen() {
    var currentIndex by remember { mutableStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    var paletteIndex by remember { mutableStateOf(0) }
    var patternRevision by remember { mutableStateOf(0) }
    val responses = remember { mutableStateListOf<String>() }

    val currentBase = demoPlates[currentIndex]
    val current = currentBase.copy(
        seed = currentBase.seed + patternRevision * 7919
    )
    val palette = if (paletteIndex == 0) {
        IshiharaPlateGenerator.defaultPalette()
    } else {
        IshiharaPlateGenerator.alternatePalette()
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Simulasi persepsi warna", style = MaterialTheme.typography.headlineSmall)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "SIMULASI EDUKATIF — BUKAN TES ISHIHARA RESMI",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Pelat titik berwarna di bawah dibuat secara prosedural untuk memberi gambaran cara kerja pola persepsi warna. Pola, warna, urutan, dan penilaiannya tidak tervalidasi secara klinis. Hasil tidak dapat menentukan buta warna atau menyatakan penglihatan normal.",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Text("Cara mencoba", style = MaterialTheme.typography.titleMedium)
        Text(
            "Atur kecerahan layar pada tingkat nyaman, matikan filter warna/blue light bila memungkinkan, pegang ponsel pada jarak nyaman, lalu lihat gambar tanpa memperbesar. Kondisi layar dan pengaturan warna tiap ponsel berbeda, jadi jawaban hanya pengalaman pribadi."
        )

        if (!completed) {
            Text(
                "Pelat simulasi ${currentIndex + 1} dari ${demoPlates.size}",
                style = MaterialTheme.typography.titleMedium
            )
            RealisticIshiharaPlate(
                plate = current,
                palette = palette,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { patternRevision++ }) {
                    Text("Acak pola")
                }
                OutlinedButton(onClick = { paletteIndex = (paletteIndex + 1) % 2 }) {
                    Text("Ganti palet")
                }
            }

            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.take(30) },
                label = { Text("Angka atau bentuk yang Anda lihat") },
                supportingText = {
                    Text("Jika tidak melihat angka, tulis “tidak terlihat” atau “ragu”.")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { answer = "Tidak terlihat" }) {
                    Text("Tidak terlihat")
                }
                OutlinedButton(onClick = { answer = "Ragu-ragu" }) {
                    Text("Ragu-ragu")
                }
            }
            Button(
                enabled = answer.isNotBlank(),
                onClick = {
                    responses.add(answer.trim())
                    answer = ""
                    if (currentIndex == demoPlates.lastIndex) {
                        completed = true
                    } else {
                        currentIndex++
                        patternRevision = 0
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (currentIndex == demoPlates.lastIndex) {
                        "Selesaikan simulasi"
                    } else {
                        "Simpan dan lanjut"
                    }
                )
            }
        } else {
            Text("Simulasi selesai", style = MaterialTheme.typography.titleLarge)
            Text(
                "Jawaban Anda telah dicatat untuk sesi ini. Catatan ini bukan skor medis dan tidak akan dikategorikan normal/abnormal."
            )
            responses.forEachIndexed { index, response ->
                Text("Pelat ${index + 1}: $response")
            }
            Button(
                onClick = {
                    currentIndex = 0
                    answer = ""
                    completed = false
                    patternRevision = 0
                    responses.clear()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ulangi simulasi")
            }
        }

        Text("Apa arti hasilnya?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Simulasi ini hanya menunjukkan bahwa tes persepsi warna meminta seseorang mengenali pola atau angka dari titik-titik dengan warna berbeda. Jika Anda kesulitan melihat pola, hasil simulasi ini tidak membuktikan adanya gangguan. Namun, bila Anda memiliki kekhawatiran tentang warna yang sulit dibedakan, sering keliru mengenali warna, atau membutuhkan kepastian (misalnya untuk pekerjaan/pendidikan), konsultasikan pemeriksaan warna resmi kepada dokter mata atau optometris."
        )
        Text(
            "Ishihara resmi terutama digunakan untuk membantu menilai gangguan merah–hijau. Pemeriksaan profesional dapat memilih tes yang sesuai, dalam pencahayaan dan kondisi terkontrol. Jangan menunda konsultasi hanya karena simulasi ini terasa mudah."
        )
        Text(
            "Segera cari pertolongan medis bila perubahan penglihatan warna terjadi mendadak, terutama disertai penglihatan menurun, nyeri mata, atau keluhan pada satu mata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
