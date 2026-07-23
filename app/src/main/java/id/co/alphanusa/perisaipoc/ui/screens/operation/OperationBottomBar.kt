package id.co.alphanusa.perisaipoc.ui.screens.operation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import id.co.alphanusa.perisaipoc.R
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.ui.components.backgroundColor
import id.co.alphanusa.perisaipoc.ui.components.colorPrimary
import id.co.alphanusa.perisaipoc.ui.components.dangerColor
import id.co.alphanusa.perisaipoc.ui.components.successColor
import kotlinx.coroutines.launch

/**
 * Bar kontrol bawah layar operasional: panggilan, flip kamera, tombol
 * Start/Stop stream (stop harus ditahan), rekam video, dan ambil foto.
 */
@Composable
fun OperationBottomBar(
    hasPermissions: Boolean,
    isStreaming: Boolean,
    isRecording: Boolean,
    isCallConnected: Boolean,
    hazeState: HazeState,
    holdProgress: Animatable<Float, AnimationVector1D>,
    onHoldingChange: (Boolean) -> Unit,
    onOpenCallPanel: () -> Unit,
    onSwitchCamera: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleRecording: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holdScope = rememberCoroutineScope()
    Column(
        modifier = modifier
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
            val livekitConnected = isCallConnected

            // 1. Call
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x66041F44))
                    .clickable { onOpenCallPanel() },
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
                                    onHoldingChange(true)
                                    val fillJob = holdScope.launch {
                                        holdProgress.snapTo(0f)
                                        holdProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(
                                                durationMillis = Constants.HOLD_TO_STOP_MS,
                                                easing = LinearEasing,
                                            ),
                                        )
                                        onStopStream()
                                    }
                                    tryAwaitRelease()
                                    if (holdProgress.value < 1f) fillJob.cancel()
                                    onHoldingChange(false)
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
                        onToggleRecording()
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
                    .clickable(enabled = hasPermissions) { onTakePhoto() },
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
