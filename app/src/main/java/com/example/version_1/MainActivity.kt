package com.example.version_1

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.version_1.databinding.ActivityMainBinding
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    private var imageId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        ortSession = ortEnv.createSession(readModel(), sessionOptions)

        binding.takePhotoButton.setOnClickListener { binding.imageView.setImageBitmap(
            BitmapFactory.decodeStream(newPhoto())
        ) }
        binding.detectTextButton.setOnClickListener {
            try {
                performTextDetection(ortSession)
                Toast.makeText(baseContext, "TextDetection performed!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Exception caught when perform TextDetection", e)
                Toast.makeText(baseContext, "Failed to perform TextDetection", Toast.LENGTH_SHORT).show()
                binding.textResult.text = "$e"
            }
        }
    }

    private fun newPhoto(): InputStream {
        imageId = imageId.xor(1)
        return assets.open("test_${imageId}.jpg")
    }

    private fun readPhoto(): InputStream {
        return assets.open("test_${imageId}.jpg")
    }

    private fun readModel(): ByteArray {
        val modelID = R.raw.yolo
        return resources.openRawResource(modelID).readBytes()
    }

    private fun performTextDetection(ortSession: OrtSession) {
        var textDetector = TextDetector()
        var imageStream = readPhoto()
        binding.imageView.setImageBitmap(
            BitmapFactory.decodeStream(imageStream)
        )
        imageStream.reset()
        var result = textDetector.detect(imageStream, ortEnv, ortSession)
        updateUI(result)
    }

    private fun updateUI(result: Result) {
        val mutableBitmap: Bitmap = result.outputBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawBitmap(mutableBitmap, 0.0f, 0.0f, paint)
        canvas.drawText("Text Result", 0.0f, 0.0f, paint)
        binding.imageView.setImageBitmap(mutableBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        ortEnv.close()
        ortSession.close()
    }

    // Start Permissions Segment
    // Request Permission using Activity Result Launcher
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    // Check permissions
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Initialize Activity Result Launcher
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                // Start the camera
            }
        }
    // End Permissions Segment

    companion object {
        private const val TAG = "Version_1"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}