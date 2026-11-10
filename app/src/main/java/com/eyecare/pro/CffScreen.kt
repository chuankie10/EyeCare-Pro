package com.eyecare.pro

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun CffScreen() {
    var frequency by remember { mutableFloatStateOf(12f) }
    var running by remember { mutableStateOf(false) }
    var reportedThreshold by remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "experimental-cff")
    val halfCycleMs = (500f / frequency).toInt().coerceAtLeast(16)
    val animatedColor by transition.animateColor(
        initialValue = Color.Black,
        targetValue = Color.White,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = halfCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Eksperimen kedip (CFF)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Atur frekuensi dan amati apakah bidang masih terlihat berkedip atau tampak menyatu. Hentikan bila terasa tidak nyaman.",
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp)
                .background(if (running) animatedColor else Color.DarkGray, RoundedCornerShape(16.dp))
        )
        Text("Frekuensi stimulasi: ${String.format(Locale.getDefault(), "%.0f", frequency)} Hz")
        Slider(value = frequency, onValueChange = { frequency = it }, valueRange = 5f..30f, steps = 24)
        Button(onClick = { running = !running }) {
            Text(if (running) "Hentikan stimulasi" else "Mulai stimulasi")
        }
        Button(
            enabled = !running,
            onClick = { reportedThreshold = frequency }
        ) {
            Text("Catat frekuensi yang diamati")
        }
        if (reportedThreshold > 0f) {
            Text("Catatan pengamatan terakhir: ${String.format(Locale.getDefault(), "%.0f", reportedThreshold)} Hz")
        }
        Text(
            "PENTING: ini bukan pengukuran CFF klinis. Animasi Android mengikuti frame rate dan penjadwalan sistem; layar dapat membatasi atau mengubah kedipan. PWM, refresh rate, brightness, dan panel tiap perangkat berbeda. Nilai yang dipilih hanya catatan pengamatan, bukan hasil diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
