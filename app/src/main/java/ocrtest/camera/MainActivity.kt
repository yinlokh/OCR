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
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.io.ByteArrayOutputStream

/**
 * Written with sample from
 * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
 */
class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    var captureButton : Button? = null
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
        previewSurface?.getBitmap()?.compress(Bitmap.CompressFormat.PNG, 50, stream)
        var byteArray = stream.toByteArray()
        console?.append("\n byte array size = " + byteArray.size)
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
