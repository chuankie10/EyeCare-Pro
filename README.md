# EyeCare Pro

Aplikasi Android untuk edukasi dan pengamatan awal terkait kenyamanan penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Fitur pada build 0.6.0

- Pemeriksaan pencahayaan sekitar melalui `Sensor.TYPE_LIGHT`; sensor hanya menampilkan pembacaan dan tidak mengubah kecerahan layar atau pengaturan perangkat.
- Eksperimen CFF/FFF berbasis frame Compose. Target frekuensi dibatasi secara teoritis oleh refresh rate yang dilaporkan perangkat; pengguna mencatat persepsi subjektif.
- CFF/FFF mewajibkan pengguna membaca peringatan dan mengonfirmasi tidak memiliki epilepsi/riwayat kejang akibat cahaya sebelum tombol mulai aktif. Jika memiliki riwayat atau ragu, jangan jalankan stimulasi.
- Satu percobaan CFF/FFF berhenti otomatis setelah maksimal 10 detik. Sesi dibatalkan pada `ON_PAUSE`/background dan tidak dilanjutkan otomatis.
- Model observasi CFF menolak frekuensi tidak valid atau target di atas batas teoritis berdasarkan refresh rate yang dilaporkan.
- Estimasi jarak wajah melalui kamera depan dan ML Kit Face Detection. Hasil merupakan perkiraan relatif yang memerlukan kalibrasi manual.
- Pengingat aturan 20-20-20 menggunakan WorkManager dan notifikasi Android.
- Modul Ishihara memiliki model domain, repository contract, ViewModel, alur jawaban, opsi ragu-ragu/lewati, dan rekap mentah benar/salah/ragu-ragu/tidak dijawab. **Modul produksi tetap terkunci** sampai pelat asli berlisensi, edisi, kunci jawaban, dan bukti validasi penyajian layar tersedia.

## Arsitektur Ishihara

- Domain mendefinisikan `IshiharaDatasetRepository`, model dataset/pelat, validasi metadata, dan mesin scoring.
- Data menyediakan `NoLicensedIshiharaDatasetRepository` sebagai implementasi aman yang mengembalikan `null` selama materi resmi belum disediakan.
- ViewModel hanya bergantung pada kontrak domain, bukan kelas konkret di lapisan data.
- Factory di paket `di` menghubungkan implementasi data ke ViewModel pada composition root `MainActivity`.
- UI Compose menampilkan status unavailable jika dataset tidak tersedia atau aset tidak dapat dimuat.

Tidak ada pelat pseudo-Ishihara, pola acak, atau angka sintetis yang dipakai sebagai tes medis. Materi hanya boleh ditambahkan setelah hak distribusi digital, sumber dan edisi resmi, kunci jawaban yang cocok, serta bukti validasi tampilan ponsel ditinjau. Unit test memakai fixture internal yang bukan materi pemeriksaan.

Aplikasi hanya menghitung respons mentah. Skor tidak diubah menjadi kategori normal/abnormal atau diagnosis. Pemeriksaan warna yang bermakna secara klinis harus mengikuti prosedur yang tervalidasi dan dilakukan dengan kondisi penyajian yang sesuai.

## CFF/FFF: batasan dan keselamatan

- Target frekuensi adalah jadwal perangkat lunak, bukan pengukuran frekuensi cahaya fisik.
- Satu siklus hitam-putih memerlukan setidaknya dua refresh; batas teoritis alternasi sekitar setengah refresh rate: 60 Hz → 30 Hz, 90 Hz → 45 Hz, 120 Hz → 60 Hz.
- Frame terlewat, refresh rate adaptif, frame pacing Android, PWM, kecerahan, panel, mode hemat daya, dan beban sistem dapat mengubah stimulus aktual.
- Sesi dibatasi maksimal 10 detik dan dibatalkan pada `ON_PAUSE` atau saat composable dilepas. Batas ini adalah pengamanan aplikasi, bukan protokol klinis tervalidasi.
- Respons bersifat subjektif dan tidak boleh digunakan sendirian untuk menyimpulkan kelelahan mata atau kondisi medis.
- Jangan menjalankan stimulasi berkedip bila memiliki epilepsi/riwayat kejang akibat cahaya atau tidak yakin dengan risikonya. Hentikan jika timbul pusing, mual, sakit kepala, atau ketidaknyamanan.

## Batasan umum

- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Pembacaan sensor cahaya ponsel bukan pengukuran terkalibrasi pada bidang kerja.
- Sensor cahaya tidak mengubah kecerahan layar secara otomatis.
- Estimasi jarak wajah dipengaruhi kamera, zoom, orientasi, posisi, dan pencahayaan.
- Pengingat berkala WorkManager dapat tertunda oleh kebijakan baterai Android.

## Toolchain dan target Android

- Minimum Android: 7.0 / API 24
- Compile SDK: 37
- Target SDK: 36 (dipertahankan sampai perilaku target API 37 diuji)
- JDK/JVM: 17
- Android Gradle Plugin: 9.3.1
- Gradle: 9.5.0
- Kotlin Gradle plugin: 2.4.21 melalui buildscript classpath untuk AGP built-in Kotlin
- Compose compiler plugin: 2.4.21
- Compose BOM: 2025.09.01 (Compose 1.9.2)
- AndroidX Core KTX: 1.17.0
- Activity Compose: 1.11.0
- Lifecycle: 2.9.4
- CameraX: 1.5.3
- ML Kit Face Detection: 16.1.7
- WorkManager: 2.10.5
- JUnit: 4.13.2
- AndroidX Test JUnit: 1.3.0
- Espresso: 3.7.0

AGP 9.3 mendukung compile API 37 dan memerlukan Gradle 9.5.0 serta JDK 17. Kotlin 2.4.21 didokumentasikan kompatibel penuh dengan Gradle sampai 9.7.0 dan AGP sampai 9.3.1. AGP 9 menggunakan built-in Kotlin, sehingga plugin `org.jetbrains.kotlin.android` dihapus; Compose tetap memakai plugin compiler yang versinya sama dengan Kotlin. Versi dependency dipatok untuk build yang dapat direproduksi, bukan klaim bahwa semuanya merupakan rilis terbaru.

## Build dan pengujian

Gunakan JDK 17, Android SDK Platform 37.0, Build Tools 36.0.0, dan Gradle 9.5.0. Dari direktori repository:

```bash
gradle testDebugUnitTest
gradle connectedDebugAndroidTest
gradle assembleDebug
```

`connectedDebugAndroidTest` memerlukan emulator atau perangkat Android yang terhubung. CI menjalankan Compose UI smoke test pada emulator API 35, selain unit test dan build APK.

APK debug dihasilkan di:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Daftar uji perangkat

1. Android 7.0/API 24: instalasi, pembukaan aplikasi, navigasi, dan sensor yang tersedia.
2. Android 12/API 31 atau lebih baru: notifikasi, izin kamera, dan perilaku foreground/background.
3. Android 17/API 37: instalasi, navigasi, rotasi, lifecycle, dan kompatibilitas runtime; target SDK tetap 36 pada build ini.
4. Layar 60/90/120 Hz: refresh rate yang dilaporkan, batas teoritis CFF, mulai/henti, timeout 10 detik, dan pencatatan respons.
5. Refresh rate adaptif dan mode hemat daya: pastikan sesi dihentikan bila batas baru tidak mendukung frekuensi yang sedang berjalan.
6. Tekan Home, buka aplikasi lain, atau kunci layar saat CFF berjalan: pastikan stimulasi berhenti pada `ON_PAUSE` dan tidak berjalan kembali setelah resume.
7. CFF: pastikan tombol mulai terkunci sampai peringatan dibaca dan konfirmasi tidak memiliki riwayat fotosensitivitas diberikan.
8. Ishihara: layar menyatakan materi belum tersedia dan tidak menampilkan pelat sintetis.
9. Kamera/sensor/notifikasi: izin ditolak, perangkat tanpa sensor cahaya, kamera tidak tersedia, dan aplikasi kembali dari background.
10. Aksesibilitas: ukuran teks besar, kontras, TalkBack, dan orientasi layar.

CI menjalankan unit test, UI smoke test emulator API 35, dan build APK. Pengujian pada perangkat fisik tetap wajib sebelum rilis.
