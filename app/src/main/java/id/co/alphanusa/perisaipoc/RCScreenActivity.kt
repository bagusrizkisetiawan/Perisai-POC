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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import id.co.alphanusa.perisaipoc.core.media.MediaStoreSaver
import id.co.alphanusa.perisaipoc.data.local.AppSettingsManager
import id.co.alphanusa.perisaipoc.data.remote.api.ApiConfig
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.domain.model.BatteryData
import id.co.alphanusa.perisaipoc.domain.model.PocData
import id.co.alphanusa.perisaipoc.domain.model.getBatteryStatus
import id.co.alphanusa.perisaipoc.realtime.CentrifugoClientManager
import id.co.alphanusa.perisaipoc.realtime.CentrifugoConnectionState
import id.co.alphanusa.perisaipoc.stream.CameraStreamController
import id.co.alphanusa.perisaipoc.ui.components.AlertStream
import id.co.alphanusa.perisaipoc.ui.components.ConnectionStatusBar
import id.co.alphanusa.perisaipoc.ui.components.DialogCall
import id.co.alphanusa.perisaipoc.ui.components.OsmdroidMapView
import id.co.alphanusa.perisaipoc.ui.components.RCCameraPreview
import id.co.alphanusa.perisaipoc.ui.components.RCHoldToStopOverlay
import id.co.alphanusa.perisaipoc.ui.components.backgroundColor
import id.co.alphanusa.perisaipoc.ui.components.colorPrimary
import id.co.alphanusa.perisaipoc.ui.components.dangerColor
import id.co.alphanusa.perisaipoc.ui.components.successColor
import id.co.alphanusa.perisaipoc.ui.theme.POCHuaweiStreamTheme
import id.co.alphanusa.perisaipoc.ui.viewmodel.LivekitViewModel
import id.co.alphanusa.perisaipoc.ui.viewmodel.LivekitViewModelFactory
import id.co.alphanusa.perisaipoc.ui.viewmodel.UserViewModel
import id.co.alphanusa.perisaipoc.ui.viewmodel.UserViewModelFactory
import id.co.alphanusa.perisaipoc.utils.HuaweiLocationHelper
import id.co.alphanusa.perisaipoc.utils.ILocationHelper
import id.co.alphanusa.perisaipoc.utils.NativeLocationHelper
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.RemoteAudioTrack
import io.livekit.android.room.track.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.File

@AndroidEntryPoint
class RCScreenActivity : ComponentActivity(), CameraStreamController.Listener {

    // =========================================================================
    // 1. PROPERTIES & STATE VARIABLES
    // =========================================================================

    // Services & Managers
    private lateinit var centrifugoManager: CentrifugoClientManager
    private lateinit var locationHelper: ILocationHelper
    private val authManager: ApiConfig by lazy { ApiConfig.getInstance(context = this) }
    private val apiService: ApiService by lazy {
        authManager.apiService
    }

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

        centrifugoManager = CentrifugoClientManager.getInstance(this)
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

        //  Setup Service menggunakan ApiConfig
        val authManager = ApiConfig.getInstance(this)
        val livekitApiService = authManager.apiService
        val userApiService = authManager.apiService

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
                    kotlinx.coroutines.delay(2000)
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
                SimpleCameraScreen(
                    location = currentLocation,
                    yaw = yaw,
                    connectionState = connectionState,
                    isStreaming = isStreaming,
                    hasPermissions = hasPermissions,
                    isFrontCamera = isFrontCamera,
                    onStartStream = { startRtmpStream() },
                    onStopStream = { stopRtmpStream() },
                    onSwitchCamera = { switchCamera() },
                    livekitApiService = livekitApiService,
                    userApiService = userApiService,
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

    private fun fetchRtmpUrl() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPocInfo()
                val accessToken = authManager.getCurrentAccessToken()
                val appSettings = AppSettingsManager.getInstance(this@RCScreenActivity)
                val baseRtmpUrl = appSettings.getRtmpUrl()
                if (response.isSuccessful) {
                    val pocId = response.body()?.data?.ID
                    if (pocId != null) {
                        withContext(Dispatchers.Main) {
                            rtmpUrl =
                                "$baseRtmpUrl/$pocId?user=drone&pass=$accessToken"
                            Log.d("RTMP_URL", "Berhasil mengambil URL: $rtmpUrl")
                        }
                    } else {
                        showToastOnMain("Data Drone ID kosong")
                    }
                } else {
                    showToastOnMain("Gagal mengambil data drone dari server")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToastOnMain("Error koneksi saat mengambil data drone")
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
            val pocData = PocData(
                pitch = pitch.toDouble(),
                roll = roll.toDouble(),
                yaw = yaw.toDouble(),
                aircraftLatitude = loc?.latitude ?: 0.0,
                aircraftLongitude = loc?.longitude ?: 0.0,
                aircraftAltitude = loc?.altitude ?: 0.0,
                homeLatitude = loc?.latitude ?: 0.0,
                homeLongitude = loc?.longitude ?: 0.0,
                gpsSatelliteCount = 0,
                gpsSignalLevel = if (loc != null) "GOOD" else "NO_GPS",
                battery = BatteryData.SingleBatteryState(
                    percentageRemaining = batteryLevel,
                    voltageLevel = 0f,
                    batteryStatus = getBatteryStatus(batteryLevel),
                ),
            )
            centrifugoManager.updatePocData(pocData)
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

    private suspend fun showToastOnMain(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@RCScreenActivity, message, Toast.LENGTH_SHORT).show()
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

    @Composable
    fun SimpleCameraScreen(
        location: Location?,
        yaw: Float,
        connectionState: CentrifugoConnectionState,
        isStreaming: Boolean,
        hasPermissions: Boolean,
        isFrontCamera: Boolean,
        onStartStream: () -> Unit,
        onStopStream: () -> Unit,
        onSwitchCamera: () -> Unit,
        livekitApiService: ApiService,
        userApiService: ApiService,
        livekitShouldConnect: Boolean,
        livekitIsMuted: Boolean,
        livekitIsSpeakerMuted: Boolean, // ← fix nama konsisten
        onLivekitConnect: () -> Unit,
        onLivekitDisconnect: () -> Unit,
        onLivekitMuteToggle: () -> Unit,
        onLivekitSpeakerToggle: () -> Unit, // ← fix nama konsisten
    ) {
        val context = LocalContext.current

        val hazeState = remember { HazeState() }
        var showStopStreamDialog by remember { mutableStateOf(false) }

        // Start: tap langsung mulai (resolusi tetap 720p). Stop: tekan-tahan 3 detik.
        val holdProgress = remember { Animatable(0f) }
        var isHoldingStop by remember { mutableStateOf(false) }
        val holdScope = rememberCoroutineScope()

        var swipMapToCam by remember { mutableStateOf(false) }

        val factory = remember(livekitApiService) { LivekitViewModelFactory(livekitApiService) }
        val livekitViewModel: LivekitViewModel = viewModel(factory = factory)
        val token by livekitViewModel.livekitToken.collectAsState()

        val userFactory = remember(userApiService) { UserViewModelFactory(userApiService) }
        val userViewModel: UserViewModel = viewModel(factory = userFactory, key = "UserViewModel")
        val user by userViewModel.user.collectAsState()

        var listUserSpeaking by remember { mutableStateOf<List<String>>(emptyList()) }

        LaunchedEffect(token) {
            if (!token.isNullOrEmpty()) {
                onLivekitConnect()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .navigationBarsPadding(),
        ) {
            Column {
                ConnectionStatusBar(
                    username = user?.Name?.trim(),
                    connectionState = connectionState,
                    onLogoutClick = {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                )

                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = if (swipMapToCam) {
                            Modifier
                                .width(140.dp)
                                .height(140.dp)
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .zIndex(0.2f)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    ) {
                        RCCameraPreview(
                            hasPermissions = hasPermissions,
                            controller = cameraController,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        if (swipMapToCam) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { swipMapToCam = !swipMapToCam },
                            )
                        }
                    }

                    Box(
                        modifier =
                        if (swipMapToCam) {
                            Modifier.fillMaxSize().zIndex(0.1f)
                        } else {
                            Modifier
                                .width(140.dp)
                                .height(140.dp)
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        },
                    ) {
                        OsmdroidMapView(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            deviceLocation = GeoPoint(
                                location?.latitude ?: -6.9828,
                                location?.longitude ?: 110.4091,
                            ),
                            deviceMarkerIcon = R.drawable.ic_map,
                            pocYaw = yaw,
                        )
                        if (!swipMapToCam) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { swipMapToCam = !swipMapToCam },
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxWidth()
                            .zIndex(0.3f),
                    ) {
                        if (isStreaming) {
                            AlertStream()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .zIndex(0.4f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val livekitConnected =
                                livekitShouldConnect && !token.isNullOrEmpty()

                            // 1. Call
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66041F44))
                                    .clickable { showStopStreamDialog = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = if (livekitConnected) R.drawable.outline_phone_in_talk_24 else R.drawable.outline_call_24,
                                    ),
                                    contentDescription = "Call",
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = ColorFilter.tint(if (livekitConnected) successColor else Color.White),
                                )
                            }

                            // 2. Flip kamera
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66041F44))
                                    .clickable { onSwitchCamera() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.outline_flip_camera_ios_24),
                                    contentDescription = "Switch camera",
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = ColorFilter.tint(Color.White),
                                )
                            }

                            // 3. Start / Stop Stream — tap untuk mulai, TAHAN 3 detik untuk stop
                            val streamingLatest by rememberUpdatedState(isStreaming)
                            val permissionLatest by rememberUpdatedState(hasPermissions)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(29.dp))
                                    .background(
                                        when {
                                            !hasPermissions -> Color.Gray
                                            isStreaming -> dangerColor
                                            else -> colorPrimary
                                        },
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                if (!permissionLatest) return@detectTapGestures
                                                if (!streamingLatest) {
                                                    // Belum live → tap biasa untuk mulai.
                                                    if (tryAwaitRelease()) onStartStream()
                                                } else {
                                                    // Sedang live → tahan hingga ring penuh untuk stop.
                                                    isHoldingStop = true
                                                    val fillJob = holdScope.launch {
                                                        holdProgress.snapTo(0f)
                                                        holdProgress.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = tween(
                                                                durationMillis = 3000,
                                                                easing = LinearEasing,
                                                            ),
                                                        )
                                                        onStopStream()
                                                    }
                                                    tryAwaitRelease()
                                                    if (holdProgress.value < 1f) fillJob.cancel()
                                                    isHoldingStop = false
                                                    holdScope.launch { holdProgress.snapTo(0f) }
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Image(
                                        painter = painterResource(
                                            id = if (isStreaming) R.drawable.outline_stop_circle_24 else R.drawable.outline_smart_display_24,
                                        ),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(if (isStreaming) Color.White else backgroundColor),
                                    )
                                    Text(
                                        text = if (isStreaming) "Tahan untuk Stop" else "Start Stream",
                                        color = if (isStreaming) Color.White else backgroundColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            // 4. Record video (toggle: tap mulai, tap lagi berhenti & simpan)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable(enabled = hasPermissions) {
                                        if (isRecording) stopRecording() else startRecording()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isRecording) 20.dp else 30.dp)
                                        .clip(RoundedCornerShape(if (isRecording) 4.dp else 15.dp))
                                        .background(dangerColor),
                                )
                            }

                            // 5. Ambil foto
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEAF6F9))
                                    .clickable(enabled = hasPermissions) { takePhoto() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.outline_photo_camera_24),
                                    contentDescription = "Camera",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(backgroundColor),
                                )
                            }
                        }
                    }
                }
            }

            // Overlay ring merah saat menahan tombol Stop
            if (isHoldingStop) {
                RCHoldToStopOverlay(progress = holdProgress.value)
            }

            val ctx = context ?: return@Box
            val settings = AppSettingsManager.getInstance(ctx)

            if (livekitShouldConnect && !token.isNullOrEmpty()) {
                RoomScope(
                    url = settings.getLivekitUrl(),
                    token = token!!,
                    audio = true,
                    video = false,
                    connect = true,
                ) {
                    // 1. Track lokal (mic)
                    val localTrackRefs by rememberTracks(sources = listOf(Track.Source.MICROPHONE))
                    val audioTracks = localTrackRefs.filter {
                        it.publication?.kind == Track.Kind.AUDIO
                    }

                    // 2. Remote audio tracks (sudah subscribed)
                    val remoteAudioTrackRefs by rememberTracks(
                        sources = listOf(Track.Source.MICROPHONE),
                        onlySubscribed = true,
                    )
                    val remoteAudioTracks = remoteAudioTrackRefs.filter {
                        it.participant is RemoteParticipant
                    }

                    // ── Effect: Mic lokal ──────────────────────────────────────────────────
                    // ✅ Pakai localTrackRefs sebagai dependency supaya tunggu mic track ter-publish
                    LaunchedEffect(localTrackRefs, livekitIsMuted) {
                        localTrackRefs.forEach { trackRef ->
                            val track = trackRef.publication?.track
                            if (track != null) {
                                it.localParticipant.setMicrophoneEnabled(!livekitIsMuted)
                                Log.d("LiveKit", "🎤 Mic enabled = ${!livekitIsMuted}")
                            }
                        }
                    }

                    // ── Effect: Speaker mute via AudioManager (level system) ───────────────
                    LaunchedEffect(livekitIsSpeakerMuted) {
                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.adjustStreamVolume(
                            AudioManager.STREAM_VOICE_CALL,
                            if (livekitIsSpeakerMuted) {
                                AudioManager.ADJUST_MUTE
                            } else {
                                AudioManager.ADJUST_UNMUTE
                            },
                            0,
                        )
                        Log.d("LiveKit", "🔇 STREAM_VOICE_CALL muted=$livekitIsSpeakerMuted")
                    }

                    // ── Effect: Speaker mute via track volume (level LiveKit, per-track) ───
                    for (trackRef in remoteAudioTracks) {
                        val sid = trackRef.publication?.sid ?: continue
                        key(sid) {
                            val track = trackRef.publication?.track
                            LaunchedEffect(livekitIsSpeakerMuted, track) {
                                val audioTrack = track as? RemoteAudioTrack
                                if (audioTrack != null) {
                                    val vol = if (livekitIsSpeakerMuted) 0.0 else 1.0
                                    audioTrack.setVolume(vol)
                                    Log.d("LiveKit", "🔊 Track $sid → volume=$vol")
                                } else {
                                    Log.w("LiveKit", "⚠️ Track $sid bukan RemoteAudioTrack: $track")
                                }
                            }
                        }
                    }
                    // Dialog hanya terima data, tidak kelola koneksi
                    if (showStopStreamDialog) {
                        DialogCall(
                            onDismiss = { showStopStreamDialog = false },
                            audioTracks = audioTracks,
                            isMuted = livekitIsMuted,
                            isSpeakerMuted = livekitIsSpeakerMuted,
                            onMuteToggle = onLivekitMuteToggle,
                            onSpeakerToggle = onLivekitSpeakerToggle,
                            onEndCall = {
                                onLivekitDisconnect()
                                livekitViewModel.clearLivekitToken()
                            },
                            onJoin = null,
                        )
                    }
                }
            }

            if (showStopStreamDialog && !livekitShouldConnect) {
                DialogCall(
                    onDismiss = { showStopStreamDialog = false },
                    audioTracks = emptyList(),
                    isMuted = livekitIsMuted,
                    isSpeakerMuted = livekitIsSpeakerMuted, // ← fix: tambah yang hilang
                    onMuteToggle = onLivekitMuteToggle,
                    onSpeakerToggle = onLivekitSpeakerToggle, // ← fix: tambah yang hilang
                    onEndCall = { },
                    onJoin = { livekitViewModel.fetchLivekitToken() },
                )
            }
        }
    }
}
