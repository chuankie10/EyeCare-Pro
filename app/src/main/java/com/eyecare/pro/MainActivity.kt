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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import java.util.Locale

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorAvailable = lightSensor != null

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EnvironmentScreen(lux = currentLux, sensorAvailable = sensorAvailable)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        super.onPause()
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
private fun EnvironmentScreen(lux: Float?, sensorAvailable: Boolean) {
    val presentation = lux?.let { LightConditionEvaluator.evaluate(it).presentation() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("EyeCare Pro", style = MaterialTheme.typography.headlineLarge)
        Text("Pemeriksaan lingkungan", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))

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
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format(Locale.getDefault(), "%.0f", lux), style = MaterialTheme.typography.displayLarge)
                        Text(" lux", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(presentation!!.title, style = MaterialTheme.typography.titleLarge, color = presentation.color)
                    Text(presentation.advice, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Rentang operasional aplikasi: 100–400 lux.", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Saat cahaya berada dalam rentang tersebut, kecerahan layar aplikasi diatur ke 80%. Pengaturan ini hanya berlaku selama aplikasi aktif.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Catatan: rentang ini adalah ambang operasional aplikasi, bukan standar medis. Sensor dan hasilnya dapat berbeda antarperangkat.",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
