package id.co.alphanusa.perisaipoc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.CallParticipant
import id.co.alphanusa.perisaipoc.domain.usecase.GetCallParticipantsUseCase
import id.co.alphanusa.perisaipoc.domain.usecase.JoinCallUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mengelola token room dan daftar peserta komunikasi suara (LiveKit). */
@HiltViewModel
class LivekitViewModel @Inject constructor(
    private val joinCall: JoinCallUseCase,
    private val getParticipants: GetCallParticipantsUseCase,
) : ViewModel() {

    private val _roomToken = MutableStateFlow<String?>(null)
    val roomToken: StateFlow<String?> = _roomToken.asStateFlow()

    private val _participants = MutableStateFlow<List<CallParticipant>>(emptyList())
    val participants: StateFlow<List<CallParticipant>> = _participants.asStateFlow()

    fun join() {
        viewModelScope.launch {
            when (val result = joinCall()) {
                is AppResult.Success -> _roomToken.value = result.data.token
                is AppResult.Failure -> Unit
            }
        }
    }

    fun refreshParticipants() {
        viewModelScope.launch {
            when (val result = getParticipants()) {
                is AppResult.Success -> _participants.value = result.data
                is AppResult.Failure -> Unit
            }
        }
    }

    fun clearRoomToken() {
        _roomToken.value = null
    }
}
