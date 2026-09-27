package com.uxankit.stepwise.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The dark pill bar at the bottom of the screen ("Saved to Inbox · Move to Today · Undo").
 * IA rule: Undo is available after Done, Skip and Delete.
 */
data class AppMessage(
    val id: Long,
    val text: String,
    /** Green action with the undo arrow, e.g. "Undo" or "Resume". */
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    /** Underlined secondary action, e.g. "Move to Today". */
    val linkLabel: String? = null,
    val link: (() -> Unit)? = null,
)

class Messages {
    private var nextId = 0L
    private val _current = MutableStateFlow<AppMessage?>(null)
    val current: StateFlow<AppMessage?> = _current.asStateFlow()

    fun show(
        text: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
        linkLabel: String? = null,
        link: (() -> Unit)? = null,
    ) {
        _current.value = AppMessage(++nextId, text, actionLabel, action, linkLabel, link)
    }

    fun showUndo(text: String, undo: () -> Unit) = show(text, actionLabel = "Undo", action = undo)

    fun dismiss(id: Long) {
        _current.update { if (it?.id == id) null else it }
    }
}
