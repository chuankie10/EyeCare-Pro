package com.eyecare.pro

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
fun FaceDistanceScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    var eyeDistancePx by remember { mutableStateOf<Float?>(null) }
    var calibratedEyeDistancePx by remember { mutableStateOf<Float?>(null) }
    var distanceText by remember { mutableStateOf("40") }
    var estimatedDistanceCm by remember { mutableStateOf<Float?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Estimasi jarak wajah", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Letakkan wajah di tengah kamera depan. Untuk kalibrasi, ukur jarak kamera ke wajah dengan penggaris, masukkan nilainya, lalu tekan Kalibrasi.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!permissionGranted) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Izinkan kamera")
            }
        } else {
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
            val detector = remember {
                FaceDetection.getClient(
                    FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build()
                )
            }
            val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

            DisposableEffect(previewView, lifecycleOwner) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(analyzerExecutor) { proxy ->
                            val mediaImage = proxy.image
                            if (mediaImage == null) {
                                proxy.close()
                            } else {
                                val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                                detector.process(input)
                                    .addOnSuccessListener { faces ->
                                        val face = faces.maxByOrNull { it.boundingBox.width() }
                                        val left = face?.getLandmark(FaceLandmark.LEFT_EYE)?.position
                                        val right = face?.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                                        if (left != null && right != null) {
                                            val measured = abs(left.x - right.x)
                                            if (measured > 1f) {
                                                eyeDistancePx = measured
                                                val referencePx = calibratedEyeDistancePx
                                                val referenceCm = distanceText.toFloatOrNull()
                                                if (referencePx != null && referenceCm != null && referenceCm > 0f) {
                                                    estimatedDistanceCm = referenceCm * referencePx / measured
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { cameraError = "Deteksi wajah gagal. Pastikan wajah terlihat jelas." }
                                    .addOnCompleteListener { proxy.close() }
                            }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                    } catch (error: Exception) {
                        cameraError = "Kamera depan tidak dapat dibuka: ${error.localizedMessage ?: "kesalahan perangkat"}"
                    }
                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    runCatching { providerFuture.get().unbindAll() }
                    analyzerExecutor.shutdown()
                    detector.close()
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
            Text(
                eyeDistancePx?.let { "Landmark mata terdeteksi (${String.format(Locale.getDefault(), "%.1f", it)} px)" }
                    ?: "Mencari landmark kedua mata…"
            )
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("Jarak terukur saat kalibrasi (cm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = eyeDistancePx != null && (distanceText.toFloatOrNull() ?: 0f) > 0f,
                onClick = {
                    calibratedEyeDistancePx = eyeDistancePx
                    estimatedDistanceCm = distanceText.toFloatOrNull()
                }
            ) {
                Text("Kalibrasi pada jarak ini")
            }
            Text(
                estimatedDistanceCm?.let { "Perkiraan jarak saat ini: ${String.format(Locale.getDefault(), "%.1f", it)} cm" }
                    ?: "Lakukan kalibrasi untuk mulai menghitung jarak."
            )
            cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Batasan: hasil ini hanya perkiraan relatif berdasarkan perubahan jarak antarmata pada gambar kamera. Kalibrasi harus dilakukan ulang jika posisi, zoom, kamera, atau orientasi berubah. Ini bukan alat ukur klinis atau penentu gangguan mata.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
