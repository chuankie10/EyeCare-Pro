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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eyecare.pro.domain.ishihara.IshiharaScoringEngine
import com.eyecare.pro.presentation.ishihara.IshiharaUiState
import com.eyecare.pro.presentation.ishihara.IshiharaViewModel

@Composable
fun IshiharaScreen(viewModel: IshiharaViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tes Ishihara", style = MaterialTheme.typography.headlineSmall)

        when (val current = state) {
            is IshiharaUiState.Unavailable -> {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tes belum tersedia", style = MaterialTheme.typography.titleLarge)
                        Text(current.message)
                    }
                }
                Text(
                    "Aplikasi tidak membuat pola titik atau angka acak sebagai pengganti pelat Ishihara. " +
                        "Modul baru dapat diaktifkan setelah materi asli diperoleh secara sah dan " +
                        "identitas edisi, kunci jawaban, izin distribusi digital, serta bukti " +
                        "validasi penyajian pada layar ponsel diperiksa."
                )
                Text(
                    "Tanpa materi dan aturan interpretasi edisi yang sesuai, aplikasi tidak " +
                        "memberikan indikasi normal atau gangguan penglihatan warna. Untuk " +
                        "pemeriksaan, konsultasikan dengan dokter mata atau optometris.",
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
                    "Pelat ${current.currentQuestion} dari ${current.totalQuestions}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Sumber materi: ${current.sourceTitle}")

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
                        contentDescription = "Pelat tes warna dari edisi yang telah ditinjau",
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
                    OutlinedButton(
                        onClick = viewModel::skipCurrentQuestion,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lewati pertanyaan")
                    }
                    Text(
                        "Jangan gunakan filter warna, mode kenyamanan mata, atau pembesaran " +
                            "gambar. Kecerahan, kalibrasi warna, ukuran, dan karakteristik layar " +
                            "tetap dapat memengaruhi respons.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is IshiharaUiState.Completed -> {
                Text("Sesi selesai", style = MaterialTheme.typography.titleLarge)
                Text("Sumber materi: ${current.sourceTitle}")
                Text("Jawaban benar: ${current.score.correctAnswers}")
                Text("Jawaban salah: ${current.score.incorrectAnswers}")
                Text("Jawaban ragu-ragu: ${current.score.unsureAnswers}")
                Text("Tidak dijawab: ${current.score.unansweredQuestions}")
                Text("Total pelat: ${current.score.totalQuestions}")
                Text(
                    "Indikasi klinis: tidak dapat ditentukan oleh aplikasi ini. Rekap jawaban " +
                        "bukan diagnosis dan tidak boleh diterjemahkan menjadi status normal " +
                        "atau gangguan warna tanpa aturan interpretasi resmi untuk edisi tes " +
                        "tersebut serta validasi penyajian layar.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = viewModel::startOrShowUnavailable) {
                    Text("Ulangi pemeriksaan")
                }
            }
        }
    }
}
