package id.co.alphanusa.perisaipoc.realtime

import android.util.Log
import com.google.gson.Gson
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.core.util.JwtDecoder
import id.co.alphanusa.perisaipoc.data.mapper.toDto
import id.co.alphanusa.perisaipoc.domain.model.PocTelemetry
import id.co.alphanusa.perisaipoc.domain.repository.RealtimeRepository
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import io.github.centrifugal.centrifuge.Client
import io.github.centrifugal.centrifuge.ConnectedEvent
import io.github.centrifugal.centrifuge.ConnectingEvent
import io.github.centrifugal.centrifuge.DisconnectedEvent
import io.github.centrifugal.centrifuge.ErrorEvent
import io.github.centrifugal.centrifuge.EventListener
import io.github.centrifugal.centrifuge.Options
import io.github.centrifugal.centrifuge.Subscription
import io.github.centrifugal.centrifuge.SubscriptionEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class CentrifugoConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Klien Centrifugo: menjaga koneksi WebSocket dan mengirim telemetri secara
 * berkala ke channel `mobile-data:<pocId>`.
 *
 * Id perangkat diambil dari klaim `sub` pada token, sehingga channel selalu
 * cocok dengan identitas yang diberikan server.
 */
@Singleton
class CentrifugoClientManager @Inject constructor(
    private val realtimeRepository: RealtimeRepository,
    private val settingsRepository: SettingsRepository,
) {

    private companion object {
        const val TAG = "CentrifugoClient"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob())

    private var client: Client? = null
    private var subscription: Subscription? = null
    private var transmissionJob: Job? = null
    private var currentTelemetry: PocTelemetry? = null

    private val _connectionState = MutableStateFlow(CentrifugoConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CentrifugoConnectionState> = _connectionState.asStateFlow()

    /** Membuka koneksi: ambil token dulu, lalu sambungkan WebSocket. */
    fun startConnection() {
        scope.launch {
            _connectionState.value = CentrifugoConnectionState.CONNECTING
            when (val result = realtimeRepository.generateConnectionToken()) {
                is AppResult.Success -> connect(result.data)
                is AppResult.Failure -> {
                    Log.e(TAG, "Gagal mengambil token: ${result.message}")
                    _connectionState.value = CentrifugoConnectionState.ERROR
                }
            }
        }
    }

    fun stopConnection() {
        transmissionJob?.cancel()
        subscription?.unsubscribe()
        client?.disconnect()
        _connectionState.value = CentrifugoConnectionState.DISCONNECTED
    }

    /** Menyimpan telemetri terbaru; pengiriman dilakukan oleh loop berkala. */
    fun updateTelemetry(telemetry: PocTelemetry) {
        currentTelemetry = telemetry
    }

    fun cleanup() {
        stopConnection()
        scope.cancel()
    }

    private fun connect(token: String) {
        val websocketUrl = settingsRepository.getConfig().centrifugoWebSocketUrl
        val pocId = JwtDecoder.extractSubject(token)
        if (pocId == null) {
            Log.e(TAG, "Token tidak memuat id perangkat")
            _connectionState.value = CentrifugoConnectionState.ERROR
            return
        }
        val channel = Constants.TELEMETRY_CHANNEL_PREFIX + pocId

        try {
            client = Client(websocketUrl, Options().apply { this.token = token }, listener(channel))
            client?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyambung ke Centrifugo", e)
            _connectionState.value = CentrifugoConnectionState.ERROR
        }
    }

    private fun listener(channel: String) = object : EventListener() {
        override fun onConnected(client: Client?, event: ConnectedEvent?) {
            _connectionState.value = CentrifugoConnectionState.CONNECTED
            subscribe(channel)
            startTransmission(channel)
        }

        override fun onDisconnected(client: Client?, event: DisconnectedEvent?) {
            _connectionState.value = CentrifugoConnectionState.DISCONNECTED
            transmissionJob?.cancel()
        }

        override fun onConnecting(client: Client?, event: ConnectingEvent?) {
            _connectionState.value = CentrifugoConnectionState.CONNECTING
        }

        override fun onError(client: Client?, event: ErrorEvent?) {
            Log.e(TAG, "Centrifugo error: ${event?.error?.message ?: "tidak diketahui"}")
            _connectionState.value = CentrifugoConnectionState.ERROR
        }
    }

    private fun subscribe(channel: String) {
        val activeClient = client ?: return
        try {
            subscription = activeClient.newSubscription(
                channel,
                object : SubscriptionEventListener() {},
            )
            subscription?.subscribe()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal subscribe channel $channel", e)
        }
    }

    private fun startTransmission(channel: String) {
        transmissionJob?.cancel()
        transmissionJob = scope.launch {
            while (isActive) {
                currentTelemetry?.let { publish(channel, it) }
                delay(Constants.TELEMETRY_INTERVAL_MS)
            }
        }
    }

    private fun publish(channel: String, telemetry: PocTelemetry) {
        val activeClient = client ?: return
        try {
            val payload = gson.toJson(telemetry.toDto()).toByteArray()
            activeClient.publish(channel, payload) { error, _ ->
                if (error != null) Log.e(TAG, "Gagal publish ke $channel", error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyiapkan payload telemetri", e)
        }
    }
}
