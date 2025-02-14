package com.teammeditalk.medicalconnect

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.teammeditalk.medicalconnect.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture

    private lateinit var cameraExecutor: ExecutorService

    // 텍스트 인식 시작
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    // 권한 확인
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast
                    .makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT,
                    ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext,
                it,
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        // 여기를 바인딩으로 안바꿔줘서 라이브러리가 적용 안된 거였다!!
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 입력 이미지 준비
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS,
            )
        }
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val name =
            SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
                .format(System.currentTimeMillis())

        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        Timber.d("image saved :${outputFileResults.savedUri}")
                        if (outputFileResults.savedUri != null) {
                            getImage(outputFileResults.savedUri!!)
                        } else {
                            Timber.d("save Uri is null")
                        }
                    } catch (e: Exception) {
                        Timber.e("URI 생성 실패: ${e.message}")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e("Photo capture failed: ${exception.message}")
                }
            },
        )
    }

    private fun getImage(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizeText(image)
            } catch (e: Exception) {
                Timber.d("이미지 처리 실패 :${e.message}")
            }
        }
    }

    private fun recognizeText(image: InputImage) {
        lifecycleScope.launch(Dispatchers.Main) {
            recognizer
                .process(image)
                .addOnSuccessListener {
                    Timber.d("success recognize :$it")
                }.addOnFailureListener {
                    Timber.d("failed recognize :${it.message}")
                }
        }
    }

    private fun captureVideo() {}

    private fun startCamera() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                    val preview =
                        Preview
                            .Builder()
                            .build()
                            .also {
                                it.surfaceProvider = viewBinding.viewFinder.surfaceProvider
                            }

                    imageCapture =
                        ImageCapture
                            .Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 지연 시간 최소화
                            .setTargetRotation(viewBinding.viewFinder.display.rotation)
                            .build()

                    val imageAnalyzer =
                        ImageAnalysis
                            .Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                    val cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            this@MainActivity,
                            cameraSelector,
                            preview,
                            imageCapture,
                        )
                    } catch (e: Exception) {
                        Timber.d("use case binding failed :$e")
                    }
                }, ContextCompat.getMainExecutor(this@MainActivity))
            } catch (e: TimeoutException) {
                Timber.d("Failed to start camera timeout :${e.message}")
            } catch (e: Exception) {
                Timber.d("Failed to start camera :${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onStop() {
        super.onStop()
        recognizer.close()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
