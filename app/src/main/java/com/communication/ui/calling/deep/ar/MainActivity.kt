package com.communication.ui.calling.deep.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.communication.ui.calling.deep.ar.rawmedia.CameraHelper
import com.communication.ui.calling.deep.ar.rawmedia.DeepARHelper
import com.communication.ui.calling.deep.ar.rawmedia.Helper
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "ACS_DEEPAR "
        var deepARHelper: DeepARHelper? = null
    }

    private lateinit var surfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener { l -> startCallComposite() }
        surfaceView = findViewById(R.id.surfaceview)
    }

    override fun onStart() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO),
                1)
        }
        super.onStart()
    }

    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth.toFloat(), scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false)
        bm.recycle()
        return resizedBitmap
    }

    private fun startCallComposite() {

        val cameraHelper = CameraHelper(this)

        deepARHelper = DeepARHelper(this)
        deepARHelper?.startDeepAR()

        deepARHelper?.onImageProcessed = { image ->
            Log.d(TAG, image.width.toString() + " " + image.height.toString())

            val canvas: Canvas = surfaceView.holder.lockCanvas()
            val bitmap =
                Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).apply {
                    setHasAlpha(false)
                    copyPixelsFromBuffer(image.planes[0].buffer.apply {
                        order(ByteOrder.nativeOrder())
                        rewind()
                    })
                }

            try {



                val b = Helper.resizeBitmap(
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

        cameraHelper.onImageReceived = { image, mirroring ->
            Handler(Looper.getMainLooper()).post {
                deepARHelper?.processImage(image, mirroring)
            }
        }

        cameraHelper.startCamera()

        /*val communicationTokenRefreshOptions =
            CommunicationTokenRefreshOptions({ fetchToken() }, true)
        val communicationTokenCredential =
            CommunicationTokenCredential(communicationTokenRefreshOptions)

        val locator: CallCompositeJoinLocator =
            CallCompositeGroupCallLocator(UUID.fromString(BuildConfig.GROUP_CALL_ID))
        val remoteOptions =
            CallCompositeRemoteOptions(locator,
                communicationTokenCredential,
                BuildConfig.DISPLAY_NAME)

        val callComposite: CallComposite = CallCompositeBuilder().build()

        callComposite.addOnRemoteParticipantJoinedEventHandler {
            RawOutgoingVideoStreamFeature().startVideo(this, callComposite.callObject)
        }

        callComposite.launch(this, remoteOptions)*/
    }

    private fun fetchToken(): String? {
        return BuildConfig.ACS_TOKEN
    }
}
