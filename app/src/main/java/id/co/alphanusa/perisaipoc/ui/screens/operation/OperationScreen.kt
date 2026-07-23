package id.co.alphanusa.perisaipoc.ui.screens.operation

import android.content.Context
import android.location.Location
import android.media.AudioManager
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chrisbanes.haze.HazeState
import id.co.alphanusa.perisaipoc.R
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.realtime.CentrifugoConnectionState
import id.co.alphanusa.perisaipoc.stream.CameraStreamController
import id.co.alphanusa.perisaipoc.ui.components.AlertStream
import id.co.alphanusa.perisaipoc.ui.components.CallDialog
import id.co.alphanusa.perisaipoc.ui.components.ConnectionStatusBar
import id.co.alphanusa.perisaipoc.ui.components.OsmdroidMapView
import id.co.alphanusa.perisaipoc.ui.components.RCCameraPreview
import id.co.alphanusa.perisaipoc.ui.components.RCHoldToStopOverlay
import id.co.alphanusa.perisaipoc.ui.components.backgroundColor
import id.co.alphanusa.perisaipoc.ui.viewmodel.LivekitViewModel
import id.co.alphanusa.perisaipoc.ui.viewmodel.UserViewModel
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.RemoteAudioTrack
import io.livekit.android.room.track.Track
import org.osmdroid.util.GeoPoint

/**
 * Layar operasional: preview kamera, peta, status koneksi, panel call, dan bar
 * kontrol bawah. Murni tampilan — seluruh aksi dilaporkan lewat callback.
 */
@Composable
fun OperationScreen(
    location: Location?,
    yaw: Float,
    connectionState: CentrifugoConnectionState,
    isStreaming: Boolean,
    hasPermissions: Boolean,
    isFrontCamera: Boolean,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onSwitchCamera: () -> Unit,
    livekitShouldConnect: Boolean,
    livekitIsMuted: Boolean,
    livekitIsSpeakerMuted: Boolean,
    onLivekitConnect: () -> Unit,
    onLivekitDisconnect: () -> Unit,
    onLivekitMuteToggle: () -> Unit,
    onLivekitSpeakerToggle: () -> Unit,
    cameraController: CameraStreamController,
    isRecording: Boolean,
    livekitUrl: String,
    onToggleRecording: () -> Unit,
    onTakePhoto: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current

    val hazeState = remember { HazeState() }
    var showCallPanel by remember { mutableStateOf(false) }

    // Start: tap langsung mulai (resolusi tetap 720p). Stop: tekan-tahan 3 detik.
    val holdProgress = remember { Animatable(0f) }
    var isHoldingStop by remember { mutableStateOf(false) }
    val holdScope = rememberCoroutineScope()

    var swipMapToCam by remember { mutableStateOf(false) }

    val livekitViewModel: LivekitViewModel = hiltViewModel()
    val token by livekitViewModel.roomToken.collectAsState()

    val userViewModel: UserViewModel = hiltViewModel()
    val user by userViewModel.profile.collectAsState()

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
                username = user?.name?.trim(),
                connectionState = connectionState,
                onLogoutClick = onLogout,
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
                    // Aspek view = aspek konten (4:3 native, portrait 3:4)
                    // sehingga tidak ada bar hitam maupun stretch.
                    RCCameraPreview(
                        hasPermissions = hasPermissions,
                        controller = cameraController,
                        modifier = if (swipMapToCam) {
                            Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(Constants.Stream.PREVIEW_ASPECT_RATIO)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(4.dp))
                        },
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
                            location?.latitude ?: Constants.Map.DEFAULT_LATITUDE,
                            location?.longitude ?: Constants.Map.DEFAULT_LONGITUDE,
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

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth()
                        .zIndex(0.3f),
                ) {
                    if (isStreaming) {
                        AlertStream()
                    }
                }

                OperationBottomBar(
                    hasPermissions = hasPermissions,
                    isStreaming = isStreaming,
                    isRecording = isRecording,
                    isCallConnected = livekitShouldConnect && !token.isNullOrEmpty(),
                    hazeState = hazeState,
                    holdProgress = holdProgress,
                    onHoldingChange = { isHoldingStop = it },
                    onOpenCallPanel = { showCallPanel = true },
                    onSwitchCamera = onSwitchCamera,
                    onStartStream = onStartStream,
                    onStopStream = onStopStream,
                    onToggleRecording = onToggleRecording,
                    onTakePhoto = onTakePhoto,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // Overlay ring merah saat menahan tombol Stop
        if (isHoldingStop) {
            RCHoldToStopOverlay(progress = holdProgress.value)
        }

        if (livekitShouldConnect && !token.isNullOrEmpty()) {
            RoomScope(
                url = livekitUrl,
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
                if (showCallPanel) {
                    CallDialog(
                        onDismiss = { showCallPanel = false },
                        audioTracks = audioTracks,
                        isMuted = livekitIsMuted,
                        isSpeakerMuted = livekitIsSpeakerMuted,
                        onMuteToggle = onLivekitMuteToggle,
                        onSpeakerToggle = onLivekitSpeakerToggle,
                        onEndCall = {
                            onLivekitDisconnect()
                            livekitViewModel.clearRoomToken()
                        },
                        onJoin = null,
                    )
                }
            }
        }

        if (showCallPanel && !livekitShouldConnect) {
            CallDialog(
                onDismiss = { showCallPanel = false },
                audioTracks = emptyList(),
                isMuted = livekitIsMuted,
                isSpeakerMuted = livekitIsSpeakerMuted, // ← fix: tambah yang hilang
                onMuteToggle = onLivekitMuteToggle,
                onSpeakerToggle = onLivekitSpeakerToggle, // ← fix: tambah yang hilang
                onEndCall = { },
                onJoin = { livekitViewModel.join() },
            )
        }
    }
}
