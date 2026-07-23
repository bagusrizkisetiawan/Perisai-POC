package id.co.alphanusa.perisaipoc.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.co.alphanusa.perisaipoc.core.common.AppResult
import id.co.alphanusa.perisaipoc.domain.model.UserProfile
import id.co.alphanusa.perisaipoc.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Menyediakan profil pengguna yang sedang login untuk ditampilkan di UI. */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = getUserProfile()) {
                is AppResult.Success -> _profile.value = result.data
                is AppResult.Failure -> Unit
            }
        }
    }

    fun clear() {
        _profile.value = null
    }
}
