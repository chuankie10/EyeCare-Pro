package com.eyecare.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IshiharaScreen() {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tes warna Ishihara", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Modul ini disiapkan, tetapi belum diaktifkan karena repository belum memiliki pelat Ishihara asli yang berlisensi dan tervalidasi.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "EyeCare Pro tidak akan menampilkan lingkaran atau angka acak sebagai pengganti pelat tes. Gambar tiruan dapat menghasilkan kesimpulan yang menyesatkan.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Agar tes dapat diaktifkan, aset resmi harus diperoleh dengan izin pemegang hak, ditampilkan tanpa perubahan warna atau skala yang merusak validitas, dan diuji pada kondisi layar yang sesuai.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Jika Anda memiliki paket pelat resmi beserta izin penggunaan, aset dapat diintegrasikan pada tahap berikutnya. Hasil tes tetap hanya skrining awal dan bukan diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
