# EyeCare Pro

Aplikasi Android untuk edukasi dan skrining awal penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Fitur yang tersedia pada build saat ini
- Pemeriksaan pencahayaan sekitar melalui \`Sensor.TYPE_LIGHT\`, dengan ambang operasional aplikasi 100–400 lux.
- Pengaturan kecerahan jendela aplikasi menjadi 80% saat pembacaan lux berada pada rentang operasional, dan dipulihkan saat aplikasi meninggalkan foreground.
- Eksperimen visual kedip CFF/FFF dengan pengaturan frekuensi, pembatas teoritis berdasarkan refresh rate layar, dan pencatatan persepsi subjektif. Ini bukan pengukuran CFF klinis.
- Estimasi jarak wajah melalui kamera depan dan ML Kit Face Detection. Pengguna harus mengukur jarak kalibrasi dengan alat ukur; hasil berikutnya adalah perkiraan relatif.
- Pengingat aturan 20-20-20 menggunakan WorkManager dan notifikasi Android.
- Ringkasan status edukatif untuk modul.

## Ishihara: sengaja dikunci sampai materi resmi tersedia
Repository belum memiliki pelat Ishihara asli yang lisensinya dan validitas edisinya telah diverifikasi. Karena itu modul tidak menampilkan gambar tes dan tidak menghasilkan indikasi buta warna.

Lapisan domain sekarang menyediakan model dataset, pemeriksaan prasyarat metadata, alur jawaban ViewModel, dan penghitung jawaban benar/salah/tidak dijawab. Default repository selalu mengembalikan \`null\`. Untuk mengaktifkan tes, pemilik proyek harus memperoleh izin distribusi digital, identitas edisi resmi, gambar asli yang tidak dimodifikasi, kunci jawaban, aturan interpretasi resmi, serta bukti validasi untuk penyajian pada layar ponsel. Flag verifikasi di kode adalah gerbang review maintainer, bukan bukti otomatis bahwa aset benar-benar berlisensi atau tervalidasi.

Skor mentah hanya berupa jumlah jawaban benar. Aplikasi tidak mengubah skor menjadi label normal/defisiensi karena ambang interpretasi yang sah belum tersedia. Unit test memakai data fixture buatan untuk menguji logika penghitung saja; fixture tersebut bukan pelat Ishihara dan tidak ditampilkan oleh aplikasi.

## CFF/FFF: batasan teknis dan keselamatan
Stimulus dijadwalkan pada frame Compose. Batas teoritis pergantian bidang hitam-putih adalah sekitar setengah refresh rate layar (misalnya 30 Hz pada layar 60 Hz), dengan batas aplikasi 60 Hz. Ini bukan jaminan frekuensi fisik aktual: frame yang terlewat, refresh rate adaptif, PWM, kecerahan, panel, mode hemat daya, dan beban sistem dapat mengubah stimulus. Hasil yang dicatat hanya persepsi pengguna pada frekuensi target, bukan ambang CFF yang terukur dan bukan diagnosis tunggal kelelahan mata.

Modul menampilkan peringatan kedipan dan meminta pengguna mengonfirmasi telah membacanya. Jangan jalankan stimulasi jika memiliki epilepsi atau riwayat kejang yang dipicu cahaya; hentikan bila timbul pusing, mual, sakit kepala, atau rasa tidak nyaman.

## Batasan dan keselamatan umum
- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Tes Ishihara hanya boleh memakai pelat asli yang sah, berlisensi, dan tervalidasi.
- Estimasi jarak wajah memerlukan kalibrasi manual. Perubahan zoom, orientasi, kamera, posisi, atau pencahayaan dapat memengaruhi hasil.
- Rentang 100–400 lux adalah ambang operasional aplikasi, bukan standar diagnosis.
- Pengingat berkala WorkManager dapat tertunda oleh kebijakan baterai Android.

## Target teknis
- Minimum Android: 7.0 (API 24)
- Bahasa: Kotlin
- UI: Jetpack Compose
- UI architecture: state-driven Compose; modul Ishihara menggunakan domain/data/presentation separation dan ViewModel; fitur kamera memakai CameraX + ML Kit on-device.
- Java/JVM: 17

## Versi build dan dependency
Versi plugin dan library dideklarasikan eksplisit di \`build.gradle.kts\`, \`app/build.gradle.kts\`, dan workflow CI. Build saat ini memakai Android Gradle Plugin 8.13.2, Kotlin 2.2.21, Gradle 8.13, JDK 17, dan compile/target SDK 36. Minimum SDK tetap 24.

## Build lokal
Gunakan JDK 17, Android SDK 36, dan Gradle 8.13. Dari direktori repository jalankan:
\`\`\`bash
gradle testDebugUnitTest
gradle assembleDebug
\`\`\`
APK debug dihasilkan pada \`app/build/outputs/apk/debug/app-debug.apk\`. GitHub Actions menjalankan unit test dan mengunggah APK sebagai artifact \`EyeCare-Pro-debug\`.

## Status validasi
CI sebelumnya berhasil membangun APK dasar dan menjalankan unit test. Perubahan terbaru harus lulus workflow CI sebelum dianggap terintegrasi. Fitur kamera, tampilan kedip, notifikasi, sensor, dan perilaku lintas-refresh-rate tetap memerlukan pengujian pada perangkat Android fisik. Kompatibilitas API 24+ dijaga pada konfigurasi, tetapi tidak dapat dianggap terverifikasi di seluruh perangkat hanya dari build CI.
