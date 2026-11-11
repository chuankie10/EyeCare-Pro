package com.eyecare.pro

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyecare.pro.domain.ishihara.IshiharaScoringEngine
import com.eyecare.pro.presentation.ishihara.IshiharaUiState
import com.eyecare.pro.presentation.ishihara.IshiharaViewModel

@Composable
fun IshiharaScreen(viewModel: IshiharaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tes warna Ishihara", style = MaterialTheme.typography.headlineSmall)

        when (val current = state) {
            IshiharaUiState.Unavailable -> {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tes belum tersedia", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Repository ini belum berisi pelat Ishihara asli yang lisensinya dan " +
                                "validitas edisinya telah diperiksa. Karena itu tes tetap dikunci."
                        )
                    }
                }
                Text(
                    "Tidak ada pola acak, angka buatan, atau gambar tiruan yang digunakan sebagai " +
                        "pengganti materi tes."
                )
                Text(
                    "Sebelum diaktifkan, proyek memerlukan: (1) izin tertulis untuk distribusi " +
                        "digital, (2) identitas edisi dan sumber resmi, (3) kunci serta aturan " +
                        "penilaian edisi tersebut, dan (4) bukti validasi untuk cara penyajian " +
                        "pada layar ponsel."
                )
                Text(
                    "Tanpa materi dan aturan resmi tersebut, aplikasi tidak dapat memberikan " +
                        "indikasi gangguan buta warna. Pemeriksaan profesional tetap diperlukan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is IshiharaUiState.Question -> {
                val context = LocalContext.current
                val plateBitmap = remember(context, current.plate.imageAssetPath) {
                    runCatching {
                        context.assets.open(current.plate.imageAssetPath).use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }

                Text(
                    "Pelat \${current.currentQuestion} dari \${current.totalQuestions}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Sumber materi: \${current.sourceTitle}")
                if (plateBitmap == null) {
                    Text(
                        "Aset pelat tidak dapat dimuat. Sesi dihentikan agar jawaban tidak " +
                            "dikumpulkan dari materi yang tidak tampil."
                    )
                    Button(onClick = viewModel::abortBecauseAssetCannotBeLoaded) {
                        Text("Tutup sesi")
                    }
                } else {
                    Image(
                        bitmap = plateBitmap,
                        contentDescription = "Pelat tes warna resmi",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    )
                    Text("Pilih jawaban yang Anda lihat:")
                    current.plate.answerOptions.forEach { option ->
                        OutlinedButton(
                            onClick = { viewModel.submitAnswer(option) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.submitAnswer(IshiharaScoringEngine.UNSURE_ANSWER)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tidak tahu / tidak terlihat")
                    }
                    Text(
                        "Jangan mengubah warna gambar, memakai filter warna, atau memperbesar " +
                            "gambar. Kondisi layar tetap dapat memengaruhi hasil.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is IshiharaUiState.Completed -> {
                Text("Sesi selesai", style = MaterialTheme.typography.titleLarge)
                Text("Sumber materi: \${current.sourceTitle}")
                Text(
                    "Jawaban benar: \${current.score.correctAnswers} dari " +
                        "\${current.score.totalQuestions}"
                )
                Text("Jawaban terisi: \${current.score.answeredQuestions}")
                Text("Tidak dijawab: \${current.score.unansweredQuestions}")
                Text(
                    "Indikasi klinis: tidak dapat ditentukan oleh aplikasi ini. Rekap jumlah " +
                        "jawaban bukan diagnosis dan tidak boleh diterjemahkan menjadi status " +
                        "normal atau gangguan warna tanpa aturan interpretasi resmi untuk edisi " +
                        "tes tersebut serta validasi penyajian layar.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = viewModel::startOrShowUnavailable) {
                    Text("Ulangi / periksa ketersediaan materi")
                }
            }
        }
    }
}
