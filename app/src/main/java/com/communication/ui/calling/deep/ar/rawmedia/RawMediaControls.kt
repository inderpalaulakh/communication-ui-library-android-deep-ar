package com.communication.ui.calling.deep.ar.rawmedia

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.communication.ui.calling.deep.ar.MainActivity
import com.communication.ui.calling.deep.ar.R

class RawMediaControls private constructor(
    private val context: Application,
) {
    companion object {
        lateinit var startRawMedia: Button
        lateinit var changeEffect: Button
        private var viewer: RawMediaControls? = null
        private var isInitialized = false
        const val DEFAULT_GRAVITY = Gravity.TOP or Gravity.START
        const val POSITION_X = 10
        const val POSITION_L = 10

        fun viewer(context: Application): RawMediaControls {
            if (viewer == null) {
                viewer = RawMediaControls(context)
            }
            return viewer!!
        }
    }

    private val linearLayout: LinearLayout =
        LayoutInflater.from(context).inflate(R.layout.raw_media_control_bar, null) as LinearLayout



    private val windowManager: WindowManager =
        linearLayout.context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

    fun display(frameCount: Int) {

    }

    fun show() {

        startRawMedia =
            linearLayout.findViewById<Button>(R.id.startRawMedia)

        changeEffect =
            linearLayout.findViewById<Button>(R.id.changeEffect)

        if (drawOverlaysPermission(context) && linearLayout.visibility != View.VISIBLE) {
            if (!isInitialized) {
                isInitialized = true
                init()
            }
            showControls()
            linearLayout.visibility = View.VISIBLE
        }
    }

    fun hide() {
        if (linearLayout.visibility == View.VISIBLE) {
            linearLayout.visibility = View.GONE

        }
    }

    private fun init() {
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.RGBA_8888
        )
        params.gravity = DEFAULT_GRAVITY
        params.x = POSITION_X
        params.y = POSITION_L
        windowManager.addView(linearLayout, params)
        linearLayout.setOnTouchListener(MovingTouchListener(params, windowManager))
        linearLayout.isHapticFeedbackEnabled = false
        linearLayout.visibility = View.GONE
    }

    private fun drawOverlaysPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)
            .also {
                if (!it) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            }
    }

    private fun showControls() {

    }

    private class MovingTouchListener(
        private val params: WindowManager.LayoutParams,
        private val windowManager: WindowManager,
    ) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        override fun onTouch(
            v: View,
            event: MotionEvent,
        ): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(v, params)
                }
            }
            return false
        }
    }
}
