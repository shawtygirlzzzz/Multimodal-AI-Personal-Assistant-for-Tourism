package com.malacca.guide.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

object CameraManager {

    fun capturePhoto(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onCaptured: (Bitmap) -> Unit,
        onFailed: (String) -> Unit
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture
                )

                val outputFile = File.createTempFile("capture", ".jpg", context.cacheDir)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                            if (bitmap != null) onCaptured(bitmap)
                            else onFailed("Failed to decode captured image")
                        }
                        override fun onError(exception: ImageCaptureException) {
                            onFailed(exception.message ?: "Camera capture error")
                        }
                    }
                )
            } catch (e: Exception) {
                onFailed(e.message ?: "Camera setup error")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
