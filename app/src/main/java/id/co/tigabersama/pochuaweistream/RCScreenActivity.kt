package id.co.tigabersama.pochuaweistream

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
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.utils.gl.AspectRatioMode
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import id.co.tigabersama.pochuaweistream.data.local.AppSettingsManager
import id.co.tigabersama.pochuaweistream.data.remote.api.ApiConfig
import id.co.tigabersama.pochuaweistream.data.remote.api.ApiService
import id.co.tigabersama.pochuaweistream.domain.model.BatteryData
import id.co.tigabersama.pochuaweistream.domain.model.PocData
import id.co.tigabersama.pochuaweistream.domain.model.getBatteryStatus
import id.co.tigabersama.pochuaweistream.realtime.CentrifugoClientManager
import id.co.tigabersama.pochuaweistream.realtime.CentrifugoConnectionState
import id.co.tigabersama.pochuaweistream.ui.components.AlertStream
import id.co.tigabersama.pochuaweistream.ui.components.ConnectionStatusBar
import id.co.tigabersama.pochuaweistream.ui.components.DialogCall
import id.co.tigabersama.pochuaweistream.ui.components.DialogMap
import id.co.tigabersama.pochuaweistream.ui.components.DialogResolution
import id.co.tigabersama.pochuaweistream.ui.components.OsmdroidMapView
import id.co.tigabersama.pochuaweistream.ui.components.SwipeToStopButton
import id.co.tigabersama.pochuaweistream.ui.components.backgroundColor
import id.co.tigabersama.pochuaweistream.ui.components.colorPrimary
import id.co.tigabersama.pochuaweistream.ui.components.dangerColor
import id.co.tigabersama.pochuaweistream.ui.components.successColor
import id.co.tigabersama.pochuaweistream.ui.theme.POCHuaweiStreamTheme
import id.co.tigabersama.pochuaweistream.ui.viewmodel.LivekitViewModel
import id.co.tigabersama.pochuaweistream.ui.viewmodel.LivekitViewModelFactory
import id.co.tigabersama.pochuaweistream.ui.viewmodel.UserViewModel
import id.co.tigabersama.pochuaweistream.ui.viewmodel.UserViewModelFactory
import id.co.tigabersama.pochuaweistream.utils.HuaweiLocationHelper
import id.co.tigabersama.pochuaweistream.utils.ILocationHelper
import id.co.tigabersama.pochuaweistream.utils.NativeLocationHelper
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

class RCScreenActivity : ComponentActivity(), ConnectChecker {

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

    // Camera & Stream Variables
    private var rtmpCamera: RtmpCamera2? = null
    private var rtmpUrl: String? = null
    private var isStreaming by mutableStateOf(false)
    var isFrontCamera by mutableStateOf(false)

    // Device States (Location, Permissions, Sensors)
    private var currentLocation by mutableStateOf<Location?>(null)
    private var hasPermissions by mutableStateOf(false)
    private var pitch by mutableFloatStateOf(0f)
    private var roll by mutableFloatStateOf(0f)
    private var yaw by mutableFloatStateOf(0f)
    private var batteryLevel by mutableIntStateOf(0)

    // LiveKit
    private val wsURL = "wss://livekit.digicx.id"
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
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
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
                    SensorManager.SENSOR_DELAY_UI
                )

                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }


            LaunchedEffect(connectionState) {
                when (connectionState) {
                    CentrifugoConnectionState.ERROR,
                    CentrifugoConnectionState.DISCONNECTED -> {
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
                    onStartStream = { w, h, br -> startRtmpStream(w, h, br) },
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
                            0
                        ) // restore ke 50%
                    },
                    onLivekitMuteToggle = { livekitIsMuted = !livekitIsMuted },
                    onLivekitSpeakerToggle = { livekitIsSpeakerMuted = !livekitIsSpeakerMuted }
                )
            }
        }
    }

    fun ComponentActivity.requireNeededPermissions(onPermissionsGranted: (() -> Unit)? = null) {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                // Check if any permissions weren't granted.
                for (grant in grants.entries) {
                    if (!grant.value) {
                        Toast.makeText(
                            this,
                            "Missing permission: ${grant.key}",
                            Toast.LENGTH_SHORT
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
                    it
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

        if (isStreaming) rtmpCamera?.stopStream()
        rtmpCamera?.stopPreview()
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
                                "${baseRtmpUrl}/$pocId?user=drone&pass=$accessToken"
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

    private fun startRtmpStream(width: Int, height: Int, bitrate: Int) {
        if (isStreaming) return

        val urlToStream = rtmpUrl
        if (urlToStream == null) {
            Toast.makeText(this, "URL RTMP belum siap, silakan tunggu...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val rotation = CameraHelper.getCameraOrientation(this)
        val prepared = rtmpCamera?.prepareVideo(
            width,
            height,
            30,             // fps
            bitrate,
            2,              // iFrameInterval (detik)
            rotation
        ) == true

        if (prepared) {
            rtmpCamera?.startStream(urlToStream)
            isStreaming = true
        } else {
            Toast.makeText(this, "Gagal menyiapkan video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRtmpStream() {
        if (!isStreaming) return
        rtmpCamera?.stopStream()
        isStreaming = false
        Toast.makeText(this, "RTMP Stream Dihentikan", Toast.LENGTH_SHORT).show()
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
                    batteryStatus = getBatteryStatus(batteryLevel)
                )
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
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return camera && audio && location
    }

    private fun getBatteryPercentage(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun switchCamera() {
        rtmpCamera?.switchCamera()
        isFrontCamera = !isFrontCamera
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

    override fun onConnectionStarted(url: String) = runOnUiThread {
        Toast.makeText(this, "Memulai koneksi...", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionSuccess() = runOnUiThread {
        Toast.makeText(this, "Berhasil terhubung ke Server RTMP", Toast.LENGTH_SHORT).show()
    }

    override fun onConnectionFailed(reason: String) = runOnUiThread {
        Toast.makeText(this, "Gagal terhubung: $reason", Toast.LENGTH_LONG).show()
        rtmpCamera?.stopStream()
        isStreaming = false
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() = runOnUiThread {
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
        onStartStream: (width: Int, height: Int, bitrate: Int) -> Unit,
        onStopStream: () -> Unit,
        onSwitchCamera: () -> Unit,
        livekitApiService: ApiService,
        userApiService: ApiService,
        livekitShouldConnect: Boolean,
        livekitIsMuted: Boolean,
        livekitIsSpeakerMuted: Boolean,         // ← fix nama konsisten
        onLivekitConnect: () -> Unit,
        onLivekitDisconnect: () -> Unit,
        onLivekitMuteToggle: () -> Unit,
        onLivekitSpeakerToggle: () -> Unit,     // ← fix nama konsisten
    ) {
        val context = LocalContext.current

        val hazeState = remember { HazeState() }
        var showStopStreamDialog by remember { mutableStateOf(false) }
        var showResolutionDialog by remember { mutableStateOf(false) }
        var showStopConfirmDialog by remember { mutableStateOf(false) }

        // Saat sedang stream: tombol diganti "geser untuk akhiri" + dialog konfirmasi.
        // Saat belum stream: klik -> munculkan dialog pilih resolusi dulu.
        val onStreamClick = {
            if (!isStreaming) showResolutionDialog = true
        }

        var showDialogMap by remember { mutableStateOf(false) }

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
                .navigationBarsPadding()
        ) {
            // ── Camera Preview ──────────────────────────────────────────────
            if (hasPermissions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState)
                ) {
                    AndroidView(
                        factory = { context ->
                            OpenGlView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }.also { glView ->
                                rtmpCamera = RtmpCamera2(glView, this@RCScreenActivity)
                                glView.setAspectRatioMode(AspectRatioMode.Fill)

                                // Surface SurfaceView dihancurkan setiap kali aplikasi
                                // masuk background, dan dibuat ulang saat kembali ke depan.
                                // Callback ini memastikan preview kamera otomatis hidup
                                // lagi tanpa perlu kill aplikasi.
                                glView.holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {}

                                    override fun surfaceChanged(
                                        holder: SurfaceHolder,
                                        format: Int,
                                        width: Int,
                                        height: Int
                                    ) {
                                        if (rtmpCamera?.isOnPreview == false) {
                                            rtmpCamera?.startPreview()
                                        }
                                    }

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        if (rtmpCamera?.isOnPreview == true) {
                                            rtmpCamera?.stopPreview()
                                        }
                                    }
                                })
                            }
                        },
                        update = { glView ->
                            if (rtmpCamera?.isOnPreview == false) {
                                glView.post { rtmpCamera?.startPreview() }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Menunggu Izin Kamera...", color = Color.White, fontSize = 16.sp)
                }
            }

            // ── 2. Top Bar: Status & Lokasi ─────────────────────────────────
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                ConnectionStatusBar(
                    username = user?.Name?.trim(),
                    connectionState = connectionState,
                    onLogoutClick = {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(100.dp),
                    ) {
                        if (!showDialogMap) {
                            OsmdroidMapView(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                deviceLocation = GeoPoint(
                                    location?.latitude ?: -6.9828,
                                    location?.longitude ?: 110.4091
                                ),
                                deviceMarkerIcon = R.drawable.ic_map,
                                pocYaw = yaw
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showDialogMap = true }
                        )
                    }
                    if (isStreaming) {
                        AlertStream()
                    }
                }
            }

            // ── Bottom Bar: Lokasi + Tombol ─────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
                    .background(color = Color(0x80070C28))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Stream

                    if (livekitShouldConnect && !token.isNullOrEmpty()) {
                        if (isStreaming) {
                            SwipeToStopButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                onConfirmed = { showStopConfirmDialog = true }
                            )
                        } else {
                            Button(
                                onClick = onStreamClick,
                                enabled = hasPermissions,
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(40.dp),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorPrimary,
                                    disabledContainerColor = Color.Gray
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.outline_smart_display_24),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(backgroundColor)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clickable {
                                    showStopStreamDialog = true
                                }
                                .border(
                                    width = 1.dp,
                                    color = if (livekitShouldConnect && !token.isNullOrEmpty()) successColor else colorPrimary,
                                    shape = RoundedCornerShape(size = 2.dp)
                                )
                                .weight(1f)
                                .height(40.dp)
                                .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(id = if (livekitShouldConnect && !token.isNullOrEmpty()) R.drawable.outline_phone_in_talk_24 else R.drawable.outline_call_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(if (livekitShouldConnect && !token.isNullOrEmpty()) successColor else colorPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (listUserSpeaking.isNotEmpty()) {
                                    Text(
                                        text = "Speaking: " + listUserSpeaking.joinToString(", "),
                                        color = successColor,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Speaking: -",
                                        color = successColor,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        if (isStreaming) {
                            SwipeToStopButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                text = "Geser untuk akhiri stream",
                                onConfirmed = { showStopConfirmDialog = true }
                            )
                        } else {
                            Button(
                                onClick = onStreamClick,
                                enabled = hasPermissions,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorPrimary,
                                    disabledContainerColor = Color.Gray
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.outline_smart_display_24),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(backgroundColor)
                                    )
                                    Text(
                                        text = "Start Stream",
                                        color = backgroundColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clickable {
                                    showStopStreamDialog = true
                                }
                                .border(
                                    width = 1.dp,
                                    color = colorPrimary,
                                    shape = RoundedCornerShape(size = 2.dp)
                                )
                                .width(44.dp)
                                .height(40.dp)
                                .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                        ) {
                            Image(
                                modifier = Modifier.align(Alignment.Center),
                                painter = painterResource(id = R.drawable.outline_call_24),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        Modifier
                            .clickable {
                                onSwitchCamera()
                            }
                            .border(
                                width = 1.dp,
                                color = colorPrimary,
                                shape = RoundedCornerShape(size = 2.dp)
                            )
                            .width(44.dp)
                            .height(40.dp)
                            .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                    ) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(id = R.drawable.outline_flip_camera_ios_24),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPrimary)
                        )
                    }
                }
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
                        onlySubscribed = true
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
                            if (livekitIsSpeakerMuted) AudioManager.ADJUST_MUTE
                            else AudioManager.ADJUST_UNMUTE,
                            0
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
                            onJoin = null
                        )
                    }
                }
            }

            if (showStopStreamDialog && !livekitShouldConnect) {
                DialogCall(
                    onDismiss = { showStopStreamDialog = false },
                    audioTracks = emptyList(),
                    isMuted = livekitIsMuted,
                    isSpeakerMuted = livekitIsSpeakerMuted,     // ← fix: tambah yang hilang
                    onMuteToggle = onLivekitMuteToggle,
                    onSpeakerToggle = onLivekitSpeakerToggle,   // ← fix: tambah yang hilang
                    onEndCall = { },
                    onJoin = { livekitViewModel.fetchLivekitToken() }
                )
            }
            if (showDialogMap) {
                DialogMap(
                    onDismiss = { showDialogMap = false }, deviceLocation = GeoPoint(
                        location?.latitude ?: -6.9828,
                        location?.longitude ?: 110.4091
                    ),
                    deviceMarkerIcon = R.drawable.ic_map,
                    pocYaw = yaw
                )
            }

            // Dialog konfirmasi setelah geser tombol "akhiri stream"
            if (showStopConfirmDialog) {
                Dialog(
                    onDismissRequest = { showStopConfirmDialog = false },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .border(
                                        width = 0.5.dp,
                                        color = colorPrimary,
                                        shape = RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp,
                                            bottomStart = 10.dp,
                                            bottomEnd = 10.dp
                                        )
                                    )
                                    .fillMaxWidth()
                                    .matchParentSize()
                                    .background(
                                        color = Color(0x1A02D8FA),
                                        shape = RoundedCornerShape(
                                            topStart = 2.dp,
                                            topEnd = 2.dp,
                                            bottomStart = 10.dp,
                                            bottomEnd = 10.dp
                                        )
                                    )
                                    .padding(20.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    color = colorPrimary,
                                    shape = RoundedCornerShape(
                                        topStart = 2.dp,
                                        topEnd = 2.dp
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Stop Stream",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Image(
                                    modifier = Modifier
                                        .clickable {
                                            showStopConfirmDialog = false
                                        }
                                        .size(24.dp),
                                    painter = painterResource(id = R.drawable.outline_close_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.border_left),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(28.dp)
                        )

                        Image(
                            painter = painterResource(id = R.drawable.border_right),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .padding(top = 42.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Apakah Anda yakin ingin mengakhiri siaran (stream) ini?",
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Button(
                                    onClick = {
                                        showStopConfirmDialog = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = dangerColor,
                                        disabledContainerColor = Color.Gray
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = "Batal", color = Color.White)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        showStopConfirmDialog = false
                                        onStopStream()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorPrimary,
                                        disabledContainerColor = Color.Gray
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = "Stop Stream", color = backgroundColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dialog pilih resolusi sebelum mulai stream
            if (showResolutionDialog) {
                DialogResolution(
                    onDismiss = { showResolutionDialog = false },
                    onSelect = { res ->
                        showResolutionDialog = false
                        onStartStream(res.width, res.height, res.bitrate)
                    }
                )
            }
        }
    }
}