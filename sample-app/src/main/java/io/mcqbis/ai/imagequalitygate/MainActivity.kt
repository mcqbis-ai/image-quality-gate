package io.mcqbis.ai.imagequalitygate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermission()

        setContent {
            DebugCameraScreen()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                0
            )
        }
    }

    @Composable
    fun DebugCameraScreen() {

        var selectedTab by remember { mutableIntStateOf(0) }
        var qualityResult by remember { mutableStateOf<ImageQualityResult?>(null) }
        var preprocessedBitmap by remember { mutableStateOf<Bitmap?>(null) }

        val tabs = listOf("Metrics", "Preview")

        Column(modifier = Modifier.fillMaxSize()) {

            CameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onResult = { result ->
                    qualityResult = result
                    preprocessedBitmap = result.debugInfo?.preprocessedBitmap
                }
            )

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when (selectedTab) {
                    0 -> MetricsTab(result = qualityResult)
                    1 -> PreviewTab(bitmap = preprocessedBitmap)
                }
            }
        }
    }

    @Composable
    fun MetricsTab(result: ImageQualityResult?) {
        if (result == null) {
            Text("Oczekiwanie na analizę…")
            return
        }

        val debug = result.debugInfo

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Metryka", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Wynik", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("Czas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()

            // Preprocessing — brak score
            MetricTableRow("Preprocessing", score = null, timeMs = debug?.preprocessingTimeMs)
            MetricTableRow("Rozmycie",      score = result.blurScore,     timeMs = debug?.blurTimeMs)
            MetricTableRow("Ekspozycja",    score = result.exposureScore,  timeMs = debug?.exposureTimeMs)
            MetricTableRow("Szum",          score = result.noiseScore,     timeMs = debug?.noiseTimeMs)
            MetricTableRow("Kontrast",      score = result.contrastScore,  timeMs = debug?.contrastTimeMs)

            HorizontalDivider()
            MetricTableRow("Łącznie", score = result.summaryScore, timeMs = debug?.totalTimeMs)
        }
    }

    @Composable
    fun MetricTableRow(label: String, score: Float?, timeMs: Long?) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label,                                          modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(score?.let { "%.3f".format(it) } ?: "—",       modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(timeMs?.let { "${it} ms" } ?: "—",             modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    fun PreviewTab(bitmap: Bitmap?) {
        if (bitmap == null) {
            Text("Oczekiwanie na podgląd…")
            return
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Wstępnie przetworzone zdjęcie",
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        onResult: (ImageQualityResult) -> Unit
    ) {
        val context = LocalContext.current
        val previewView = remember { PreviewView(context) }

        // Callback wrapped in ref to avoid restarting the camera on recomposition
        val onResultRef = rememberUpdatedState(onResult)

        AndroidView(
            factory = {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    var lastAnalysisMs = 0L
                    val frameIntervalMs = 200L // 5 FPS

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysisMs >= frameIntervalMs) {
                            lastAnalysisMs = now

                            val bitmap = imageProxy.toBitmap()
                            val result = ImageQualityGate.analyze(
                                bitmap,
                                enableDebugInfo = true
                            )
                            onResultRef.value(result)
                        }
                        imageProxy.close()
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = modifier
        )
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}