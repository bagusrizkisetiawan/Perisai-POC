package id.co.alphanusa.perisaipoc.ui.components

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.co.alphanusa.perisaipoc.stream.CameraStreamController

/**
 * Menampilkan preview kamera lewat [SurfaceView] yang di-drive oleh RtmpStream.
 * Frame berasal dari CameraX (melalui CameraXSource) sehingga setajam PreviewView.
 * Bila izin belum diberikan, menampilkan teks penunggu.
 */
@Composable
fun RCCameraPreview(
    hasPermissions: Boolean,
    controller: CameraStreamController,
    modifier: Modifier = Modifier,
) {
    if (!hasPermissions) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Menunggu Izin Kamera...", color = Color.White, fontSize = 16.sp)
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        controller.onSurfaceAvailable(this@apply)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int,
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        controller.onSurfaceDestroyed()
                    }
                })
            }
        },
    )
}
