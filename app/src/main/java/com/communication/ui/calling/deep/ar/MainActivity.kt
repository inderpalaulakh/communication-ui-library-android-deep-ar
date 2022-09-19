package com.communication.ui.calling.deep.ar

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.azure.android.communication.common.CommunicationTokenCredential
import com.azure.android.communication.common.CommunicationTokenRefreshOptions
import com.azure.android.communication.ui.calling.CallComposite
import com.azure.android.communication.ui.calling.CallCompositeBuilder
import com.azure.android.communication.ui.calling.models.CallCompositeGroupCallLocator
import com.azure.android.communication.ui.calling.models.CallCompositeJoinLocator
import com.azure.android.communication.ui.calling.models.CallCompositeRemoteOptions
import com.communication.ui.calling.deep.ar.rawmedia.CameraHelper
import com.communication.ui.calling.deep.ar.rawmedia.DeepARHelper
import com.communication.ui.calling.deep.ar.rawmedia.RawOutgoingVideoStreamFeature
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "ACS_DEEPAR "
        var deepARHelper: DeepARHelper? = null
    }

    private lateinit var glSurfaceView: GLSurfaceView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener { l -> startCallComposite() }
        glSurfaceView = findViewById(R.id.glsurfaceview) as GLSurfaceView
        glSurfaceView.setRenderer( CubeRenderer(false));
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

    private fun startCallComposite() {

        val cameraHelper = CameraHelper(this)

        deepARHelper = DeepARHelper(this)
        deepARHelper?.startDeepAR()

        deepARHelper?.onImageProcessed = { image ->
            Log.d(TAG, image.timestamp.toString())

        }

        cameraHelper.onImageReceived = { image, mirroring ->
            Handler(Looper.getMainLooper()).post {
                deepARHelper?.processImage(image, mirroring )
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
