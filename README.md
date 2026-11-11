# EyeCare Pro

Aplikasi Android untuk edukasi dan skrining awal penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Fitur yang tersedia pada build saat ini
- Pemeriksaan pencahayaan sekitar melalui `Sensor.TYPE_LIGHT`, dengan panduan rentang terpisah untuk pekerjaan dominan layar (300–500 lux) dan membaca/menulis (500–750 lux). Angka tersebut adalah rentang panduan edukatif, bukan ambang medis.
- Saran tindakan ketika cahaya di bawah/di atas rentang aktivitas; pembacaan sensor ponsel hanya proksi dan bukan pengukuran terkalibrasi pada bidang kerja.
- Eksperimen visual kedip dengan panduan mulai 5 Hz, kenaikan bertahap, pencatatan apakah kedipan masih terlihat/menyatu/tidak pasti, dan peringatan keselamatan. Ini bukan pengukuran CFF klinis.
- Estimasi jarak wajah melalui kamera depan dan ML Kit Face Detection. Referensi kalibrasi dikunci sampai pengguna menghapus atau memperbaruinya; hasil berikutnya adalah perkiraan relatif.
- Pengingat aturan 20-20-20 menggunakan WorkManager dan notifikasi Android.
- Simulasi persepsi warna dengan lima pelat titik berwarna orisinal, input jawaban, dan ringkasan respons sesi; simulasi ini tidak menilai normal/abnormal dan bukan tes Ishihara resmi.\n- Ringkasan status edukatif untuk modul.

## Modul yang menunggu aset resmi
Tes Ishihara belum diaktifkan karena belum ada pelat asli yang berlisensi dan tervalidasi di repository. Aplikasi sengaja tidak membuat pelat acak atau tiruan.

## Batasan dan keselamatan
- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Simulasi persepsi warna di aplikasi bukan tes Ishihara resmi dan tidak memiliki validitas klinis. Pemeriksaan diagnostik harus dilakukan dengan metode tervalidasi oleh tenaga kesehatan.
- CFF melalui layar ponsel bersifat eksperimental. Refresh rate, frame pacing, PWM, kecerahan, dan karakteristik panel membatasi akurasi frekuensi kedipan fisik.
- Estimasi jarak wajah memerlukan kalibrasi manual. Perubahan zoom, orientasi, kamera, posisi, atau pencahayaan dapat memengaruhi hasil.
- Rentang pencahayaan pada aplikasi adalah panduan edukatif berdasarkan aktivitas, bukan standar medis, pengukuran klinis, atau batas diagnosis.
- Pengingat berkala WorkManager dapat tertunda oleh kebijakan baterai Android.

## Target teknis
- Minimum Android: 7.0 (API 24)
- Bahasa: Kotlin
- UI: Jetpack Compose
- UI architecture: state-driven Compose; fitur kamera memakai CameraX + ML Kit on-device.
- Java/JVM: 17

## Build lokal
Gunakan JDK 17, Android SDK 36, dan Gradle 8.13. Dari direktori repository jalankan:
```bash
gradle testDebugUnitTest
gradle assembleDebug
```
APK debug dihasilkan pada `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions menjalankan unit test dan mengunggah APK sebagai artifact `EyeCare-Pro-debug`.

## Status validasi
CI sebelumnya berhasil membangun APK dasar dan menjalankan unit test. Revisi 0.2.0 dan simulasi warna menunggu hasil CI terbaru. Fitur kamera, tampilan, notifikasi, sensor, dan animasi tetap memerlukan pengujian pada perangkat Android fisik sebelum dianggap siap rilis.
