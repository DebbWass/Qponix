package com.cupons.sms.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cupons.sms.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordsUiState(
    val customKeywords : Set<String> = emptySet(),
    val customBlacklist: Set<String> = emptySet()
)

@HiltViewModel
class KeywordsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<KeywordsUiState> = combine(
        prefs.customKeywords,
        prefs.customBlacklist
    ) { keywords, blacklist ->
        KeywordsUiState(customKeywords = keywords, customBlacklist = blacklist)
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = KeywordsUiState()
    )

    fun addKeyword(word: String) = viewModelScope.launch {
        prefs.addCustomKeyword(word)
    }

    fun removeKeyword(word: String) = viewModelScope.launch {
        prefs.removeCustomKeyword(word)
    }

    fun addBlacklist(word: String) = viewModelScope.launch {
        prefs.addBlacklistWord(word)
    }

    fun removeBlacklist(word: String) = viewModelScope.launch {
        prefs.removeBlacklistWord(word)
    }
}
