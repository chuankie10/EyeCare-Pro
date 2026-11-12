# EyeCare Pro

Aplikasi Android untuk edukasi dan pengamatan awal terkait kenyamanan penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Fitur pada build 0.3.0

- Pemeriksaan pencahayaan sekitar melalui `Sensor.TYPE_LIGHT`, dengan rentang panduan terpisah untuk pekerjaan dominan layar (300–500 lux) dan membaca/menulis (500–750 lux). Angka tersebut merupakan panduan edukatif, bukan ambang medis.
- Eksperimen CFF/FFF berbasis frame Compose, dengan target yang dibatasi secara teoritis oleh refresh rate yang dilaporkan perangkat. Pengguna mencatat apakah stimulus masih tampak berkedip, tampak menyatu, atau tidak pasti.
- Estimasi jarak wajah melalui kamera depan dan ML Kit Face Detection. Hasil merupakan perkiraan relatif yang memerlukan kalibrasi manual.
- Pengingat aturan 20-20-20 menggunakan WorkManager dan notifikasi Android.
- Modul Ishihara dengan model domain, gerbang lisensi/validasi, alur jawaban, dan rekap mentah. **Modul tetap terkunci** karena repository belum berisi pelat asli yang berlisensi, identitas edisi, kunci jawaban, dan bukti validasi penyajian layar.

## Ishihara: kebijakan materi

Tidak ada pelat pseudo-Ishihara, pola acak, atau angka sintetis yang dipakai sebagai tes medis. Materi hanya boleh ditambahkan setelah pemilik proyek memastikan hak distribusi digital, sumber dan edisi resmi, kunci jawaban yang cocok dengan edisi, serta bukti validasi untuk penyajian pada layar ponsel. Unit test menggunakan fixture teks internal saja; fixture tersebut bukan materi pemeriksaan.

Aplikasi hanya menghitung jawaban benar, salah, ragu-ragu, dan tidak dijawab. Aplikasi tidak mengubah skor menjadi kategori normal/abnormal atau diagnosis.

## CFF/FFF: batasan dan keselamatan

- Target frekuensi adalah jadwal perangkat lunak, bukan pengukuran frekuensi cahaya fisik.
- Satu siklus hitam-putih membutuhkan setidaknya dua refresh; batas teoritis alternasi adalah sekitar setengah refresh rate. Contoh: 60 Hz → 30 Hz; 90 Hz → 45 Hz; 120 Hz → 60 Hz.
- Frame terlewat, refresh rate adaptif, frame pacing Android, PWM, kecerahan, panel, mode hemat daya, dan beban sistem dapat menurunkan atau mengubah stimulus aktual.
- Respons pengguna bersifat subjektif dan tidak boleh digunakan sendirian untuk menyimpulkan kelelahan mata atau kondisi medis.
- Jangan menjalankan stimulasi berkedip bila memiliki epilepsi atau riwayat kejang yang dipicu cahaya. Hentikan jika timbul pusing, mual, sakit kepala, atau ketidaknyamanan.

## Batasan umum

- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Pembacaan sensor cahaya ponsel bukan pengukuran terkalibrasi pada bidang kerja.
- Estimasi jarak wajah dipengaruhi kamera, zoom, orientasi, posisi, dan pencahayaan.
- Pengingat berkala WorkManager dapat tertunda oleh kebijakan baterai Android.

## Target teknis dan versi

- Minimum Android: 7.0 (API 24)
- Compile/Target SDK: 36
- Bahasa/UI: Kotlin, Jetpack Compose
- Arsitektur fitur Ishihara: domain + repository interface + ViewModel + Compose UI
- JDK/JVM: 17
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.2.21
- Gradle: 8.13
- Compose BOM: 2025.10.01
- AndroidX Core KTX: 1.17.0
- Activity Compose: 1.11.0
- Lifecycle: 2.9.4
- CameraX: 1.5.0
- ML Kit Face Detection: 16.1.7
- WorkManager: 2.10.5

## Build dan unit test

Gunakan JDK 17, Android SDK 36, dan Gradle 8.13. Dari direktori repository:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

APK debug dihasilkan di:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Daftar uji perangkat

1. Android 7.0/API 24: instalasi, pembukaan aplikasi, navigasi, sensor yang tersedia.
2. Android 12/API 31 atau lebih baru: notifikasi dan izin kamera.
3. Android terbaru yang tersedia: instalasi, rotasi, lifecycle, dan navigasi.
4. Layar 60/90/120 Hz: refresh rate yang dilaporkan, batas teoritis CFF, mulai/henti, dan pencatatan respons.
5. Refresh rate adaptif dan mode hemat daya: pastikan hasil tetap diberi label subjektif.
6. Ishihara: pastikan layar menyatakan materi belum tersedia dan tidak menampilkan pelat sintetis.
7. Kamera/sensor/notifikasi: izin ditolak, perangkat tanpa sensor cahaya, kamera tidak tersedia, dan aplikasi kembali dari background.
8. Aksesibilitas: ukuran teks besar, kontras, TalkBack, dan orientasi layar.

CI menjalankan unit test dan build APK. Pengujian pada perangkat fisik tetap wajib sebelum rilis.
