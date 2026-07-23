package id.co.alphanusa.perisaipoc.ui.screen.home

import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import id.co.alphanusa.perisaipoc.R
import id.co.alphanusa.perisaipoc.RCScreenActivity
import id.co.alphanusa.perisaipoc.ui.components.QRCodeScannerDialog
import id.co.alphanusa.perisaipoc.ui.components.backgroundColor
import id.co.alphanusa.perisaipoc.ui.components.colorPrimary
import id.co.alphanusa.perisaipoc.ui.components.dangerColor
import id.co.alphanusa.perisaipoc.ui.viewmodel.AuthViewModel

@Composable
fun HomeScreen(authViewModel: AuthViewModel = hiltViewModel(), onNavigateToSettings: () -> Unit) {
    val authState by authViewModel.state.collectAsState()

    // Sesi masih ada bila sudah login ATAU refresh gagal/timeout (refresh token masih tersimpan).
    // Dalam kondisi ini tombol bawah = Logout, bukan Scan QR.
    val sessionActive = authState.isLoggedIn || authState.isConnectionError

    var showQRScanner by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf("") }

    val hazeState = remember { HazeState() }

    var locationStatus by remember { mutableStateOf<String?>(null) }
    var isCheckingLocation by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val camera = permissions[Manifest.permission.CAMERA] ?: false
            val audio = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (fineLocation && camera && audio) {
                // Izin diberikan semua, langsung buka RCScreenActivity
                val intent = Intent(context, RCScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    "Izin Kamera, Mic, & Lokasi wajib diberikan!",
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
    )

    if (authState.isLoggedIn) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }

    fun openRCScreen() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                backgroundColor,
            ),
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState),
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 120.dp),
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
                            bottomEnd = 10.dp,
                        ),
                    )
                    .hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(
                        color = Color(0x1A02D8FA),
                        shape = RoundedCornerShape(
                            topStart = 2.dp,
                            topEnd = 2.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 10.dp,
                        ),
                    )
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
            )
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 0.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = colorPrimary,
                        shape = RoundedCornerShape(
                            topStart = 2.dp,
                            topEnd = 2.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                        ),
                    ),

            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .align(Alignment.TopEnd),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.accent),
                    contentDescription = null,
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                )
            }

            Image(
                painter = painterResource(id = R.drawable.border_left),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(28.dp)
                    .height(28.dp),
            )
            Image(
                painter = painterResource(id = R.drawable.border_right),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(28.dp)
                    .height(28.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Header ──────────────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 52.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_app_home),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorPrimary),
                        modifier = Modifier.size(120.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // ── Muncul saat refresh token gagal/timeout (sesi masih tersimpan) ──
                    if (authState.isConnectionError) {
                        authState.error?.let { errorMsg ->
                            Box(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Accent line kiri
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(dangerColor)
                                        .align(Alignment.CenterStart),
                                )

                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Authentication Failed",
                                        fontSize = 14.sp,
                                        color = dangerColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text(
                                        text = errorMsg,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                // Accent line kanan
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(dangerColor)
                                        .align(Alignment.CenterEnd),
                                )
                            }
                            Spacer(modifier = Modifier.height(38.dp))
                        }

                        Button(
                            onClick = { authViewModel.reconnect() },
                            enabled = !authState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPrimary,
                                contentColor = backgroundColor,
                            ),
                        ) {
                            if (authState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = backgroundColor,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_refresh_24),
                                    contentDescription = "Reconnect",
                                    modifier = Modifier.size(18.dp),
                                    tint = backgroundColor,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Reconnect",
                                fontSize = 12.sp,
                                color = backgroundColor,
                            )
                        }
                    }
                    if (sessionActive) {
                        // Logout sebagai Row teks clickable (bukan tombol berisi)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { authViewModel.logout() }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = dangerColor,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Logout from PERISAI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = dangerColor,
                            )
                        }
                    } else {
                        // Scan QR sebagai tombol berisi (menangani state loading)
                        Button(
                            onClick = { showQRScanner = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (authState.isLoading) Color(0xFFFFA000) else colorPrimary,
                                disabledContainerColor = Color(0xFF37474F),
                            ),
                        ) {
                            if (authState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = backgroundColor,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = backgroundColor,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (authState.isLoading) "Authenticating..." else "Scan QR Code",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = backgroundColor,
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                }
            }
        }

        // ── QR Scanner Dialog (tidak diubah) ─────────────────────────────────
        if (showQRScanner) {
            QRCodeScannerDialog(
                onDismiss = { showQRScanner = false },
                onQRCodeScanned = { qrCode: String ->
                    // Parsing QR & penyimpanan konfigurasi ditangani ApplyQrConfigUseCase.
                    authViewModel.onQrScanned(qrCode)
                    showQRScanner = false
                },
            )
        }
        if (scannedResult.isNotEmpty()) {
            Log.d("QR CODE SCAN", "Scanned Result: $scannedResult")
        }
    }
}
