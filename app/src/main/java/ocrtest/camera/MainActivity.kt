package ocrtest.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CaptureRequest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.common.collect.ImmutableList
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ocrtest.camera.models.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

/**
 * Written with sample from
 * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
 */
class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    val GOOGLE_VISION_BASE_URL = "https://vision.googleapis.com"
    val GOOGLE_VISION_API_KEY = "" 

    var captureButton : Button? = null
    var cloudVisionService : CloudVisionService? = null
    var console : TextView? = null
    var previewSurface : TextureView? = null
    var cameraManager : CameraManager? = null
    var camera : CameraDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        captureButton = findViewById(R.id.capture_button)
        console = findViewById(R.id.console)
        previewSurface = findViewById(R.id.preview_surface)
        previewSurface?.surfaceTextureListener = this
        console?.append("\nSetting click listener")
        captureButton?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                console?.append("\nAttempting capture")
                capture()
            }
        })
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var okHttpClient = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()
        var retrofitBuilder = Retrofit.Builder()
                .baseUrl(GOOGLE_VISION_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
        cloudVisionService = retrofitBuilder.create(CloudVisionService::class.java)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        closeCamera()
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        cameraOpen()
    }

    fun cameraOpen() {
        if (cameraManager == null) {
            return
        }

        try {
            console?.append("\nNumber of cameras: " + cameraManager!!.cameraIdList!!.size)
            for (cameraId in cameraManager!!.cameraIdList) {
                val characteristic = cameraManager!!.getCameraCharacteristics(cameraId)
                if (characteristic.get(LENS_FACING).equals(LENS_FACING_BACK)) {
                    cameraOpen(cameraId)
                    return
                }
            }
        } catch (e: Exception) {}
    }

    fun cameraOpen(id : String) : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            console?.append("\nMissing Camera Permission, Cannot open camera.")
            return false
        }
        cameraManager!!.openCamera(id, CameraCallback(this), null)
        return true
    }

    fun cameraOpened(camera: CameraDevice?) {
        this.camera = camera
        var surfaces = ArrayList<Surface>()
        var surface : Surface? = null
        if (previewSurface != null) {
            surface = Surface(previewSurface?.surfaceTexture)
            surfaces.add(surface)
        }

        console?.append("\nStarting preview")
        previewSurface?.surfaceTexture?.setDefaultBufferSize(640, 480)
        var previewRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        if (surface != null) {
            previewRequestBuilder?.addTarget(surface as? Surface)
        }

        console?.append("\nCreating capture session")
        camera?.createCaptureSession(surfaces, StateCallback(this, previewRequestBuilder), null)
    }

    fun cameraDisconnected() { }

    fun captureSessionConfigured(
            session: CameraCaptureSession?,
            previewRequestBuilder: CaptureRequest.Builder?) {
        var request = previewRequestBuilder?.build()
        if (request != null) {
            session?.setRepeatingRequest(
                    request,
                    object : CameraCaptureSession.CaptureCallback() {},
                    null)
        }
    }

    fun closeCamera() {
        camera?.close()
    }

    fun capture() {
        var stream = ByteArrayOutputStream()
        previewSurface?.getBitmap()?.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        var byteArray = stream.toByteArray()
        console?.append("\n byte array size = " + byteArray.size)
        retrofitCloudVisionCall(byteArray)
    }

    fun retrofitCloudVisionCall(image: ByteArray) {
        val request = CloudVisionRequest(
                CloudVisionImage(Base64.encodeToString(image, 0)),
                CloudVisionFeatures("TEXT_DETECTION", "10")
        )

        val requestStartTime = System.currentTimeMillis()
        val requests = CloudVisionRequests(
                ImmutableList.builder<CloudVisionRequest>().add(request).build())
        cloudVisionService?.annotate(GOOGLE_VISION_API_KEY, requests)
                ?.subscribeOn(Schedulers.computation())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnError({throwable -> Log.e("yinlokh", Log.getStackTraceString(throwable))})
                ?.subscribe({response -> parseResponse(response, requestStartTime)})
    }

    fun parseResponse(response: CloudVisionResponses, responseStartTimeMs: Long) : Question? {
        console?.setText("")
        console?.append("\nResults(rtt " + (System.currentTimeMillis() - responseStartTimeMs) + "ms): ")

        // first annotation is the most confident one
        val question = response.responses.get(0)?.textAnnotations?.get(0)?.description ?: ""
        val lines = question.split('\n')
        if (lines.size >= 3) {
            val answers = lines.subList(lines.size - 3 - 1, lines.size)
            var questionBuilder = StringBuilder()
            for (line in lines.subList(0, lines.size - 3 - 1)) {
                questionBuilder.append(line)
                questionBuilder.append(" ")
            }
            val question = questionBuilder.toString()
            console?.append("\nQuestion: " + question)
            console?.append("\nAnswers: ")
            console?.append("\n1 - " + answers[0])
            console?.append("\n2 - " + answers[1])
            console?.append("\n3 - " + answers[2])
            return Question(question, answers)
        }
        return null
    }

    class CameraCallback(var activity : MainActivity) : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice?) {
            activity.cameraOpened(camera)
        }

        override fun onDisconnected(camera: CameraDevice?) {
            activity.cameraDisconnected()
        }

        override fun onError(camera: CameraDevice?, error: Int) { }
    }

    class StateCallback(var activity: MainActivity, var requestBuilder : CaptureRequest.Builder?)
        : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession?) {
            activity.captureSessionConfigured(session, requestBuilder)
        }

        override fun onConfigureFailed(session: CameraCaptureSession?) {
        }

    }
}
