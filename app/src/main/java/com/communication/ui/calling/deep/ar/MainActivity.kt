package com.communication.ui.calling.deep.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.communication.ui.calling.deep.ar.rawmedia.DeepARHelper
import com.communication.ui.calling.deep.ar.rawmedia.RawOutgoingVideoStreamFeature
import java.util.*
import com.communication.ui.calling.deep.ar.BuildConfig


class MainActivity : AppCompatActivity() {

    companion object {
        var deepARHelper: DeepARHelper? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener { l -> startCallComposite() }
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
        deepARHelper = DeepARHelper(this)
        deepARHelper?.startDeepAR()

        val communicationTokenRefreshOptions =
            CommunicationTokenRefreshOptions({ fetchToken() }, true)
        val communicationTokenCredential =
            CommunicationTokenCredential(communicationTokenRefreshOptions)

        val locator: CallCompositeJoinLocator =
            CallCompositeGroupCallLocator(UUID.fromString(BuildConfig.GROUP_CALL_ID))
        val remoteOptions =
            CallCompositeRemoteOptions(locator, communicationTokenCredential, BuildConfig.DISPLAY_NAME)

        val callComposite: CallComposite = CallCompositeBuilder().build()

        callComposite.addOnRemoteParticipantJoinedEventHandler {
            RawOutgoingVideoStreamFeature().startVideo(this, callComposite.callObject)
        }

        callComposite.launch(this, remoteOptions)
    }

    private fun fetchToken(): String? {
        return BuildConfig.ACS_TOKEN
    }
}