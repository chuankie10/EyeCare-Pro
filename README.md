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
- Jika perangkat tidak melaporkan refresh rate yang valid, stimulasi dinonaktifkan; aplikasi tidak mengasumsikan 60 Hz.
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
- Kotlin: 2.4.21 melalui dukungan Kotlin bawaan AGP 9
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

AGP 9.3 mendukung compile API 37 dan memerlukan Gradle 9.5.0 serta JDK 17. Kotlin 2.4.21 didokumentasikan kompatibel penuh dengan Gradle sampai 9.7.0 dan AGP sampai 9.3.1. AGP 9 menggunakan built-in Kotlin, sehingga plugin `org.jetbrains.kotlin.android` dihapus. Compose memakai plugin compiler yang versinya sama dengan Kotlin; marker plugin dipatok sebagai classpath dependency melalui Maven Central dan diterapkan pada modul app agar resolusinya tidak bergantung pada marker lookup Gradle Plugin Portal. Versi dependency dipatok untuk build yang dapat direproduksi, bukan klaim bahwa semuanya merupakan rilis terbaru.

## Build dan pengujian

Gunakan JDK 17, Android SDK Platform 37.0, dan Build Tools 36.0.0. Gradle 9.5.0 disediakan melalui Gradle Wrapper yang tersimpan di repository (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, dan `gradle/wrapper/gradle-wrapper.properties`). Dari direktori repository:

```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

`connectedDebugAndroidTest` memerlukan emulator atau perangkat Android yang terhubung. CI memisahkan build/unit test dari instrumentasi agar APK tetap dapat dibuat ketika emulator hosted runner bermasalah.

### Emulator pada CI

CI memakai AVD x86_64 dengan akselerasi KVM pada runner Linux. Workflow memuat modul KVM generik dan modul vendor Intel/AMD, mengatur izin `/dev/kvm`, lalu memverifikasi `emulator -accel-check`. Jika runner tidak mengekspos KVM, job berhenti dengan diagnostik yang jelas; CI tidak diam-diam beralih ke TCG yang sebelumnya gagal boot secara tidak konsisten.

Matrix emulator:
- API 24: AOSP `default` x86_64.
- API 35: Google APIs x86_64.
- API 37.0: Google APIs 16 KB page-size x86_64.

Script `.github/scripts/run-emulator-tests.sh` membuat AVD dan mengatur `disk.dataPartition.size=4096MB` sebelum cold boot. Script menunggu ADB `device`, `sys.boot_completed=1`, PackageManager, dan ActivityManager (`am get-current-user`) siap. APK aplikasi dan APK instrumentasi sekarang dibangun sekali pada job build, diunggah sebagai artifact, lalu dipasang langsung melalui ADB pada setiap emulator. Matrix tidak lagi menjalankan Gradle atau mengunduh ulang dependency Kotlin/AGP pada tiga runner terpisah. Hasil AndroidJUnitRunner disimpan ke `test-results/instrumentation-api-<API>.txt`.

Run #150 (commit `37f2d74`) membuktikan:
- Gradle Wrapper, unit test, dan build APK: lulus; artifact debug APK berhasil diunggah.
- API 35 UI test: satu test gagal karena test memanggil `setContent` pada `MainActivity` yang sudah mengisi Compose content. Test tersebut dipindahkan ke `createComposeRule()` dengan Activity kosong.
- API 24 dan API 37: ActivityManager belum siap saat pemasangan APK tes.
- Run #154 membuktikan pemeriksaan ActivityManager kini berhasil dan partisi data emulator memiliki ruang kosong memadai (API 24: sekitar 5,8 GB tersedia). Namun, job API 24 gagal sebelum instrumentasi karena runner terpisah tidak dapat mengambil beberapa dependency buildscript Kotlin/ASM dari repository Maven. Build job utama tetap lulus.
- Workflow baru menghilangkan resolver Gradle dari job emulator dengan memakai APK yang sudah dibangun di job utama. UI test belum boleh dinyatakan lulus sampai run setelah perubahan ini selesai.

Run #148 (commit `6d846a2`) membuktikan:
- Gradle Wrapper, unit test, dan build APK: lulus.
- API 35 UI test: lulus.
- API 24 dan API 37 UI test: gagal saat pemasangan APK karena ruang internal emulator tidak cukup / instalasi split APK tidak selesai. KVM sudah berhasil diaktifkan pada ketiga runner.
- Perbaikan ukuran data partition menjadi 4 GB sedang divalidasi pada run berikutnya. Sampai hasil itu selesai, kompatibilitas emulator API 24 dan API 37 belum boleh dinyatakan lulus.


APK debug dihasilkan di:

```
app/build/outputs/apk/debug/app-debug.apk
```

Gradle Wrapper mengunduh distribusi Gradle 9.5.0 resmi pada penggunaan pertama dan memverifikasi checksum. Di Windows gunakan `gradlew.bat` sebagai pengganti `./gradlew`. JDK 17 dan Android SDK Platform 37.0 tetap harus tersedia.

## Daftar uji perangkat

1. Android 7.0/API 24: instalasi, pembukaan aplikasi, navigasi, dan sensor yang tersedia.
2. Android 12/API 31 atau lebih baru: notifikasi, izin kamera, dan perilaku foreground/background.
3. Android 17/API 37: instalasi, navigasi, rotasi, lifecycle, dan kompatibilitas runtime; target SDK tetap 36 pada build ini.
4. Layar 60/90/120 Hz: refresh rate yang dilaporkan, batas teoritis CFF, mulai/henti, timeout 10 detik, dan pencatatan respons.
5. Refresh rate tidak tersedia/invalid: tombol mulai CFF harus tetap nonaktif.
6. Refresh rate adaptif dan mode hemat daya: pastikan sesi dihentikan bila batas baru tidak mendukung frekuensi yang sedang berjalan.
7. Tekan Home, buka aplikasi lain, atau kunci layar saat CFF berjalan: pastikan stimulasi berhenti pada `ON_PAUSE` dan tidak berjalan kembali setelah resume.
8. CFF: pastikan tombol mulai terkunci sampai peringatan dibaca dan konfirmasi tidak memiliki riwayat fotosensitivitas diberikan.
9. Ishihara: layar menyatakan materi belum tersedia dan tidak menampilkan pelat sintetis.
10. Kamera/sensor/notifikasi: izin ditolak, perangkat tanpa sensor cahaya, kamera tidak tersedia, dan aplikasi kembali dari background.
11. Aksesibilitas: ukuran teks besar, kontras, TalkBack, dan orientasi layar.

Pengujian pada perangkat fisik tetap wajib sebelum rilis.


## Catatan validasi lanjutan — 22 November 2026

Run #160 pada commit `f6c6b03` mengonfirmasi bahwa build, unit test, dan pembuatan kedua APK berhasil. Hasil instrumentasi yang dapat dibuktikan dari log:

- Android API 35: **3/3 UI test lulus** (`OK (3 tests)`).
- Android API 24: **3/3 UI test melaporkan lulus** (`OK (3 tests)` dan `INSTRUMENTATION_CODE: -1`), tetapi proses klien ADB tidak keluar setelah ringkasan akhir. Job akhirnya mencapai batas waktu 45 menit dan dibatalkan oleh runner; ini bukan kegagalan assertion, tetapi job belum dapat dinyatakan lulus.
- Android API 37 / image 16 KB: emulator boot dan ActivityManager siap, tetapi PackageManager mengembalikan `Broken pipe (32)` saat query paket dan pemasangan APK. Belum ada hasil UI test API 37.

Perbaikan berikutnya pada commit `98fa58e`:
- Menunggu PackageManager lulus tiga query berturut-turut setelah ActivityManager siap.
- Menghapus query daftar paket yang hanya dipakai untuk mencetak contoh dan dapat menutupi kegagalan service.
- Mencoba ulang pemasangan sekali dengan `adb install --no-streaming` jika streaming install gagal.
- Mengumpulkan status service, ruang data, logcat terkait PackageManager/system_server, dan log proses emulator saat gagal.
- Membatasi proses `adb shell am instrument` menjadi 120 detik. Jika AndroidJUnitRunner telah mengirim `OK (N tests)` dan `INSTRUMENTATION_CODE: -1` tanpa `FAILURES!!!`, tetapi klien ADB tidak menutup, hasil assertion tetap dicatat lulus dengan peringatan. Tanpa ringkasan akhir tersebut, job tetap gagal.

Perbaikan ini **belum dianggap tervalidasi** sampai run setelah commit `98fa58e` selesai. Kompatibilitas API 24/API 37 tetap belum dinyatakan lulus penuh. Pengujian perangkat fisik juga belum dilakukan.


## Validasi CI dan 16 KB — 22 November 2026

Run [#164](https://github.com/chuankie10/EyeCare-Pro/actions/runs/44992269509) selesai dengan hasil:
- Unit test, build debug APK, dan build instrumentation APK: **lulus**.
- Android API 24: **3/3 Compose UI tests lulus**. Pembatas 120 detik pada ADB menghindari job menggantung setelah ringkasan hasil.
- Android API 35: **3/3 Compose UI tests lulus**.
- Android API 37 pada image `google_apis_ps16k`: gagal sebelum APK dipasang. Log menunjukkan `surfaceflinger` abort (`SIGABRT`) di thread `RegionSampling`, lalu `system_server` mati. Ini kegagalan image emulator, bukan hasil uji aplikasi.

Perubahan setelah run #164:
- `.github/workflows/android-ci.yml` memakai image standar `google_apis` untuk pengujian OS Android API 37 agar uji antarmuka Android terbaru tidak bergantung pada image preview 16 KB yang crash.
- `.github/scripts/check-16k-alignment.sh` menjalankan `zipalign -c -P 16 4` dan memeriksa ELF LOAD alignment pada pustaka `arm64-v8a` serta `x86_64`.
- Pemeriksaan manual APK run #164 menemukan seluruh 64-bit ELF LOAD segments ber-alignment `0x4000` dan data offset pustaka native di dalam APK berada pada batas 16 KB. Ini pemeriksaan awal; skrip CI dan uji runtime 16 KB tetap diperlukan.

Jangan menafsirkan pengujian API 37 standar sebagai bukti uji runtime pada kernel 16 KB. Pengujian perangkat fisik 16 KB dan perangkat fisik Android 7.0/versi baru tetap belum dilakukan.
