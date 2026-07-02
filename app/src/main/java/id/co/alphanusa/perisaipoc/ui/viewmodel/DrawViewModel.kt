package id.co.alphanusa.perisaipoc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.co.alphanusa.perisaipoc.data.remote.api.ApiService
import id.co.alphanusa.perisaipoc.data.remote.response.DrawMapItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawViewModel(
    private val api: ApiService,
) : ViewModel() {

    companion object {
        private const val TAG = "DrawViewModel"
    }

    private val _drawItems = MutableStateFlow<List<DrawMapItem>>(emptyList())
    val drawItems: StateFlow<List<DrawMapItem>> = _drawItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var fetchJob: Job? = null

    /** Dipanggil setiap kali bounding box peta berubah. Sudah di-debounce. */
    fun fetchDraw(long1: Double, lat1: Double, long2: Double, lat2: Double) {
        Log.d(TAG, "fetchDraw() called → long1=$long1, lat1=$lat1, long2=$long2, lat2=$lat2")
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(400)
            _isLoading.value = true
            try {
                val resp = api.getDraw(long1 = long1, lat1 = lat1, long2 = long2, lat2 = lat2)
                val items = resp.data.orEmpty()

                // ✅ ASSIGN STATE DULU — supaya UI dapat data meskipun logging crash
                _drawItems.value = items
                _error.value = null
                Log.d(TAG, "📦 _drawItems di-update → ${items.size} item")

                // 🔹 Logging detail (null-safe + dibungkus try-catch terpisah)
                try {
                    Log.d(TAG, "✅ Response (size=${items.size}): ${resp.message}")
                    items.forEachIndexed { i, item ->
                        val pt = item.point
                        val pts = item.points
                        Log.d(
                            TAG,
                            "[$i] type=${item.type}, color=${item.color}, " +
                                "point=${pt?.let { "(${it.lat},${it.long})" } ?: "null"}, " +
                                "points=${pts?.size ?: 0}, radius=${item.radius}",
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Logging detail gagal (abaikan)", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gagal fetch /v1/draw: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class DrawViewModelFactory(
    private val api: ApiService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DrawViewModel(api) as T
}
