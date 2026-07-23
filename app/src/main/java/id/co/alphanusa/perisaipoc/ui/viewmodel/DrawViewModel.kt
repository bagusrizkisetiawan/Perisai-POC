package id.co.alphanusa.perisaipoc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.model.MapBounds
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.usecase.GetMapOverlayUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.LoadStickerImageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Memuat overlay peta untuk area yang sedang dilihat. Permintaan di-debounce
 * agar menggeser peta tidak memicu panggilan API beruntun.
 */
@HiltViewModel
class DrawViewModel @Inject constructor(
    private val getMapOverlay: GetMapOverlayUseCase,
    private val loadStickerImage: LoadStickerImageUseCase,
) : ViewModel() {

    private val _items = MutableStateFlow<List<MapOverlayItem>>(emptyList())
    val items: StateFlow<List<MapOverlayItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var fetchJob: Job? = null

    /** Dipanggil setiap kali batas area peta berubah. */
    fun loadOverlay(bounds: MapBounds) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(Constants.Map.FETCH_DEBOUNCE_MS)
            _isLoading.value = true
            when (val result = getMapOverlay(bounds)) {
                is AppResult.Success -> {
                    _items.value = result.data
                    _error.value = null
                }

                is AppResult.Failure -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    /** Mengunduh ikon pin; null bila gagal. */
    suspend fun loadSticker(iconId: String): ByteArray? =
        when (val result = loadStickerImage(iconId)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> null
        }
}
