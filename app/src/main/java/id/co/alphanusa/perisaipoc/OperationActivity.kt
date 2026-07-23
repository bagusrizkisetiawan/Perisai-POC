package id.co.alphanusa.perisaipoc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.media.MediaStoreSaver
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.model.BatteryInfo
import id.co.alphanusa.perisaipoc.domain.model.BatteryStatus
import id.co.alphanusa.perisaipoc.domain.model.GpsSignalLevel
import id.co.alphanusa.perisaipoc.domain.model.PocTelemetry
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import id.co.alphanusa.perisaipoc.domain.usecase.BuildStreamUrlUseCase
import id.co.alphanusa.perisaipoc.realtime.CentrifugoClientManager
import id.co.alphanusa.perisaipoc.realtime.CentrifugoConnectionState
import id.co.alphanusa.perisaipoc.stream.CameraStreamController
import id.co.alphanusa.perisaipoc.ui.screens.operation.OperationScreen
import id.co.alphanusa.perisaipoc.ui.theme.POCHuaweiStreamTheme
import id.co.alphanusa.perisaipoc.utils.HuaweiLocationHelper
import id.co.alphanusa.perisaipoc.utils.ILocationHelper
import id.co.alphanusa.perisaipoc.utils.NativeLocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class OperationActivity : ComponentActivity(), CameraStreamController.Listener {

    // =========================================================================
    // 1. PROPERTIES & STATE VARIABLES
    // =========================================================================

    // Services & Managers
    @Inject lateinit var centrifugoManager: CentrifugoClientManager

    @Inject lateinit var buildStreamUrl: BuildStreamUrlUseCase

    @Inject lateinit var settingsRepository: SettingsRepository
    private lateinit var locationHelper: ILocationHelper

    // Camera & Stream (CameraX + RootEncoder RtmpStream)
    private val mediaStoreSaver by lazy { MediaStoreSaver(this) }
    private val cameraController: CameraStreamController by lazy {
        CameraStreamController(this, this)
    }
    private var rtmpUrl: String? = null
    private var recordFilePath: String? = null
    private var isStreaming by mutableStateOf(false)
    private var isRecording by mutableStateOf(false)
    var isFrontCamera by mutableStateOf(false)

    // Device States (Location, Permissions, Sensors)
    private var currentLocation by mutableStateOf<Location?>(null)
    private var hasPermissions by mutableStateOf(false)
    private var pitch by mutableFloatStateOf(0f)
    private var roll by mutableFloatStateOf(0f)
    private var yaw by mutableFloatStateOf(0f)
    private var batteryLevel by mutableIntStateOf(0)

    // LiveKit
    private var livekitShouldConnect by mutableStateOf(false)
    private var livekitIsMuted by mutableStateOf(true)
    private var livekitIsSpeakerMuted by mutableStateOf(false)

    // =========================================================================
    // 2. ANDROID LIFECYCLE
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Verifikasi izin di awal
        hasPermissions = checkRequiredPermissions()
        if (!hasPermissions) {
            Toast.makeText(this, "Akses ditolak karena izin belum lengkap!", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        centrifugoManager.startConnection()

        locationHelper = if (isHuaweiDevice()) {
            HuaweiLocationHelper(this)
        } else {
            NativeLocationHelper(this)
        }

        locationHelper.getLastLocation { location ->
            if (location != null) {
                currentLocation = location
            }
        }

        locationHelper.startLocationUpdates { location ->
            currentLocation = location
            sendToCentrifugo()
        }

        fetchRtmpUrl()

        // OsmDroid
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext),
        )
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val connectionState by centrifugoManager.connectionState.collectAsState()
            val context = LocalContext.current

            // 1. POLLING LOKASI SECARA LIVE (Update setiap 2 detik)
            LaunchedEffect(Unit) {
                while (true) {
                    batteryLevel = getBatteryPercentage()
                    delay(Constants.BATTERY_POLL_INTERVAL_MS)
                }
            }

            // 2. LISTENER SENSOR HP (PITCH, ROLL, YAW) SECARA LIVE
            DisposableEffect(Unit) {
                val sensorManager =
                    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        event?.let {
                            if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                                val rotationMatrix = FloatArray(9)
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                                val orientationValues = FloatArray(3)
                                SensorManager.getOrientation(rotationMatrix, orientationValues)

                                yaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                                pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                                roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                                sendToCentrifugo()
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                sensorManager.registerListener(
                    listener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI,
                )

                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }

            LaunchedEffect(connectionState) {
                when (connectionState) {
                    CentrifugoConnectionState.ERROR,
                    CentrifugoConnectionState.DISCONNECTED,
                    -> {
                        centrifugoManager.startConnection()
                    }

                    CentrifugoConnectionState.CONNECTING -> {
                        delay(50000) // tunggu 10 detik
                        // cek lagi, kalau masih CONNECTING berarti timeout
                        if (centrifugoManager.connectionState.value == CentrifugoConnectionState.CONNECTING) {
                            centrifugoManager.stopConnection()
                            delay(1000)
                            centrifugoManager.startConnection()
                        }
                    }

                    else -> Unit
                }
            }

            // 3. TAMPILKAN UI
            POCHuaweiStreamTheme {
                OperationScreen(
                    location = currentLocation,
                    yaw = yaw,
                    connectionState = connectionState,
                    isStreaming = isStreaming,
                    hasPermissions = hasPermissions,
                    isFrontCamera = isFrontCamera,
                    onStartStream = { startRtmpStream() },
                    onStopStream = { stopRtmpStream() },
                    onSwitchCamera = { switchCamera() },
                    livekitShouldConnect = livekitShouldConnect,
                    livekitIsMuted = livekitIsMuted,
                    livekitIsSpeakerMuted = livekitIsSpeakerMuted,
                    onLivekitConnect = { livekitShouldConnect = true },
                    onLivekitDisconnect = {
                        livekitShouldConnect = false
                        livekitIsMuted = true
                        livekitIsSpeakerMuted = false

                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        am.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxMusic / 2,
                            0,
                        ) // restore ke 50%
                    },
                    onLivekitMuteToggle = { livekitIsMuted = !livekitIsMuted },
                    onLivekitSpeakerToggle = { livekitIsSpeakerMuted = !livekitIsSpeakerMuted },
                    cameraController = cameraController,
                    isRecording = isRecording,
                    livekitUrl = settingsRepository.getConfig().livekitUrl,
                    onToggleRecording = {
                        if (isRecording) stopRecording() else startRecording()
                    },
                    onTakePhoto = { takePhoto() },
                    onLogout = { returnToHome() },
                )
            }
        }
    }

    fun ComponentActivity.requireNeededPermissions(onPermissionsGranted: (() -> Unit)? = null) {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { grants ->
                // Check if any permissions weren't granted.
                for (grant in grants.entries) {
                    if (!grant.value) {
                        Toast.makeText(
                            this,
                            "Missing permission: ${grant.key}",
                            Toast.LENGTH_SHORT,
                        )
                            .show()
                    }
                }

                // If all granted, notify if needed.
                if (onPermissionsGranted != null && grants.all { it.value }) {
                    onPermissionsGranted()
                }
            }

        val neededPermissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it,
                ) == PackageManager.PERMISSION_DENIED
            }
            .toTypedArray()

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions)
        } else {
            onPermissionsGranted?.invoke()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraController.release()
        locationHelper.stopLocationUpdates()
    }

    // =========================================================================
    // 3. CORE LOGIC (Network, Streaming, Centrifugo)
    // =========================================================================

    /** Menyiapkan URL RTMP tujuan streaming lewat use case. */
    private fun fetchRtmpUrl() {
        lifecycleScope.launch {
            when (val result = buildStreamUrl()) {
                is AppResult.Success -> {
                    rtmpUrl = result.data
                    Log.d("RTMP_URL", "URL streaming siap")
                }

                is AppResult.Failure -> showToastOnMain(result.message)
            }
        }
    }

    private fun startRtmpStream() {
        if (isStreaming) return

        val urlToStream = rtmpUrl
        if (urlToStream == null) {
            Toast.makeText(this, "URL RTMP belum siap, silakan tunggu...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (cameraController.startStream(urlToStream)) {
            isStreaming = true
        } else {
            Toast.makeText(this, "Gagal menyiapkan video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRtmpStream() {
        if (!isStreaming) return
        cameraController.stopStream()
        isStreaming = false
        Toast.makeText(this, "RTMP Stream Dihentikan", Toast.LENGTH_SHORT).show()
    }

    // ── Record video ke galeri ──────────────────────────────────────────────
    private fun startRecording() {
        if (isRecording) return
        val file = mediaStoreSaver.newRecordingFile()
        if (cameraController.startRecord(file.absolutePath)) {
            recordFilePath = file.absolutePath
            isRecording = true
            Toast.makeText(this, "Mulai merekam...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Gagal memulai rekaman", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        cameraController.stopRecord()
        isRecording = false
        val path = recordFilePath ?: return
        lifecycleScope.launch {
            mediaStoreSaver.saveVideoToGallery(File(path))
                .onSuccess { showToastOnMain("Video tersimpan (${MediaStoreSaver.VIDEO_ALBUM})") }
                .onFailure { showToastOnMain("Gagal menyimpan video: ${it.message}") }
        }
    }

    // ── Ambil foto ke galeri ────────────────────────────────────────────────
    private fun takePhoto() {
        val started = cameraController.takePhoto { bitmap ->
            lifecycleScope.launch {
                mediaStoreSaver.savePhotoToGallery(bitmap)
                    .onSuccess { showToastOnMain("Foto tersimpan (${MediaStoreSaver.PHOTO_ALBUM})") }
                    .onFailure { showToastOnMain("Gagal menyimpan foto: ${it.message}") }
            }
        }
        if (!started) {
            Toast.makeText(this, "Kamera belum siap", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToCentrifugo() {
        val loc = currentLocation
        if (centrifugoManager.connectionState.value == CentrifugoConnectionState.CONNECTED) {
            val telemetry = PocTelemetry(
                pitch = pitch.toDouble(),
                roll = roll.toDouble(),
                yaw = yaw.toDouble(),
                latitude = loc?.latitude ?: 0.0,
                longitude = loc?.longitude ?: 0.0,
                altitude = loc?.altitude ?: 0.0,
                homeLatitude = loc?.latitude ?: 0.0,
                homeLongitude = loc?.longitude ?: 0.0,
                gpsSatelliteCount = 0,
                gpsSignalLevel =
                if (loc != null) GpsSignalLevel.GOOD else GpsSignalLevel.NO_GPS,
                battery = BatteryInfo(
                    percentage = batteryLevel,
                    voltage = 0f,
                    status = BatteryStatus.fromPercentage(batteryLevel),
                ),
                timestamp = System.currentTimeMillis(),
            )
            centrifugoManager.updateTelemetry(telemetry)
        }
    }

    // =========================================================================
    // 4. HARDWARE & PERMISSION HELPERS
    // =========================================================================

    private fun checkRequiredPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val audio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return camera && audio && location
    }

    private fun getBatteryPercentage(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun switchCamera() {
        cameraController.switchCamera()
        isFrontCamera = cameraController.isFrontCamera
    }

    private fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("huawei") || brand.contains("huawei")
    }

    // =========================================================================
    // 5. UTILITY FUNCTIONS
    // =========================================================================

    /** Kembali ke layar utama (dipakai saat logout dari status bar). */
    private fun returnToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private suspend fun showToastOnMain(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@OperationActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // 6. CONNECT CHECKER INTERFACE IMPLEMENTATION
    // =========================================================================

    override fun onConnectionStarted() = runOnUiThread {
        Toast.makeText(this, "Memulai koneksi...", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionSuccess() = runOnUiThread {
        Toast.makeText(this, "Berhasil terhubung ke Server RTMP", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionFailed(reason: String) = runOnUiThread {
        Toast.makeText(this, "Gagal terhubung: $reason", Toast.LENGTH_LONG).show()
        isStreaming = false
    }

    override fun onDisconnected() = runOnUiThread {
        Toast.makeText(this, "Terputus dari Server RTMP", Toast.LENGTH_SHORT).show()
        isStreaming = false
    }

    override fun onAuthError() = runOnUiThread {
        Toast.makeText(this, "Error Autentikasi RTMP", Toast.LENGTH_SHORT).show()
    }

    override fun onAuthSuccess() = runOnUiThread {
        Toast.makeText(this, "Autentikasi RTMP Sukses", Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // 7. COMPOSE UI COMPONENTS
    // =========================================================================
}
