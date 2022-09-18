package com.communication.ui.calling.deep.ar.rawmedia

import android.content.Context
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.azure.android.communication.calling.SoftwareBasedVideoFrameSender
import com.azure.android.communication.calling.VideoFrameSender
import com.azure.android.communication.calling.VideoFrameSenderChangedEvent
import com.azure.android.communication.calling.VideoFrameSenderChangedListener
import com.communication.ui.calling.deep.ar.MainActivity
import java.nio.ByteBuffer
import java.util.*

class FrameGenerator(val context: Context) :
    VideoFrameSenderChangedListener {
    private val random: Random
    private var videoFrameSender: VideoFrameSender? = null
    private var frameIteratorThread: Thread? = null
    val c = CameraHelper(context)

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
        MainActivity.deepARHelper?.setFrameGenerator(this)

        c.onImageReceived = { imageProxy, mirroring ->
            try {

                Handler(Looper.getMainLooper()).post {
                    MainActivity.deepARHelper?.processImage(imageProxy, mirroring)
                }
            } catch (ex: Exception) {
                ex.message
            }

        }
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
    }
}


