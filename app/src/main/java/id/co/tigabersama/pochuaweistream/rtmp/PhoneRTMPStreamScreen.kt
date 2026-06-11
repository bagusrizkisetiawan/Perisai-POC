//package id.co.tigabersama.pochuaweistream.rtmp
//
//import android.view.SurfaceHolder
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import com.pedro.encoder.input.video.CameraOpenException
//import com.pedro.library.rtmp.RtmpCamera2
//import com.pedro.library.view.OpenGlView
//import com.pedro.common.ConnectChecker
//
//@Composable
//fun PhoneRTMPStreamScreen(streamUrl: String) {
//    val context = LocalContext.current
//    var rtmpCamera by remember { mutableStateOf<RtmpCamera2?>(null) }
//    var isStreaming by remember { mutableStateOf(false) }
//
//    // Listener untuk memantau status koneksi RTMP
//    val connectChecker = remember {
//        object : ConnectChecker {
//            override fun onConnectionSuccess() {
//                isStreaming = true
//                // Bisa gunakan postValue ke ViewModel atau show Toast
//            }
//
//            override fun onConnectionFailed(reason: String) {
//                isStreaming = false
//                rtmpCamera?.stopStream()
//            }
//
//            override fun onConnectionStarted(url: String) {}
//            override fun onDisconnect() {
//                isStreaming = false
//            }
//            override fun onAuthError() {}
//            override fun onAuthSuccess() {}
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // AndroidView untuk menampilkan Preview Kamera
//        AndroidView(
//            modifier = Modifier.fillMaxSize(),
//            factory = { ctx ->
//                OpenGlView(ctx).apply {
//                    // Inisialisasi RtmpCamera2 menggunakan OpenGLView ini
//                    rtmpCamera = RtmpCamera2(this, connectChecker)
//
//                    // Callback saat Surface (layar render) siap
//                    holder.addCallback(object : SurfaceHolder.Callback {
//                        override fun surfaceCreated(holder: SurfaceHolder) {}
//                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                            rtmpCamera?.startPreview()
//                        }
//                        override fun surfaceDestroyed(holder: SurfaceHolder) {
//                            if (rtmpCamera?.isStreaming == true) {
//                                rtmpCamera?.stopStream()
//                            }
//                            rtmpCamera?.stopPreview()
//                        }
//                    })
//                }
//            }
//        )
//
//        // Tombol Kontrol Stream di atas Preview
//        Button(
//            onClick = {
//                if (!isStreaming) {
//                    if (rtmpCamera?.prepareAudio() == true && rtmpCamera?.prepareVideo() == true) {
//                        rtmpCamera?.startStream(streamUrl)
//                    } else {
//                        Toast.makeText(context, "Gagal mempersiapkan audio/video", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    rtmpCamera?.stopStream()
//                    isStreaming = false
//                }
//            },
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 32.dp)
//        ) {
//            Text(if (isStreaming) "Stop Stream" else "Start Stream")
//        }
//    }
//}