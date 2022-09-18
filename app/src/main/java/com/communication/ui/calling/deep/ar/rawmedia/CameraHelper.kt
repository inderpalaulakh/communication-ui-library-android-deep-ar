package com.communication.ui.calling.deep.ar.rawmedia

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(var context: Context) {
    private var started: Boolean = false
    private lateinit var customLifecycle: CustomLifecycle
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalysis: ImageAnalysis
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    var onImageReceived: ((image: ImageProxy, mirror: Boolean) -> Unit)? = null


    fun startCamera() {
        lensFacing = CameraSelector.LENS_FACING_FRONT
        customLifecycle = CustomLifecycle()
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
        started = true
    }

    private fun bindCameraUseCases() {
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(400, 450))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
            // .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            //   .setOutputImageRotationEnabled(true)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { image ->
            onImageReceived?.invoke(image, lensFacing == CameraSelector.LENS_FACING_FRONT)
            // image.close()
        }

        val cam2Infos = cameraProvider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            // HARDWARE_LEVEL is Int type, with the order of:
            // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        val cameraSelector =
            CameraSelector.Builder().addCameraFilter {
                it.filter { camInfo ->
                    // cam2Infos[0] is either EXTERNAL or best built-in camera
                    val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
                    thisCamId == cam2Infos[0].cameraId
                }
            }.build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                customLifecycle,
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {

        }
    }

    fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        if (started) {
            bindCameraUseCases()
        }
    }

    fun stopCamera() {
        if (started) {
            imageAnalysis.clearAnalyzer()
            Handler(Looper.getMainLooper()).post {
                cameraProvider.unbindAll()
            }
            cameraProviderFuture.cancel(true)
            customLifecycle.destroy()
            started = false
        }
    }
}

class CustomLifecycle : LifecycleOwner {
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun destroy() = Handler(Looper.getMainLooper()).post {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}