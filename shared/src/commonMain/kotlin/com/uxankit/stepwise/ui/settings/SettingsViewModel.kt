package com.uxankit.stepwise.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.UserSettings
import com.uxankit.stepwise.platform.FileSharer
import com.uxankit.stepwise.util.Time
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val uid: String) : ViewModel() {

    val settings: StateFlow<UserSettings> = AppGraph.profiles.observe(uid)
        .map { it.profile?.settings ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    var exporting by mutableStateOf(false)
        private set

    fun update(transform: (UserSettings) -> UserSettings) {
        AppGraph.profiles.updateSettings(uid, transform(settings.value))
    }

    /** IA 8 · Export data: every task, step and note as a JSON file, via the share sheet. */
    fun export(sharer: FileSharer) {
        if (exporting) return
        viewModelScope.launch {
            exporting = true
            try {
                val json = AppGraph.accounts.exportJson(uid)
                sharer.share("stepwise-export-${Time.today()}.json", "application/json", json)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                AppGraph.messages.show("Couldn’t export right now. Please try again.")
            } finally {
                exporting = false
            }
        }
    }
}
