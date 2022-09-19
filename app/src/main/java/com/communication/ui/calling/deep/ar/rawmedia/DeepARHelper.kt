package com.communication.ui.calling.deep.ar.rawmedia

import ai.deepar.ar.ARErrorType
import ai.deepar.ar.AREventListener
import ai.deepar.ar.DeepAR
import ai.deepar.ar.DeepARImageFormat
import ai.deepar.ar.DeepARPixelFormat
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy
import com.communication.ui.calling.deep.ar.BuildConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DeepARHelper(context: Context) : AREventListener {
    private var deepAR: DeepAR = DeepAR(context)
    private var initialized: Boolean = false
    private var effects: ArrayList<String> = ArrayList()

    private var buffers: Array<ByteBuffer?>? = null
    private var currentBuffer = 0
    private val numberOfBuffers = 2
    private var currentEffect = 12
    var onImageProcessed: ((image: Image) -> Unit)? = null

    init {
        deepAR.setLicenseKey(BuildConfig.DEEP_AR_KEY)
        initializeFilters()
        deepAR.initialize(context, this)
        deepAR.changeLiveMode(false)
    }

    fun setFrameGenerator(genrator: FrameGenerator) {
    }


    fun startDeepAR() {
        deepAR.setOffscreenRendering(1280, 720, DeepARPixelFormat.RGBA_8888)
    }

    private fun initializeFilters() {
        effects = java.util.ArrayList()
        effects.add("none")
        effects.add("viking_helmet.deepar")
        effects.add("MakeupLook.deepar")
        effects.add("Split_View_Look.deepar")
        effects.add("Emotions_Exaggerator.deepar")
        effects.add("Emotion_Meter.deepar")
        effects.add("Stallone.deepar")
        effects.add("flower_face.deepar")
        effects.add("galaxy_background.deepar")
        effects.add("Humanoid.deepar")
        effects.add("Neon_Devil_Horns.deepar")
        effects.add("Ping_Pong.deepar")
        effects.add("Pixel_Hearts.deepar")
        effects.add("Snail.deepar")
        effects.add("Hope.deepar")
        effects.add("Vendetta_Mask.deepar")
        effects.add("Fire_Effect.deepar")
        effects.add("burning_effect.deepar")
        effects.add("Elephant_Trunk.deepar")
    }

    private fun getFilterPath(filterName: String): String? =
        if (filterName == "none") null else "file:///android_asset/$filterName"

    fun stopDeepAR() {
        deepAR.release()
        initialized = false
    }

    fun processImage(image: ImageProxy, mirroring: Boolean) {
        if (initialized) {
            buffers = arrayOfNulls(numberOfBuffers)
            for (i in 0 until numberOfBuffers) {
                buffers?.set(i, ByteBuffer.allocateDirect(image.width * image.height * 3))
                buffers?.get(i)?.order(ByteOrder.nativeOrder())
                buffers?.get(i)?.position(0)
            }

            val byteData: ByteArray
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            byteData = ByteArray(ySize + uSize + vSize)

            yBuffer[byteData, 0, ySize]
            vBuffer[byteData, ySize, vSize]
            uBuffer[byteData, ySize + vSize, uSize]
            buffers?.get(currentBuffer)?.put(byteData)
            buffers?.get(currentBuffer)?.position(0)
            deepAR.receiveFrame(
                buffers?.get(currentBuffer),
                image.width, image.height,
                image.imageInfo.rotationDegrees,
                mirroring,
                DeepARImageFormat.YUV_420_888,
                image.planes[1].pixelStride
            )
            currentBuffer = (currentBuffer + 1) % numberOfBuffers
            image.close()
        }
    }

    override fun screenshotTaken(p0: Bitmap?) {}

    override fun videoRecordingStarted() {}

    override fun videoRecordingFinished() {}

    override fun videoRecordingFailed() {}

    override fun videoRecordingPrepared() {}

    override fun shutdownFinished() {}

    override fun initialized() {
        deepAR.switchEffect("effect", getFilterPath(effects[currentEffect]))
        initialized = true
    }

    override fun faceVisibilityChanged(p0: Boolean) {}
    override fun imageVisibilityChanged(p0: String?, p1: Boolean) {}

    override fun frameAvailable(image: Image?) {
        try {
            image?.let {
                onImageProcessed?.invoke(image)
            }
        } catch (e: Exception) {
        }
    }

    override fun error(p0: ARErrorType?, p1: String?) {}

    override fun effectSwitched(p0: String?) {}
}