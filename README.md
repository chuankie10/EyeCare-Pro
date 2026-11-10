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
- Arsitektur: Clean Architecture + MVVM

## Fitur yang direncanakan
1. Pemeriksaan pencahayaan sekitar menggunakan sensor cahaya.
2. Modul tes Ishihara yang hanya aktif jika aset pelat tervalidasi tersedia.
3. Eksperimen Critical Flicker Fusion (CFF) dengan batasan perangkat dijelaskan kepada pengguna.
4. Estimasi jarak wajah berbasis deteksi wajah dengan penjelasan kebutuhan kalibrasi.
5. Dashboard hasil edukatif dan pengingat aturan 20-20-20.

## Build
Gunakan Android Studio dan JDK 17. Jalankan:
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Status
Dokumentasi awal repositori. Implementasi fitur, pengujian, dan validasi perangkat masih perlu dilakukan sebelum rilis.
