package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MeditationUIYouTubeTheme
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

      setContent {
          MeditationUIYouTubeTheme {
             // HomeScreen()
              Camera()
          }
      }
    }
}


@Composable
fun Camera(){
    val lifecycleowner = LocalLifecycleOwner.current
    val context = LocalContext.current

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
    
    AndroidView(factory = {previewView}, modifier = Modifier.fillMaxSize()){
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


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


