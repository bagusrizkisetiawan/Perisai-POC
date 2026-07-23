package id.co.alphanusa.perisaipoc.stream

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pedro.library.util.sources.video.VideoSource

/**
 * [VideoSource] RootEncoder yang mengambil frame dari **CameraX**.
 *
 * CameraX yang menangani autofocus, exposure, dan pemilihan resolusi (setajam
 * PreviewView pada scanner QR). Frame CameraX dirender ke [SurfaceTexture] milik
 * RootEncoder, lalu RootEncoder yang menampilkan preview + meng-encode + RTMP.
 *
 * Memiliki [LifecycleOwner] sendiri agar kamera tetap berjalan walau Activity
 * masuk background. Mendukung flip kamera depan/belakang lewat [switchCamera].
 */
class CameraXSource(private val context: Context) : VideoSource(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var currentSurfaceTexture: SurfaceTexture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var running = false

    /** true bila sedang memakai kamera depan. */
    val isFront: Boolean get() = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

    init {
        mainExecutor.execute { lifecycleRegistry.currentState = Lifecycle.State.CREATED }
    }

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean = true

    override fun start(surfaceTexture: SurfaceTexture) {
        this.currentSurfaceTexture = surfaceTexture
        mainExecutor.execute {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    val provider = future.get()
                    cameraProvider = provider
                    bindPreview(provider, surfaceTexture)
                    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                    running = true
                    Log.d(TAG, "CameraX dimulai (target ${width}x$height, front=$isFront)")
                } catch (e: Exception) {
                    Log.e(TAG, "CameraX gagal dimulai", e)
                }
            }, mainExecutor)
        }
    }

    private fun bindPreview(provider: ProcessCameraProvider, surfaceTexture: SurfaceTexture) {
        val preview = Preview.Builder()
            .setTargetResolution(Size(width, height))
            .build()
        preview.setSurfaceProvider(mainExecutor) { request ->
            val res = request.resolution
            surfaceTexture.setDefaultBufferSize(res.width, res.height)
            val surface = Surface(surfaceTexture)
            request.provideSurface(surface, mainExecutor) { surface.release() }
        }
        provider.unbindAll()
        provider.bindToLifecycle(this, cameraSelector, preview)
        this.preview = preview
    }

    /** Ganti antara kamera depan & belakang; rebind CameraX dengan selector baru. */
    fun switchCamera() {
        mainExecutor.execute {
            cameraSelector = if (isFront) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            val provider = cameraProvider ?: return@execute
            val st = currentSurfaceTexture ?: return@execute
            try {
                bindPreview(provider, st)
                Log.d(TAG, "Flip kamera → front=$isFront")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal flip kamera", e)
            }
        }
    }

    override fun stop() {
        mainExecutor.execute {
            cameraProvider?.unbindAll()
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            running = false
        }
    }

    override fun release() {
        mainExecutor.execute {
            cameraProvider?.unbindAll()
            cameraProvider = null
            preview = null
            currentSurfaceTexture = null
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            running = false
        }
    }

    override fun isRunning(): Boolean = running

    private companion object {
        const val TAG = "CameraXSource"
    }
}
