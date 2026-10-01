package com.aura.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val preferences: UserPreferences) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = preferences.preferences
        .map<UserPreferences, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
