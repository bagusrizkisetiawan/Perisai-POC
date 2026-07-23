package id.co.alphanusa.perisaipoc.stream

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.utils.gl.AspectRatioMode
import com.pedro.library.base.recording.RecordController
import com.pedro.library.rtmp.RtmpStream
import com.pedro.library.util.sources.audio.MicrophoneSource
import id.co.alphanusa.perisaipoc.core.util.Constants

/**
 * Membungkus pipeline streaming berbasis [RtmpStream] (arsitektur baru
 * RootEncoder) dengan **CameraX** sebagai sumber video lewat [CameraXSource].
 *
 * CameraX melakukan capture (AF/exposure/resolusi tajam) + flip kamera,
 * RootEncoder menangani preview di layar + encode H.264 + RTMP + recording +
 * take photo. Objek ini dimiliki Activity karena preview terikat [SurfaceView].
 * Event koneksi diteruskan ke [listener].
 */
class CameraStreamController(
    context: Context,
    private val listener: Listener,
) : ConnectChecker {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "CameraStream"
    }

    /** Callback status koneksi RTMP (diteruskan dari [ConnectChecker]). */
    interface Listener {
        fun onConnectionStarted()
        fun onConnectionSuccess()
        fun onConnectionFailed(reason: String)
        fun onDisconnected()
        fun onAuthError()
        fun onAuthSuccess()
    }

    private val cameraXSource = CameraXSource(context.applicationContext)
    private val stream = RtmpStream(
        context.applicationContext,
        this,
        cameraXSource,
        MicrophoneSource(),
    ).apply {
        // Adjust = jaga rasio (tidak gepeng). Fill akan men-stretch.
        getGlInterface().setAspectRatioMode(AspectRatioMode.Adjust)
    }

    private var prepared = false
    private var streaming = false
    private var recording = false
    private var previewing = false

    val isStreaming: Boolean get() = streaming
    val isRecording: Boolean get() = recording
    val isOnPreview: Boolean get() = previewing

    init {
        // Siapkan encoder di awal agar dimensi (720p) ter-set ke CameraXSource
        // SEBELUM preview dimulai — kalau tidak, dimensi 0 → preview hitam.
        prepared = prepareEncoders()
        Log.d(TAG, "prepareEncoders di init: $prepared")
    }

    /** Mulai / lanjut menampilkan preview pada [surfaceView]. */
    fun onSurfaceAvailable(surfaceView: SurfaceView) {
        if (!previewing) {
            stream.startPreview(surfaceView)
            previewing = true
        }
    }

    /** Preview hilang (surface di-destroy). Encode tetap jalan lewat lifecycle CameraX. */
    fun onSurfaceDestroyed() {
        if (previewing) {
            stream.stopPreview()
            previewing = false
        }
    }

    fun startStream(url: String): Boolean {
        if (!prepared && !prepareEncoders().also { prepared = it }) return false
        stream.startStream(url)
        streaming = true
        return true
    }

    fun stopStream() {
        stream.stopStream()
        streaming = false
    }

    fun startRecord(path: String): Boolean {
        if (!prepared && !prepareEncoders().also { prepared = it }) return false
        stream.startRecord(
            path,
            object : RecordController.Listener {
                override fun onStatusChange(status: RecordController.Status) {}
            },
        )
        recording = true
        return true
    }

    fun stopRecord() {
        stream.stopRecord()
        recording = false
    }

    fun takePhoto(onBitmap: (Bitmap) -> Unit): Boolean {
        if (!previewing) return false
        stream.getGlInterface().takePhoto { bitmap -> onBitmap(bitmap) }
        return true
    }

    /** Ganti kamera depan/belakang (berfungsi meski sedang streaming/record). */
    fun switchCamera() = cameraXSource.switchCamera()

    val isFrontCamera: Boolean get() = cameraXSource.isFront

    fun release() {
        if (recording) stream.stopRecord()
        if (streaming) stream.stopStream()
        if (previewing) stream.stopPreview()
        stream.release()
        cameraXSource.release()
        streaming = false
        recording = false
        previewing = false
    }

    private fun prepareEncoders(): Boolean {
        val rotation = CameraHelper.getCameraOrientation(appContext)
        val video = try {
            stream.prepareVideo(
                Constants.Stream.WIDTH,
                Constants.Stream.HEIGHT,
                Constants.Stream.VIDEO_BITRATE,
                Constants.Stream.FPS,
                Constants.Stream.I_FRAME_INTERVAL_SEC,
                rotation,
            )
        } catch (e: Exception) {
            Log.e(TAG, "prepareVideo gagal", e)
            false
        }
        val audio = try {
            stream.prepareAudio(
                Constants.Stream.AUDIO_BITRATE,
                true,
                Constants.Stream.SAMPLE_RATE,
            )
        } catch (e: Exception) {
            Log.e(TAG, "prepareAudio gagal", e)
            false
        }
        Log.d(TAG, "prepareEncoders video=$video audio=$audio rotation=$rotation")
        return video && audio
    }

    // ── ConnectChecker → diteruskan ke listener ─────────────────────────────
    override fun onConnectionStarted(url: String) = listener.onConnectionStarted()

    override fun onConnectionSuccess() = listener.onConnectionSuccess()

    override fun onConnectionFailed(reason: String) {
        stream.stopStream()
        streaming = false
        listener.onConnectionFailed(reason)
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() = listener.onDisconnected()

    override fun onAuthError() = listener.onAuthError()

    override fun onAuthSuccess() = listener.onAuthSuccess()
}
