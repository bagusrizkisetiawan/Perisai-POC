package id.co.tigabersama.pochuaweistream.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import id.co.tigabersama.pochuaweistream.R
import id.co.tigabersama.pochuaweistream.ui.components.backgroundColor
import id.co.tigabersama.pochuaweistream.ui.components.colorPrimary

/**
 * Pilihan resolusi video untuk RTMP stream.
 * Lebar/tinggi dalam orientasi landscape (sensor kamera), bitrate dalam bit per detik.
 */
data class StreamResolution(
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int,
    val bitrate: Int
)

val defaultStreamResolutions = listOf(
    StreamResolution("1080p", "Full HD · 1920 x 1080", 1920, 1080, 4_000_000),
    StreamResolution("720p", "HD · 1280 x 720", 1280, 720, 2_500_000),
    StreamResolution("480p", "SD · 854 x 480", 854, 480, 1_200_000),
    StreamResolution("360p", "Hemat data · 640 x 360", 640, 360, 800_000),
)

@Composable
fun DialogResolution(
    onDismiss: () -> Unit,
    onSelect: (StreamResolution) -> Unit,
    resolutions: List<StreamResolution> = defaultStreamResolutions,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pilih Resolusi Video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Image(
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .size(22.dp),
                        painter = painterResource(id = R.drawable.outline_close_24),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

                Text(
                    text = "Stream akan dimulai setelah resolusi dipilih.",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                resolutions.forEachIndexed { index, res ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(res) }
                            .border(
                                width = 1.dp,
                                color = colorPrimary,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = res.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = res.subtitle,
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                        }
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.outline_smart_display_24),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPrimary)
                        )
                    }
                    if (index != resolutions.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
