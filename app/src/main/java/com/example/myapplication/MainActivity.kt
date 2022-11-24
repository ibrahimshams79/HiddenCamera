package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture.withOutput
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.cameraState
import com.example.myapplication.ui.theme.MeditationUIYouTubeTheme
import com.google.android.material.internal.ContextUtils.getActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onClickRequestPermission()
        setContent {
            MeditationUIYouTubeTheme {
                HomeScreen()

            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ", "Granted")
            } else {
                Log.i("Permission: ", "Denied")
            }
        }

    fun onClickRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("permission", "Previously Granted")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("permission", "Show Permission Dialogue")
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED -> {
                Log.i("permission", "Previously Granted")
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> Log.i("permission", "Show Permission Dialogue")
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }
}


@SuppressLint("RestrictedApi")
@Composable
fun camera(whichCamera: Int, cameraState: String): VideoCapture<Recorder> {
    val recorder = Recorder.Builder().build()
    var videoCapture: VideoCapture<Recorder> = withOutput(recorder)
    val lifecycleowner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.camPreview
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    AndroidView(factory = { previewView }) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            if (whichCamera == 1) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            } else if (whichCamera == 2) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            } else if (whichCamera == 3) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            } else if (whichCamera == 4) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }

            val faceAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }

            val selector = QualitySelector
                .from(
                    Quality.UHD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )

            fun getResolutions(selector:CameraSelector,
                               provider:ProcessCameraProvider
            ): Map<Quality, Size> {
                return selector.filter(provider.availableCameraInfos).firstOrNull()
                    ?.let { camInfo ->
                        QualitySelector.getSupportedQualities(camInfo)
                            .associateWith { quality ->
                                QualitySelector.getResolution(camInfo, quality)!!
                            }
                    } ?: emptyMap()
            }

            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(selector)
                .build()

            // Create the VideoCapture UseCase and make it available to use
            // in the other part of the application.
            videoCapture = withOutput(recorder)

//            try {
//                cameraProvider.unbindAll()
//
//                cameraProvider.bindToLifecycle(
//                    lifecycleowner,
//                    cameraSelector,
//                    preview,
//                    faceAnalysis,
//                    videoCapture
//                )
//
//            } catch (e: Exception) {
//                Log.e("Exception", "Message: ${e.localizedMessage}")
//            }
            try {
                cameraProvider.bindToLifecycle(
                    lifecycleowner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    videoCapture,
                    preview
                )
            } catch (exc: Exception) {
                // we are on the main thread, let's reset the controls on the UI.
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    return videoCapture
}

@SuppressLint("RestrictedApi")
fun captureVideo(videoCapture: VideoCapture<Recorder>, context: Context) {
    val appDirectory = File(Environment.getExternalStorageDirectory().path + "/Hidden Camera")

    if (!appDirectory.exists()) {
        appDirectory.mkdirs()
    }

    val date = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
    val currentDate = date.format(Date())
    val videoFileName = appDirectory.absolutePath + "/" + currentDate + ".mp4"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "/Hidden Camera")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions
        .Builder(
            getApplicationContext(context).getContentResolver(),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        .setContentValues(contentValues)
        .build()

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            context as Activity,
            Manifest.permission.RECORD_AUDIO)
    }

    var recording: Recording? = null

    val recordingListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                val msg = "Capture Started"
                Toast.makeText(getActivity(context), msg, Toast.LENGTH_SHORT)
                    .show()
                // update app internal recording state

            }
            is VideoRecordEvent.Finalize -> {
                val msg = if (!event.hasError()) {
                    // update app internal state


                    "Video capture succeeded: ${event.outputResults.outputUri}"

                } else {
                    // update app state when the capture failed.
                    recording?.close()
                    recording = null

                    "Video capture ends with error: ${event.error}"
                }
                Toast.makeText(getActivity(context), msg, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    if (cameraState == "Start") {
        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context), recordingListener)
        cameraState="Stop"
    } else {
        recording?.stop()
        cameraState="Start"
    }
}







