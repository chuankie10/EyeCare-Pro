package com.eyecare.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyecare.pro.domain.cff.CffStimulusTiming
import com.eyecare.pro.domain.cff.FlickerPerception
import com.eyecare.pro.presentation.cff.CffViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private const val MAX_TRIAL_DURATION_MILLIS = 10_000L

@Composable
fun CffScreen(
    displayRefreshRateHz: Float = 60f,
    onResult: (com.eyecare.pro.domain.cff.CffObservation) -> Unit = {},
    viewModel: CffViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val safeRefreshRate = displayRefreshRateHz.takeIf { it.isFinite() && it > 0f } ?: 60f
    val maxFrequencyHz = remember(safeRefreshRate) {
        CffStimulusTiming.maxAlternatingFrequencyHz(safeRefreshRate)
    }
    val canRun = maxFrequencyHz >= 5f

    LaunchedEffect(maxFrequencyHz) {
        viewModel.updateFrequencyLimit(maxFrequencyHz)
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                // Cancel as soon as the Activity pauses; ON_STOP is a second defensive boundary.
                viewModel.cancelSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelSession()
        }
    }

    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            delay(MAX_TRIAL_DURATION_MILLIS)
            viewModel.stopAutomatically()
        }
    }

    var flashOn by remember { mutableStateOf(false) }
    val stimulusFrequencyHz = if (state.isRunning) {
        state.lastStimulusFrequencyHz ?: state.targetFrequencyHz
    } else {
        state.targetFrequencyHz
    }
    val halfCycleNanos = remember(stimulusFrequencyHz) {
        CffStimulusTiming.halfCycleNanos(stimulusFrequencyHz)
    }
    LaunchedEffect(state.isRunning, halfCycleNanos) {
        if (!state.isRunning) {
            flashOn = false
            return@LaunchedEffect
        }
        var lastFrameNanos = 0L
        var accumulatedNanos = 0L
        while (true) {
            val frameTimeNanos = withFrameNanos { it }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameTimeNanos
            } else {
                val frameDeltaNanos = (frameTimeNanos - lastFrameNanos).coerceAtLeast(0L)
                lastFrameNanos = frameTimeNanos
                val phase = CffStimulusTiming.advanceFramePhase(
                    accumulatedNanos = accumulatedNanos,
                    frameDeltaNanos = frameDeltaNanos,
                    halfCycleNanos = halfCycleNanos
                )
                accumulatedNanos = phase.remainderNanos
                if (phase.shouldToggle) flashOn = !flashOn
            }
        }
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Eksperimen CFF / FFF", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Atur frekuensi target, amati bidang hitam-putih, lalu catat persepsi Anda. " +
                "Ini hanya eksperimen subjektif, bukan pengukuran CFF klinis."
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp).background(
                if (state.isRunning && flashOn) Color.White else Color.Black,
                RoundedCornerShape(16.dp)
            )
        )
        Text(
            "Refresh rate yang dilaporkan perangkat: " +
                String.format(Locale.ROOT, "%.1f Hz", safeRefreshRate)
        )
        Text(
            "Batas teoritis pergantian hitam-putih: " +
                String.format(Locale.ROOT, "%.1f Hz", maxFrequencyHz)
        )
        if (canRun) {
            Text(
                "Frekuensi target aplikasi: " +
                    String.format(Locale.ROOT, "%.1f Hz", stimulusFrequencyHz)
            )
            if (maxFrequencyHz > 5f) {
                Slider(
                    value = state.targetFrequencyHz,
                    onValueChange = viewModel::setTargetFrequency,
                    valueRange = 5f..maxFrequencyHz,
                    enabled = !state.isRunning && !state.awaitingObservation
                )
            }
        } else {
            Text(
                "Refresh rate perangkat terlalu rendah untuk rentang eksperimen ini.",
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            "Peringatan keselamatan: jangan jalankan stimulasi berkedip jika Anda memiliki " +
                "epilepsi atau riwayat kejang yang dipicu cahaya. Jika Anda memiliki riwayat " +
                "tersebut atau tidak yakin, jangan lanjutkan. Hentikan segera jika pusing, " +
                "mual, sakit kepala, atau tidak nyaman. Setiap percobaan dibatasi maksimal 10 detik."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.testTag("cff-warning-acknowledgement"),
                checked = state.warningAcknowledged,
                onCheckedChange = viewModel::setWarningAcknowledged,
                enabled = !state.isRunning && !state.awaitingObservation
            )
            Text("Saya telah membaca peringatan keselamatan.")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.testTag("cff-no-known-photosensitivity-risk"),
                checked = state.noKnownPhotosensitivityRiskConfirmed,
                onCheckedChange = viewModel::setNoKnownPhotosensitivityRiskConfirmed,
                enabled = !state.isRunning && !state.awaitingObservation
            )
            Text("Saya tidak memiliki epilepsi atau riwayat kejang akibat cahaya.")
        }
        Button(
            enabled = canRun && state.warningAcknowledged &&
                state.noKnownPhotosensitivityRiskConfirmed && !state.isRunning &&
                !state.awaitingObservation,
            onClick = { viewModel.start(safeRefreshRate) }
        ) { Text("Mulai stimulasi") }
        OutlinedButton(
            enabled = state.isRunning,
            onClick = viewModel::stop
        ) { Text("Hentikan stimulasi") }

        state.statusMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (state.awaitingObservation) {
            Text(
                "Apa yang Anda lihat pada target " +
                    String.format(Locale.ROOT, "%.1f Hz", state.lastStimulusFrequencyHz ?: 0f) + "?"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    viewModel.recordObservation(FlickerPerception.STILL_VISIBLE)?.let(onResult)
                }) { Text("Masih berkedip") }
                OutlinedButton(onClick = {
                    viewModel.recordObservation(FlickerPerception.APPEARS_FUSED)?.let(onResult)
                }) { Text("Tampak menyatu") }
            }
            OutlinedButton(onClick = {
                viewModel.recordObservation(FlickerPerception.UNCERTAIN)?.let(onResult)
            }) { Text("Tidak yakin") }
        }
        state.lastObservation?.let { observation ->
            Text(
                "Catatan lokal: target " +
                    String.format(Locale.ROOT, "%.1f", observation.requestedFrequencyHz) +
                    " Hz; ${observation.perception.displayLabel.lowercase(Locale.ROOT)}."
            )
        }
        Text(
            "Batas penting: pergantian hanya dijadwalkan pada frame Compose. Pada layar 60 Hz, " +
                "batas teoritis pola hitam-putih adalah sekitar 30 Hz; layar 90/120 Hz memiliki " +
                "batas teoritis berbeda. Frame yang terlewat, refresh rate adaptif, PWM, " +
                "kecerahan, panel, mode hemat daya, dan beban sistem dapat mengubah stimulus " +
                "sebenarnya. Nilai target bukan frekuensi fisik yang terukur, bukan ambang klinis, " +
                "dan tidak boleh digunakan sendirian untuk menyimpulkan kelelahan mata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
