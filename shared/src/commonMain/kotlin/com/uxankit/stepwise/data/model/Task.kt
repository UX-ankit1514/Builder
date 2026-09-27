package com.uxankit.stepwise.data.model

import kotlinx.datetime.LocalDate

/** Where a task lives. Only Inbox has no limit (IA: Urgent max 3, Today max 5). */
enum class TaskList(val key: String) {
    Inbox("inbox"),
    Today("today"),
    Urgent("urgent");

    companion object {
        fun fromKey(key: String): TaskList = entries.firstOrNull { it.key == key } ?: Inbox
    }
}

object Limits {
    const val URGENT = 3
    const val TODAY = 5
    const val TITLE_MAX = 200
    const val NOTES_MAX = 5000
    const val STEPS_MAX = 50
}

data class Step(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val deadline: LocalDate? = null,
    val list: TaskList = TaskList.Inbox,
    val steps: List<Step> = emptyList(),
    val pausedAt: Long? = null,
    val skippedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Double,
    val plannedAt: Long? = null,
) {
    val isDone: Boolean get() = completedAt != null
    val hasSteps: Boolean get() = steps.isNotEmpty()
    val stepsDone: Int get() = steps.count { it.done }

    /** The step Focus Mode shows: the first one not done yet. */
    val currentStepIndex: Int get() = steps.indexOfFirst { !it.done }
    val currentStep: Step? get() = steps.getOrNull(currentStepIndex)
    val nextStepAfterCurrent: Step?
        get() = currentStepIndex.takeIf { it >= 0 }?.let { index ->
            steps.drop(index + 1).firstOrNull { !it.done }
        }
}
