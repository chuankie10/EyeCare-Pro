package com.eyecare.pro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
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
import com.eyecare.pro.domain.cff.CffObservation
import java.util.Locale

private enum class AppPage { HOME, ENVIRONMENT, ISHIHARA, CFF, FACE, REMINDER, RESULTS }

private data class LightPresentation(val title: String, val advice: String, val color: Color)

private fun LightCondition.presentation(): LightPresentation = when (this) {
    LightCondition.TOO_DARK -> LightPresentation(
        "Terlalu redup",
        "Pindah ke tempat yang lebih terang sebelum melakukan pemeriksaan.",
        Color(0xFFC62828)
    )
    LightCondition.IDEAL -> LightPresentation(
        "Pencahayaan sesuai",
        "Kondisi cahaya berada pada rentang operasional aplikasi.",
        Color(0xFF2E7D32)
    )
    LightCondition.TOO_BRIGHT -> LightPresentation(
        "Terlalu terang",
        "Kurangi cahaya langsung atau pindah ke tempat yang lebih teduh.",
        Color(0xFFEF6C00)
    )
}

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var currentLux by mutableStateOf<Float?>(null)
    private var sensorAvailable by mutableStateOf(true)
    private var displayRefreshRateHz by mutableStateOf(60f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
        updateDisplayRefreshRate()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun updateDisplayRefreshRate() {
        val reportedRate = windowManager.defaultDisplay.refreshRate
        displayRefreshRateHz = reportedRate.takeIf { it.isFinite() && it > 0f } ?: 60f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT || event.values.isEmpty()) return
        val lux = event.values[0]
        if (!lux.isFinite()) return
        val safeLux = lux.coerceAtLeast(0f)
        currentLux = safeLux
        val brightness = if (LightConditionEvaluator.evaluate(safeLux) == LightCondition.IDEAL) {
            0.8f
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = window.attributes.apply { screenBrightness = brightness }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun EyeCareApp(
    lux: Float?,
    sensorAvailable: Boolean,
    displayRefreshRateHz: Float
) {
    var page by androidx.compose.runtime.remember { mutableStateOf(AppPage.HOME) }
    var lastCffObservation by androidx.compose.runtime.remember { mutableStateOf<CffObservation?>(null) }
    var lastDistanceCm by androidx.compose.runtime.remember { mutableStateOf<Float?>(null) }
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
                HomeModuleButton("Tes warna Ishihara") { page = AppPage.ISHIHARA }
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
            AppPage.ENVIRONMENT -> EnvironmentScreen(lux, sensorAvailable)
            AppPage.ISHIHARA -> IshiharaScreen()
            AppPage.CFF -> CffScreen(
                displayRefreshRateHz = displayRefreshRateHz,
                onResult = { lastCffObservation = it }
            )
            AppPage.FACE -> FaceDistanceScreen(onEstimate = { lastDistanceCm = it })
            AppPage.REMINDER -> ReminderScreen()
            AppPage.RESULTS -> ResultsScreen(lux, lastCffObservation, lastDistanceCm)
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
private fun EnvironmentScreen(lux: Float?, sensorAvailable: Boolean) {
    val presentation = lux?.let { LightConditionEvaluator.evaluate(it).presentation() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Pemeriksaan lingkungan", style = MaterialTheme.typography.headlineSmall)
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
                        "\${String.format(Locale.getDefault(), "%.0f", lux)} lux",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(presentation!!.title, style = MaterialTheme.typography.titleLarge, color = presentation.color)
                    Text(presentation.advice)
                }
            }
        }
        Text("Rentang operasional aplikasi: 100–400 lux.")
        Text(
            "Saat cahaya berada pada rentang tersebut, kecerahan layar aplikasi diatur ke 80%. Pengaturan ini hanya berlaku selama aplikasi aktif.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Rentang ini adalah ambang operasional aplikasi, bukan standar medis. Sensor dan hasilnya dapat berbeda antarperangkat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
