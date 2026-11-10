# EyeCare Pro

Aplikasi Android untuk edukasi dan skrining awal penglihatan. **Aplikasi ini bukan alat diagnosis medis.**

## Prinsip keselamatan dan validitas
- Hasil tidak menggantikan pemeriksaan dokter mata atau optometris.
- Tes Ishihara hanya boleh memakai pelat asli yang sah, berlisensi, dan tervalidasi. Repository ini tidak membuat atau menyertakan pelat tiruan.
- Tes CFF melalui layar ponsel bersifat eksperimental. Refresh rate, frame pacing, PWM, kecerahan, dan karakteristik panel membatasi akurasi frekuensi kedipan fisik.
- Estimasi jarak wajah membutuhkan kalibrasi perangkat dan bukan pengukuran klinis.
- Rentang 100–400 lux adalah ambang operasional aplikasi, bukan standar diagnosis.

## Target teknis
- Minimum Android: 7.0 (API 24)
- Bahasa: Kotlin
- UI: Jetpack Compose
- Arsitektur target: Clean Architecture + MVVM

## Status implementasi
- [x] Kerangka aplikasi Android dan pipeline CI.
- [x] Pemeriksaan cahaya sekitar melalui `Sensor.TYPE_LIGHT`.
- [ ] Tes Ishihara dengan aset tervalidasi dan berlisensi.
- [ ] Eksperimen CFF dengan batasan perangkat yang dijelaskan.
- [ ] Estimasi jarak wajah yang memerlukan kalibrasi.
- [ ] Dashboard hasil dan pengingat 20-20-20.
- [ ] Build CI dan uji perangkat nyata.

## Build lokal
Gunakan JDK 17, Android SDK 36, dan Gradle 8.13. Dari direktori repository jalankan:
```bash
gradle testDebugUnitTest
gradle assembleDebug
```
APK debug dihasilkan pada `app/build/outputs/apk/debug/app-debug.apk`.

## Peringatan
Fitur skrining bersifat edukatif dan tidak boleh dipakai untuk menetapkan diagnosis atau menggantikan pemeriksaan profesional.
