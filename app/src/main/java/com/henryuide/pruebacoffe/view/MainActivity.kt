package com.henryuide.pruebacoffe.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.henryuide.pruebacoffe.GoogleAuthHelper
import com.henryuide.pruebacoffe.R
import com.henryuide.pruebacoffe.databinding.ActivityMainBinding
import com.henryuide.pruebacoffe.utils.Draw


// Constants
private const val MAX_RESULT_DISPLAY = 3 // Maximum number of results displayed
private const val TAG = "TFL Classify" // Name for logging
private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // permission needed

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var googleAuthHelper: GoogleAuthHelper
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main) // data binding
        if (allPermissionsGranted()) {
            startCamera()
            //autenticacion
           // googleAuthHelper = GoogleAuthHelper(this)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // Exit the app if permission is not granted
                // Best practice is to explain and offer a chance to re-request but this is out of
                // scope in this sample. More details:
                // https://developer.android.com/training/permissions/usage-notes
                Toast.makeText(
                    this,
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            //Bind camera provider
            bindPreview(cameraProvider = cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        //val localModel = LocalModel.Builder().setAssetFilePath("plagas_detector_v1.tflite").build()
        val localModel = LocalModel.Builder().setAssetFilePath("enfermedades_cafe.tflite").build()

        val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()

        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()

        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val point = Point()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(point.x, point.y))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image
            if (image != null) {

                val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
                objectDetector
                    .process(inputImage)
                    .addOnFailureListener {
                        Log.e(TAG, "Erroraesc - ${it.message} ")
                        imageProxy.close()
                    }.addOnSuccessListener { objects ->
                        debugPrint(objects)
                        for (it in objects) {
                            println("Current - Label: ${it.labels.firstOrNull()?.text}, confidence: ${it.labels.firstOrNull()?.confidence}")
                            val confidence = when {
                                it.labels.firstOrNull()?.confidence == null -> {
                                    "Undefined"
                                }
                                it.labels.firstOrNull()?.confidence != null -> {
                                    if (it.labels.firstOrNull()?.confidence!! > 0.59f)
                                        it.labels.firstOrNull()?.text
                                    else
                                        "Undefined"
                                }
                                else -> "Undefined"
                            }
                            if (binding.layout.childCount > 1) binding.layout.removeViewAt(1)
                            val element = Draw(this, it.boundingBox, confidence!!)
                            binding.layout.addView(element, 1)
                        }
                        imageProxy.close()
                    }
            }
        }

            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
            )
        }

    private fun debugPrint(detectedObjects: List<DetectedObject>) {
        detectedObjects.forEachIndexed { index, detectedObject ->
            val box = detectedObject.boundingBox
            Log.d(TAG, "Detected object: $index")
            Log.d(TAG, " trackingId: ${detectedObject.trackingId}")
            Log.d(TAG, " boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            detectedObject.labels.forEach {
                Log.d(TAG, " categories: ${it.text}")
                Log.d(TAG, " confidence: ${it.confidence}")
            }
        }
    }
}