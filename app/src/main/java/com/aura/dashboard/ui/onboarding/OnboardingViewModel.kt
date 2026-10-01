package com.aura.dashboard.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val preferences: UserPreferencesRepository,
    private val quickApps: QuickAppRepository,
) : ViewModel() {

    val name: StateFlow<String> = savedState.getStateFlow(KEY_NAME, "")
    val country: StateFlow<String> = savedState.getStateFlow(KEY_COUNTRY, UserPreferences.DEFAULT_COUNTRY)

    fun setName(value: String) {
        savedState[KEY_NAME] = value.take(40)
    }

    fun setCountry(code: String) {
        savedState[KEY_COUNTRY] = code
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { quickApps.seedDefaultsIfNeeded() }
            preferences.completeOnboarding(name.value, country.value)
            onDone()
        }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_COUNTRY = "country"
    }
}
