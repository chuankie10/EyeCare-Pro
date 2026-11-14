package com.eyecare.pro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyecare.pro.data.ishihara.NoLicensedIshiharaDatasetRepository
import com.eyecare.pro.di.IshiharaViewModelFactory
import com.eyecare.pro.domain.cff.CffObservation
import com.eyecare.pro.presentation.ishihara.IshiharaViewModel
import java.util.Locale

private enum class AppPage { HOME, ENVIRONMENT, ISHIHARA, CFF, FACE, REMINDER, RESULTS }

private data class LightPresentation(val title: String, val advice: String, val color: Color)

private fun LightCondition.presentation(lux: Float, task: LightTask): LightPresentation = when (this) {
    LightCondition.TOO_DARK -> LightPresentation("Di bawah panduan aktivitas", "Tambah pencahayaan umum atau lampu meja yang diarahkan ke bidang kerja. Buka tirai pada siang hari. Hindari lampu yang hanya menyinari layar sementara buku/meja tetap gelap.", Color(0xFFC62828))
    LightCondition.IDEAL -> LightPresentation("Dalam rentang panduan aktivitas", "Pembacaan sensor berada pada rentang panduan untuk " + task.label.lowercase(Locale.getDefault()) + ". Tetap perhatikan kenyamanan, bayangan, kontras, dan pantulan; angka lux saja tidak menjamin pencahayaan baik.", Color(0xFF2E7D32))
    LightCondition.TOO_BRIGHT -> LightPresentation("Di atas panduan aktivitas", "Kurangi lampu langsung, gunakan diffuser atau tirai, ubah posisi meja, dan hindari pantulan pada layar. Jangan menatap matahari atau sumber cahaya kuat.", Color(0xFFEF6C00))
}

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var displayManager: DisplayManager
    private var lightSensor: Sensor? = null
    private var currentLux by mutableStateOf<Float?>(null)
    private var sensorAvailable by mutableStateOf(true)
    private var displayRefreshRateHz by mutableStateOf(60f)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            @Suppress("DEPRECATION")
            val defaultDisplayId = windowManager.defaultDisplay.displayId
            if (displayId == defaultDisplayId) updateDisplayRefreshRate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorAvailable = lightSensor != null
        updateDisplayRefreshRate()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EyeCareApp(
                        lux = currentLux,
                        sensorAvailable = sensorAvailable,
                        displayRefreshRateHz = displayRefreshRateHz
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        updateDisplayRefreshRate()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        displayManager.unregisterDisplayListener(displayListener)
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun updateDisplayRefreshRate() {
        val defaultDisplay = windowManager.defaultDisplay
        val reportedRate = displayManager.getDisplay(defaultDisplay.displayId)?.refreshRate
            ?: defaultDisplay.refreshRate
        displayRefreshRateHz = reportedRate.takeIf { it.isFinite() && it > 0f } ?: 60f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT || event.values.isEmpty()) return
        val lux = event.values[0]
        if (!lux.isFinite()) return
        // The sensor is informational only. Never override the user's system/display brightness.
        currentLux = lux.coerceAtLeast(0f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun EyeCareApp(
    lux: Float?,
    sensorAvailable: Boolean,
    displayRefreshRateHz: Float
) {
    val ishiharaViewModel: IshiharaViewModel = viewModel(
        factory = IshiharaViewModelFactory(NoLicensedIshiharaDatasetRepository())
    )
    var page by androidx.compose.runtime.remember { mutableStateOf(AppPage.HOME) }
    var lastCffObservation by androidx.compose.runtime.remember { mutableStateOf<CffObservation?>(null) }
    var lastDistanceCm by androidx.compose.runtime.remember { mutableStateOf<Float?>(null) }
    var lightTask by androidx.compose.runtime.remember { mutableStateOf(LightTask.SCREEN_WORK) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("EyeCare Pro", style = MaterialTheme.typography.headlineLarge)
        Text("Edukasi dan skrining awal penglihatan", style = MaterialTheme.typography.titleMedium)
        if (page != AppPage.HOME) {
            Button(onClick = { page = AppPage.HOME }) { Text("Kembali ke beranda") }
        }
        when (page) {
            AppPage.HOME -> {
                Text("Pilih modul pemeriksaan", style = MaterialTheme.typography.titleLarge)
                HomeModuleButton("Pemeriksaan pencahayaan") { page = AppPage.ENVIRONMENT }
                HomeModuleButton("Tes Ishihara (menunggu materi resmi)") { page = AppPage.ISHIHARA }
                HomeModuleButton("Eksperimen kedip CFF/FFF") { page = AppPage.CFF }
                HomeModuleButton("Estimasi jarak wajah") { page = AppPage.FACE }
                HomeModuleButton("Pengingat 20-20-20") { page = AppPage.REMINDER }
                HomeModuleButton("Ringkasan pemeriksaan") { page = AppPage.RESULTS }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pengingat kesehatan mata", style = MaterialTheme.typography.titleMedium)
                        Text("Gunakan aturan 20-20-20: setiap 20 menit, lihat objek sekitar 20 kaki (6 meter) selama 20 detik.")
                    }
                }
                Text(
                    "Aplikasi ini bukan alat diagnosis dan tidak menggantikan pemeriksaan dokter mata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            AppPage.ENVIRONMENT -> EnvironmentScreen(lux, sensorAvailable, lightTask) { lightTask = it }
            AppPage.ISHIHARA -> IshiharaScreen(ishiharaViewModel)
            AppPage.CFF -> CffScreen(
                displayRefreshRateHz = displayRefreshRateHz,
                onResult = { lastCffObservation = it }
            )
            AppPage.FACE -> FaceDistanceScreen(onEstimate = { lastDistanceCm = it })
            AppPage.REMINDER -> ReminderScreen()
            AppPage.RESULTS -> ResultsScreen(lux, lastCffObservation, lastDistanceCm, lightTask)
        }
    }
}

@Composable
private fun HomeModuleButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun EnvironmentScreen(lux: Float?, sensorAvailable: Boolean, task: LightTask, onTaskChange: (LightTask) -> Unit) {
    val presentation = lux?.let { LightConditionEvaluator.evaluate(it, task).presentation(it, task) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Pemeriksaan lingkungan", style = MaterialTheme.typography.headlineSmall)
        Text("Pilih aktivitas yang sedang Anda lakukan", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { onTaskChange(LightTask.SCREEN_WORK) }, modifier = Modifier.fillMaxWidth()) { Text(if (task == LightTask.SCREEN_WORK) "✓ Pekerjaan dominan layar (300–500 lux)" else "Pekerjaan dominan layar (300–500 lux)") }
        Button(onClick = { onTaskChange(LightTask.READING) }, modifier = Modifier.fillMaxWidth()) { Text(if (task == LightTask.READING) "✓ Membaca/menulis (500–750 lux)" else "Membaca/menulis (500–750 lux)") }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Intensitas cahaya sekitar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (!sensorAvailable) {
                    Text("Sensor cahaya tidak tersedia pada perangkat ini.", color = Color(0xFFC62828))
                } else if (lux == null) {
                    Text("Menunggu pembacaan sensor…")
                } else {
                    Text(
                        "${String.format(Locale.getDefault(), "%.0f", lux)} lux",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(presentation!!.title, style = MaterialTheme.typography.titleLarge, color = presentation.color)
                    Text(presentation.advice)
                }
            }
        }
        Text("Rentang panduan yang dipilih: " + task.lowerLux.toInt() + "–" + task.upperLux.toInt() + " lux", style = MaterialTheme.typography.titleMedium)
        Text("Rentang ini merupakan panduan operasional edukatif untuk aktivitas, bukan standar medis atau batas diagnosis. Kebutuhan aktual bergantung pada usia, ukuran detail, kontras, silau, dan standar tempat kerja setempat.")
        Text("Cara memperbaiki hasil", style = MaterialTheme.typography.titleMedium)
        Text("Ukur di area kerja setinggi permukaan meja. Jika rendah: nyalakan lampu ruangan, buka tirai, atau arahkan lampu meja ke buku/meja. Jika tinggi atau silau: ubah arah lampu, gunakan diffuser/tirai, dan hindari pantulan pada layar.")
        Text("Catatan: sensor ponsel mengukur cahaya di dekat sensor, bukan selalu pencahayaan tepat di atas meja. Rentang mengikuti pilihan aktivitas di aplikasi dan hanya panduan operasional edukatif. ISO/CIE 8995-1:2025 menetapkan kebutuhan pencahayaan menurut jenis aktivitas dan kualitas pencahayaan; tidak ada satu angka lux medis universal. Kecerahan layar ponsel tidak diubah oleh modul ini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
