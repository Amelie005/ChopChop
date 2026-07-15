package com.example.chopchoprecipeapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chopchoprecipeapp.data.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel handling the state persistence for user profile data.
 *
 * @param application The application instance.
 * @author Amelie Dzierzawa
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserSettingsRepository(application.applicationContext)

    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String> = repository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val profileImagePath: StateFlow<String?> = repository.profileImagePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Toggles and persists the dark mode setting.
     * @param enabled The new dark mode state
     */
    fun onDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveDarkMode(enabled)
        }
    }

    /**
     * Updates and persists the user name.
     * @param name The new user name
     */
    fun onUserNameChanged(name: String) {
        viewModelScope.launch {
            repository.saveUserName(name)
        }
    }

    /**
     * Updates and persists the local path of the profile image.
     * @param path The new local path of the profile image
     */
    fun onProfileImageChanged(path: String?) {
        viewModelScope.launch {
            repository.saveProfileImagePath(path)
        }
    }
}