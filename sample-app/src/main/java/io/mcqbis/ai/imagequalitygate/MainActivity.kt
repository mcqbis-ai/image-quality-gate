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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.mcqbis.ai.imagequalitygate.ImageQualityGate
import java.util.concurrent.Executors
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermission()

        setContent {
            DebugCameraScreen(
                onCaptureRequest = { capture() }
            )
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

    private fun capture(onResult: ((Bitmap) -> Unit)? = null) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()

                    onResult?.invoke(bitmap)
                }
            }
        )
    }

    @Composable
    fun DebugCameraScreen(
        onCaptureRequest: () -> Unit
    ) {

        var resultText by remember { mutableStateOf("No image yet") }

        Column(modifier = Modifier.fillMaxSize()) {

            CameraPreview(
                onImageCaptureReady = { captureRef ->
                    imageCapture = captureRef
                }
            )

            Button(
                onClick = {

//                    capture { bitmap ->
//
//                        val result = ImageQualityGate.analyze(bitmap)
//
//                        resultText = """
//                            Blur: ${result.blurScore}
//                            Exposure: ${result.exposureScore}
//                            Noise: ${result.noiseScore}
//                            Contrast: ${result.contrastScore}
//                            Summary: ${result.summaryScore}
//                        """.trimIndent()
//                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Capture & Analyze")
            }

            Text(
                text = resultText,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Composable
    fun CameraPreview(
        onImageCaptureReady: (ImageCapture) -> Unit
    ) {

        val context = LocalContext.current
        val previewView = remember { PreviewView(context) }

        AndroidView(factory = {

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                preview.setSurfaceProvider(previewView.surfaceProvider)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onImageCaptureReady(imageCapture)

            }, ContextCompat.getMainExecutor(context))

            previewView
        })
    }
}

/**
 * Convert ImageProxy → Bitmap (MVP version)
 */
fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}