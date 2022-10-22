package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.theme.MeditationUIYouTubeTheme
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MeditationUIYouTubeTheme {

                    val whichCamera = HomeScreen()
                    Camera(whichCamera)

                    onClickRequestPermission()


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
            ) ->  Log.i("permission", "Show Permission Dialogue")
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }
}
@Composable
fun Camera(whichCamera: Int){
    val lifecycleowner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val cameraProviderFuture = remember{
        ProcessCameraProvider.getInstance(context)
    }

    val previewView = remember{
        PreviewView(context).apply {
            id= R.id.campreview
        }
    }
    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }
    
    AndroidView(factory = {previewView}, modifier = Modifier
        .width(150.dp)
        .height(150.dp)){
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            if(whichCamera == 1){
             cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }else if (whichCamera == 2){
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }else if (whichCamera == 3){
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }else if (whichCamera == 4){
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }


            val faceAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor , FaceAnalyzer())
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleowner, cameraSelector, preview, faceAnalysis)
            }catch (e: Exception){
                Log.e("Exception", "Message: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}


