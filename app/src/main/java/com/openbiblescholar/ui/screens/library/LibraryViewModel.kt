package com.openbiblescholar.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbiblescholar.data.db.entity.SwordModuleEntity
import com.openbiblescholar.services.sword.DownloadProgress
import com.openbiblescholar.services.sword.SwordModuleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val modules: List<SwordModuleEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val moduleManager: SwordModuleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val downloadProgress: StateFlow<DownloadProgress> = moduleManager.downloadProgress

    init {
        loadModules()
    }

    private fun loadModules() {
        viewModelScope.launch {
            moduleManager.seedDefaultModules()
        }
        moduleManager.downloadProgress // observe
        // Observe all modules from DB
        viewModelScope.launch {
            // Use the static catalog merged with DB state
            _uiState.update { it.copy(modules = moduleManager.availableModules, isLoading = false) }
        }
    }

    fun downloadModule(name: String) {
        viewModelScope.launch {
            moduleManager.downloadModule(name)
            // Refresh after download
            _uiState.update { it.copy(modules = moduleManager.availableModules) }
        }
    }

    fun deleteModule(name: String) {
        viewModelScope.launch {
            moduleManager.deleteModule(name)
            _uiState.update { it.copy(modules = moduleManager.availableModules) }
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            moduleManager.seedDefaultModules()
            _uiState.update { it.copy(modules = moduleManager.availableModules) }
        }
    }
}
