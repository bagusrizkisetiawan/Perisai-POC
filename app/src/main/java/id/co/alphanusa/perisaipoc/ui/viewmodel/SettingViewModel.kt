package id.co.alphanusa.perisaipoc.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.co.alphanusa.perisaipoc.domain.model.AppConfig
import id.co.alphanusa.perisaipoc.domain.repository.SettingsRepository
import id.co.alphanusa.perisaipoc.domain.usecase.UpdateAppConfigUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Membaca & menyimpan konfigurasi endpoint pada layar Settings. */
@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateAppConfig: UpdateAppConfigUseCase,
) : ViewModel() {

    val config: StateFlow<AppConfig> = settingsRepository.config

    fun save(config: AppConfig) = updateAppConfig(config)

    fun resetToDefaults() = settingsRepository.resetToDefaults()
}
