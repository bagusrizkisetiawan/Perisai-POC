package id.co.tigabersama.pochuaweistream.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.co.tigabersama.pochuaweistream.R
import kotlin.math.roundToInt

/**
 * Tombol "geser untuk akhiri stream".
 * User harus menggeser thumb dari kiri ke kanan sampai ujung untuk memicu [onConfirmed]
 * (mencegah stop stream tidak sengaja lewat satu tap). Setelah dilepas, thumb kembali ke kiri.
 */
@Composable
fun SwipeToStopButton(
    modifier: Modifier = Modifier,
    text: String = "Geser untuk akhiri",
    onConfirmed: () -> Unit,
) {
    val density = LocalDensity.current
    val thumbSize = 34.dp
    val trackPadding = 3.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val trackPaddingPx = with(density) { trackPadding.toPx() }

    var trackWidthPx by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }

    // Jarak maksimal thumb bisa digeser (lebar track - thumb - padding kiri/kanan)
    val maxOffset = (trackWidthPx - thumbSizePx - trackPaddingPx * 2f).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(dangerColor.copy(alpha = 0.18f))
            .border(1.dp, dangerColor, RoundedCornerShape(2.dp))
            .onSizeChanged { trackWidthPx = it.width.toFloat() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = thumbSize),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((trackPaddingPx + offsetX).roundToInt(), 0) }
                .size(thumbSize)
                .clip(RoundedCornerShape(2.dp))
                .background(dangerColor)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (maxOffset > 0f && offsetX >= maxOffset * 0.9f) {
                                onConfirmed()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxOffset)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.outline_stop_circle_24),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}
