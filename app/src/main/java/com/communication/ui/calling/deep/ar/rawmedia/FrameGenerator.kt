package com.communication.ui.calling.deep.ar.rawmedia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageProxy
import com.azure.android.communication.calling.SoftwareBasedVideoFrameSender
import com.azure.android.communication.calling.VideoFrameSender
import com.azure.android.communication.calling.VideoFrameSenderChangedEvent
import com.azure.android.communication.calling.VideoFrameSenderChangedListener
import com.communication.ui.calling.deep.ar.ImageSegmentationHelper
import com.communication.ui.calling.deep.ar.MainActivity
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.max

class FrameGenerator(val context: Context) :
    VideoFrameSenderChangedListener, ImageSegmentationHelper.SegmentationListener {

    companion object {
        private const val ALPHA_COLOR = 128
    }

    private val random: Random
    private var videoFrameSender: VideoFrameSender? = null
    private var frameIteratorThread: Thread? = null
    val c = CameraHelper(context)

    private lateinit var imageSegmentationHelper: ImageSegmentationHelper
    private lateinit var bitmapBuffer: Bitmap

    @Volatile
    private var stopFrameIterator = false

    var byteArray: ByteArray? = null
    var byteBuffer: ByteBuffer? = null


    fun clone(original: ByteBuffer): ByteBuffer? {
        val clone = ByteBuffer.allocate(original.capacity())
        original.rewind() //copy from the beginning
        clone.put(original)
        original.rewind()
        clone.flip()
        return clone
    }

    private fun frameIterator() {
        //MainActivity.deepARHelper?.setFrameGenerator(this)

        c.onImageReceived = { imageProxy, mirroring ->
            try {

                /*Handler(Looper.getMainLooper()).post {
                    MainActivity.deepARHelper?.processImage(imageProxy, mirroring)
                }*/
                //sendImage(imageProxy.image)
               /* if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    bitmapBuffer = Bitmap.createBitmap(
                        imageProxy.image!!.width,
                        imageProxy.image!!.height,
                        Bitmap.Config.ARGB_8888
                    )
                }
                segmentImage(imageProxy)*/

                //segmentImage(imageProxy)
                val width = imageProxy.image!!.width
                val height = imageProxy.image!!.height

                val bitmap =
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        setHasAlpha(true)
                        copyPixelsFromBuffer(imageProxy.image!!.planes[0].buffer)
                    }

                val imageRotation = imageProxy.imageInfo.rotationDegrees
                // Pass Bitmap and rotation to the image segmentation helper for processing and segmentation
                val segmentResult: List<Segmentation> =
                    imageSegmentationHelper.segment(bitmap, imageRotation)!!

                //imageProxy.image!!.close()

                if (segmentResult != null && segmentResult.isNotEmpty()) {
                    val colorLabels = segmentResult[0].coloredLabels.mapIndexed { index, coloredLabel ->
                        ColorLabel(
                            index,
                            coloredLabel.displayName,
                            coloredLabel.argb
                        )
                    }

                    // Create the mask bitmap with colors and the set of detected labels.
                    // We only need the first mask for this sample because we are using
                    // the OutputType CATEGORY_MASK, which only provides a single mask.
                    val maskTensor = segmentResult[0].masks[0]
                    val maskArray = maskTensor.buffer.array()
                    val pixels = IntArray(maskArray.size)

                    for (i in maskArray.indices) {
                        // Set isExist flag to true if any pixel contains this color.
                        val colorLabel = colorLabels[maskArray[i].toInt()].apply {
                            isExist = true
                        }
                        val color = colorLabel.getColor()
                        pixels[i] = color
                    }
                    imageProxy.image!!.close()
                    val image = Bitmap.createBitmap(
                        pixels,
                        maskTensor.width,
                        maskTensor.height,
                        Bitmap.Config.ARGB_8888
                    )

                    Handler(Looper.getMainLooper()).post {
                        // below code is POC code
                        // In production probably Texture View should be used
                        val surfaceView = MainActivity.callComposite.pipSurfaceView
                        val canvas: Canvas = surfaceView.holder.lockCanvas()

                        try {

                            val scaleFactor = max(surfaceView.width * 1f / width, surfaceView.height * 1f / height)
                            val scaleWidth = (width * scaleFactor).toInt()
                            val scaleHeight = (height * scaleFactor).toInt()

                            val scaleBitmap = Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false)

                            /*val b: Bitmap = BitmapHelper.resizeBitmap(
                                im!!,
                                surfaceView.width,
                                surfaceView.height)*/

                            canvas.drawBitmap(scaleBitmap,
                                0.0f, 0.0f, null
                            )


                        } finally {
                            surfaceView.holder.unlockCanvasAndPost(canvas)
                        }
                    }
                }



                /*val sender = videoFrameSender as SoftwareBasedVideoFrameSender?
                val timeStamp = sender!!.timestampInTicks
                val videoFormat = videoFrameSender!!.videoFormat
                byteBuffer?.position(0)
                sender.sendFrame(imageProxy.image!!.planes[0].buffer, timeStamp).get()

                try {
                    Thread.sleep((1000.0f / videoFormat.framesPerSecond).toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }*/

            } catch (ex: Exception) {
                ex.message
            }

        }
    }

    private fun segmentImage(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the image segmentation helper for processing and segmentation
        imageSegmentationHelper.segment(bitmapBuffer, imageRotation)

    }

    /**
     * Starts thread
     */
    private fun startFrameIterator() {
        frameIterator()
    }

    /**
     * Stops thread
     */
    fun stopFrameIterator() {
        try {
            if (frameIteratorThread != null) {
                stopFrameIterator = true
                Log.d("FrameGenerator", "FrameGenerator.Stop trace, before stopping")
                frameIteratorThread!!.join()
                frameIteratorThread = null
                Log.d("FrameGenerator", "FrameGenerator.Stop trace, after stopping")
                stopFrameIterator = false
            }
        } catch (ex: InterruptedException) {
            ex.message
        }
    }

    override fun onVideoFrameSenderChanged(event: VideoFrameSenderChangedEvent) {
        if (event.videoFrameSender == null) {
            Log.d("FrameGenerator",
                "FrameGenerator.onVideoFrameSenderChanged trace, video frame sender cleaned")
        } else {
            Log.d("FrameGenerator",
                "FrameGenerator.onVideoFrameSenderChanged trace, video frame sender set")
        }
        stopFrameIterator()
        videoFrameSender = event.videoFrameSender
        Handler(Looper.getMainLooper()).post {
            c.startCamera()
        }
        startFrameIterator()
    }

    fun sendImage(image: Image?) {

        try {

            val bitmap =
                Bitmap.createBitmap(image!!.width, image.height, Bitmap.Config.ARGB_8888).apply {
                    setHasAlpha(false)
                    copyPixelsFromBuffer(image.planes[0].buffer.apply {
                        order(ByteOrder.nativeOrder())
                        rewind()
                    })
                }

            Handler(Looper.getMainLooper()).post {
                // below code is POC code
                // In production probably Texture View should be used
                val surfaceView = MainActivity.callComposite.pipSurfaceView
                val canvas: Canvas = surfaceView.holder.lockCanvas()

                try {
                    val b = BitmapHelper.resizeBitmap(
                        bitmap!!,
                        surfaceView.width,
                        surfaceView.height)

                    canvas.drawBitmap(b,
                        0.0f, 0.0f, null
                    )
                } finally {
                    surfaceView.holder.unlockCanvasAndPost(canvas)
                }
            }

            val sender = videoFrameSender as SoftwareBasedVideoFrameSender?
            val timeStamp = sender!!.timestampInTicks
            val videoFormat = videoFrameSender!!.videoFormat
            byteBuffer?.position(0)
            sender.sendFrame(image!!.planes[0].buffer, timeStamp).get()

            try {
                Thread.sleep((1000.0f / videoFormat.framesPerSecond).toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }


        } catch (ex: Exception) {
            ex.message
        }
    }

    /**
     * Initializes the random generator
     * @param context
     */
    init {
        random = Random()
        imageSegmentationHelper = ImageSegmentationHelper(
            context = context,
            imageSegmentationListener = this
        )
    }

    override fun onError(error: String) {

    }

    override fun onResults(
        results: List<Segmentation>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        setResult(results, imageHeight, imageWidth)
    }

    private fun setResult(segmentResult: List<Segmentation>?,
                          imageHeight: Int,
                          imageWidth: Int,) {
        try {
            if (segmentResult != null && segmentResult.isNotEmpty()) {
                val colorLabels = segmentResult[0].coloredLabels.mapIndexed { index, coloredLabel ->
                    ColorLabel(
                        index,
                        coloredLabel.displayName,
                        coloredLabel.argb
                    )
                }

                // Create the mask bitmap with colors and the set of detected labels.
                // We only need the first mask for this sample because we are using
                // the OutputType CATEGORY_MASK, which only provides a single mask.
                val maskTensor = segmentResult[0].masks[0]
                val maskArray = maskTensor.buffer.array()
                val pixels = IntArray(maskArray.size)

                for (i in maskArray.indices) {
                    // Set isExist flag to true if any pixel contains this color.
                    val colorLabel = colorLabels[maskArray[i].toInt()].apply {
                        isExist = true
                    }
                    val color = colorLabel.getColor()
                    pixels[i] = color
                }

                val im = Bitmap.createBitmap(
                    pixels,
                    maskTensor.width,
                    maskTensor.height,
                    Bitmap.Config.ARGB_8888
                )

                Handler(Looper.getMainLooper()).post {
                    // below code is POC code
                    // In production probably Texture View should be used
                    val surfaceView = MainActivity.callComposite.pipSurfaceView
                    val canvas: Canvas = surfaceView.holder.lockCanvas()

                    try {

                        val scaleFactor = max(surfaceView.width * 1f / maskTensor.width, surfaceView.height * 1f / maskTensor.height)
                        val scaleWidth = (maskTensor.width * scaleFactor).toInt()
                        val scaleHeight = (maskTensor.height * scaleFactor).toInt()

                        val scaleBitmap = Bitmap.createScaledBitmap(im, scaleWidth, scaleHeight, false)

                        /*val b: Bitmap = BitmapHelper.resizeBitmap(
                            im!!,
                            surfaceView.width,
                            surfaceView.height)*/

                        canvas.drawBitmap(scaleBitmap,
                            0.0f, 0.0f, null
                        )
                    } finally {
                        surfaceView.holder.unlockCanvasAndPost(canvas)
                    }
                }

//                val buffer = image.planes[0].buffer
//
//                if(byteArray == null) {
//                    byteArray = ByteArray(buffer.remaining())
//                }
//
//                buffer[byteArray]
//
//                val bitmap = Helper.bitmapFromRgba(image.width, image.height, byteArray)
//                val copyByteArray =  Helper.bitmapToRgba(bitmap)
//
//                if(byteBuffer == null) {
//                    byteBuffer = ByteBuffer.allocateDirect(copyByteArray.size)
//
//                    //Create clone with matching capacity and byte order
//                    byteBuffer!!.order(ByteOrder.nativeOrder())
//                }
//
//
//                Helper.cloneByteBuffer(copyByteArray, byteBuffer)
//
//                image.close()
//
//                val sender = videoFrameSender as SoftwareBasedVideoFrameSender?
//                val timeStamp = sender!!.timestampInTicks
//                val videoFormat = videoFrameSender!!.videoFormat
//                byteBuffer?.position(0)
//                val c = sender.sendFrame( byteBuffer ,timeStamp).get()
//
//
//                try {
//                    Thread.sleep((1000.0f / videoFormat.framesPerSecond).toLong())
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                }

                /*   val sender = videoFrameSender as SoftwareBasedVideoFrameSender?
                   val timeStamp = sender!!.timestampInTicks
                   val videoFormat = videoFrameSender!!.videoFormat
                   byteBuffer?.position(0)

                   sender.sendFrame(, timeStamp).get()

                   try {
                       Thread.sleep((1000.0f / videoFormat.framesPerSecond).toLong())
                   } catch (e: InterruptedException) {
                       e.printStackTrace()
                   }

                   sendImage(image)*/
                //image.close()
                //listener?.onLabels(colorLabels.filter { it.isExist })
            }
        } catch (ex: Exception) {

        }

    }

    data class ColorLabel(
        val id: Int,
        val label: String,
        val rgbColor: Int,
        var isExist: Boolean = false
    ) {

        fun getColor(): Int {
            // Use completely transparent for the background color.
            return if (id != 0) Color.TRANSPARENT else Color.argb(
                ALPHA_COLOR,
                Color.red(rgbColor),
                Color.green(rgbColor),
                Color.blue(rgbColor)
            )
        }
    }
}


