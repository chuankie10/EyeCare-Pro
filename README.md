# EyeCare Pro

Aplikasi Android untuk edukasi dan skrining awal penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Fitur yang tersedia pada build saat ini
- Pemeriksaan pencahayaan sekitar melalui `Sensor.TYPE_LIGHT`, dengan ambang operasional aplikasi 100–400 lux.
- Pengaturan kecerahan jendela aplikasi menjadi 80% saat pembacaan lux berada pada rentang operasional, dan dipulihkan saat aplikasi meninggalkan foreground.
- Eksperimen visual kedip dengan kontrol frekuensi dan catatan pengamatan mandiri. Ini bukan pengukuran CFF klinis.
- Estimasi jarak wajah melalui kamera depan dan ML Kit Face Detection. Pengguna harus mengukur jarak kalibrasi dengan alat ukur; hasil berikutnya adalah perkiraan relatif.
- Pengingat aturan 20-20-20 menggunakan WorkManager dan notifikasi Android.
- Ringkasan status edukatif untuk modul.

## Modul yang menunggu aset resmi
Tes Ishihara belum diaktifkan karena belum ada pelat asli yang berlisensi dan tervalidasi di repository. Aplikasi sengaja tidak membuat pelat acak atau tiruan.

## Batasan dan keselamatan
- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Tes Ishihara hanya boleh memakai pelat asli yang sah, berlisensi, dan tervalidasi.
- CFF melalui layar ponsel bersifat eksperimental. Refresh rate, frame pacing, PWM, kecerahan, dan karakteristik panel membatasi akurasi frekuensi kedipan fisik.
- Estimasi jarak wajah memerlukan kalibrasi manual. Perubahan zoom, orientasi, kamera, posisi, atau pencahayaan dapat memengaruhi hasil.
- Rentang 100–400 lux adalah ambang operasional aplikasi, bukan standar diagnosis.
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
CI berhasil membangun APK dasar dan menjalankan unit test. Fitur kamera, tampilan, notifikasi, sensor, dan animasi tetap memerlukan pengujian pada perangkat Android fisik sebelum dianggap siap rilis.
