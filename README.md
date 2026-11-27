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


## Catatan validasi emulator CI

- Runner Ubuntu 24.04 membawa Android command-line tools 12.0, yang salah mem-parsing package `android-37.0` dan menulis `target=android-0`. CI kini memasang serta mematok command-line tools 22.0 sebelum membuat AVD; metadata harus tepat `target=android-37.0` dan `image.sysdir.1` harus cocok dengan `system-images/android-37.0/google_apis_ps16k/x86_64/`. Placeholder `android-0` selalu ditolak.
- Instalasi APK pada API 24 memakai jalur ADB non-streaming terlebih dahulu, lalu mencoba streaming sebagai fallback. Setiap percobaan dibatasi 180 detik agar runner tidak menggantung sampai batas job.
- Regresi metadata AVD diuji oleh `.github/scripts/test-avd-metadata.sh` sebelum matrix emulator berjalan.


## Validasi CI dan perbaikan host Compose test — 25 November 2026

Run [#202](https://github.com/chuankie10/EyeCare-Pro/actions/runs/45343617775) menyelesaikan:
- Unit test, build debug APK, build instrumentation APK, dan pemeriksaan alignment native awal: **lulus**.
- Android API 24: **3/3 Compose UI tests lulus**.
- Android API 35: **3/3 Compose UI tests lulus**.
- Android API 37: gagal pada setup Activity untuk satu UI test. Log menunjukkan ActivityScenario tidak dapat menemukan androidx.activity.ComponentActivity di instrumentation APK (Unable to resolve activity ... com.eyecare.pro.test/androidx.activity.ComponentActivity). Ini bukan bukti crash aplikasi atau kegagalan assertion CFF.

Perbaikan pada 25 November:
- Percobaan menambahkan `ComposeTestActivity` hanya pada APK `androidTest` ternyata keliru: Activity berada di proses/package instrumentation, sedangkan `ActivityScenario` dijalankan dari proses aplikasi target. Run #209 menunjukkan `Intent in process com.eyecare.pro resolved to different process com.eyecare.pro.test`. Perbaikan berikutnya memakai `MainActivity` yang memang terdaftar pada APK aplikasi sebagai host untuk seluruh Compose UI tests.
- Run #209 juga menunjukkan API 37 memulai AVD dengan `target=android-0`; logcat merekam `SurfaceFlinger` SIGABRT di `RegionSampling` dengan assertion `!rcEnc->featureInfo()->hasReadColorBufferDma`. Ini konsisten dengan masalah parsing AVD oleh command-line tools 12.0, bukan assertion fitur aplikasi.

Perbaikan lanjutan: pin command-line tools 22.0, menolak `android-0`, memverifikasi `getconf PAGE_SIZE == 16384` pada image API 37 16 KB, dan memakai Activity target-app untuk test host. Validasi ulang belum selesai; API 37 dan pengujian perangkat fisik/16 KB tetap belum boleh dinyatakan lulus.


## Follow-up CI — 26 November 2026

Run [#209](https://github.com/chuankie10/EyeCare-Pro/actions/runs/45563506528) membuktikan job build, unit test, build APK aplikasi/instrumentasi, dan pre-check alignment 16 KB lulus. Instrumentasi belum lulus:
- API 24 dan API 35: dua UI tests lulus; tes CFF dengan `ComposeTestActivity` gagal karena Activity di APK instrumentation berjalan pada proses berbeda dari aplikasi target.
- API 37: AndroidJUnitRunner terputus setelah SurfaceFlinger mengalami SIGABRT di thread RegionSampling. Log menunjukkan assertion `!rcEnc->featureInfo()->hasReadColorBufferDma`; AVD dibuat dengan `target=android-0` oleh command-line tools 12.0.
- Perbaikan sekarang menghapus Activity khusus test, menggunakan `MainActivity` sebagai host, mematok command-line tools 22.0, menolak target `android-0`, dan memverifikasi page size 16 KB sebelum test berjalan.

Command-line tools 22.0 dipilih karena issue runner-images mendokumentasikan bahwa versi 20.0/22.0 menulis target `android-37.0`, sedangkan versi 12.0 menulis `android-0` untuk paket minor-version. Lihat [runner-images #14484](https://github.com/actions/runner-images/issues/14484). Pengujian runtime API 37 harus tetap dianggap pending sampai run baru menyelesaikan seluruh UI tests.

## Investigasi CI — run #223 dan perbaikan Compose host — 27 November 2026

Run [#223](https://github.com/chuankie10/EyeCare-Pro/actions/runs/45770805396) membuktikan:
- Unit test, `assembleDebug`, `assembleDebugAndroidTest`, upload artifact, dan pemeriksaan alignment 16 KB awal: **lulus**.
- Ketiga emulator API 24, 35, dan 37 berhasil boot, memasang APK, serta menjalankan AndroidJUnitRunner.
- Dua UI test lulus pada setiap API. Satu UI test gagal pada ketiganya dengan exception yang sama: `MainActivity has already set content`, saat `CffUnavailableRefreshRateUiTest` memanggil `setContent` melalui `createAndroidComposeRule<MainActivity>()`. Ini kegagalan setup test, bukan kegagalan runtime CFF atau crash SurfaceFlinger.
- API 37 melewati boot dan instrumentasi; log run #223 tidak menunjukkan kegagalan karena target AVD `android-0`. Image yang dipakai adalah `google_apis_ps16k`, dan log job secara eksplisit mengonfirmasi `Verified Android guest page size: 16384 bytes`. Kegagalan run #223 tetap berasal dari test host Compose yang memanggil `setContent` pada `MainActivity` yang sudah memiliki content.

Perbaikan pada commit `b900bb6`:
- Test navigasi/konfirmasi yang benar-benar menguji aplikasi tetap memakai `createAndroidComposeRule<MainActivity>()`.
- Test terisolasi yang menyuntikkan `displayRefreshRateHz = 0f` kini memakai `createComposeRule()`, sehingga Compose test host dapat menerima `setContent` tanpa menimpa content milik `MainActivity`.
- Tidak ada perubahan kode produksi, dependency Gradle, target SDK, atau materi tes medis.

Run CI setelah commit ini harus mengonfirmasi ketiga API menghasilkan `OK (3 tests)`. Sampai run tersebut selesai, hasil instrumentasi API 24/35/37 masih berstatus belum lulus. Pemeriksaan alignment APK bukan pengganti uji runtime pada perangkat 16 KB; pengujian perangkat fisik tetap wajib.

## Validasi CI — perbaikan host Compose terkonfirmasi — 27 November 2026

Run [#227](https://github.com/chuankie10/EyeCare-Pro/actions/runs/45954620658), commit `6df68b1`, selesai **success**:
- Unit test Kotlin: lulus.
- `assembleDebug` dan `assembleDebugAndroidTest`: lulus.
- Pemeriksaan alignment native 16 KB pada APK: lulus.
- Android API 24: `OK (3 tests)`, `INSTRUMENTATION_CODE: -1`.
- Android API 35: `OK (3 tests)`, `INSTRUMENTATION_CODE: -1`.
- Android API 37 / image `google_apis_ps16k`: guest page size terverifikasi `16384` byte dan `OK (3 tests)`, `INSTRUMENTATION_CODE: -1`.

Perbaikan yang diuji adalah memisahkan host UI test: navigasi aplikasi tetap memakai `createAndroidComposeRule<MainActivity>()`, sedangkan tes deterministik yang menyuntikkan refresh rate `0f` memakai `createComposeRule()` agar tidak memanggil `setContent` kedua kali pada `MainActivity`. Tidak ada perubahan kode produksi pada commit ini. Keberhasilan emulator bukan bukti uji perangkat fisik; API 37 diuji dengan target SDK aplikasi 36, bukan target 37.

## Tinjauan sumber materi Ishihara — 27 November 2026

- Wellcome Collection menyediakan salinan arsip *Tests for colour-blindness* (Shinobu Ishihara, edisi revisi lengkap ke-12, 1957) dengan lisensi CC BY-NC 4.0. Lisensi tersebut mensyaratkan atribusi dan membatasi penggunaan yang terutama ditujukan untuk keuntungan komersial: https://wellcomecollection.org/works/xkf4mb2s.
- Artikel open-access 2023 membahas prosedur cetak khusus pada pelat Ishihara resmi dan perubahan warna akibat penuaan: https://doi.org/10.1002/col.22877. Pemindaian arsip yang legal tidak otomatis memvalidasi reproduksi pada layar ponsel.
- Lisensi PWA Fonlow memberi izin atas software, tetapi secara eksplisit mengecualikan materi pihak ketiga dari pemberian lisensinya; karena itu lisensi PWA saja tidak membuktikan semua gambar pelat dapat didistribusikan ulang.

Keputusan: tidak ada gambar arsip atau PWA pihak ketiga yang disalin ke APK. Dataset produksi Ishihara tetap terkunci sampai hak distribusi untuk setiap aset, edisi dan kunci jawaban yang cocok, serta validasi penyajian layar yang relevan ditinjau. Tidak ada pola sintetis yang dipakai sebagai pengganti.
