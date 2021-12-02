package com.b57.cameraxsample

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class TestTakePhotoActivity : BaseGlassActivity() {


    private lateinit var imageCapture: ImageCapture
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var takePhoto: PreviewView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_photo)
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0x99)
        } else {
            startCamera()
        }
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture?): Boolean {
        if (gesture == GlassGestureDetector.Gesture.TAP) {
            takePhoto()
            return true
        }
        return super.onGesture(gesture)
    }

    private fun takePhoto() {
        val photoFile = File(
            filesDir,
            SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss", Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Log.d(TAG, "onError: ")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "onImageSaved: ")
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    handler.post {
                        Toast.makeText(this@TestTakePhotoActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, msg)
                }
            })
    }

    private fun startCamera() {
        takePhoto = findViewById(R.id.take_photo)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(takePhoto.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(3264, 2448))
                .setTargetRotation(takePhoto.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0x99
            && permissions[0] == Manifest.permission.CAMERA
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

}