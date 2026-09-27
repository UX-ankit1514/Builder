package com.uxankit.stepwise.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.ui.task.TaskState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the Step Done screen shows, captured at the moment the user tapped Done. */
data class StepDoneInfo(
    val stepNumber: Int,
    val totalSteps: Int,
    val stepsDone: Int,
    val taskTitle: String,
    val taskDone: Boolean,
    val nextStepTitle: String?,
    val nextStepNumber: Int?,
)

class FocusViewModel(private val uid: String, taskId: String) : ViewModel() {
    private val repo = AppGraph.tasks

    val state: StateFlow<TaskState> = repo.task(uid, taskId)
        .map { task -> task?.let { TaskState.Ready(it) } ?: TaskState.Missing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskState.Loading)

    val all: StateFlow<List<Task>?> = repo.tasks(uid)

    private var beforeDone: Task? = null
    private val task: Task? get() = (state.value as? TaskState.Ready)?.task

    fun done(): StepDoneInfo? {
        val before = task ?: return null
        val after = Planner.completeCurrentStep(before)
        repo.save(uid, after)
        beforeDone = before
        return StepDoneInfo(
            stepNumber = (before.currentStepIndex + 1).coerceAtLeast(1),
            totalSteps = before.steps.size.coerceAtLeast(1),
            stepsDone = if (after.hasSteps) after.stepsDone else 1,
            taskTitle = before.title,
            taskDone = after.isDone,
            nextStepTitle = after.currentStep?.title,
            nextStepNumber = after.currentStepIndex.takeIf { it >= 0 }?.plus(1),
        )
    }

    fun undoDone() {
        beforeDone?.let { repo.save(uid, it) }
        beforeDone = null
    }

    /** IA Flow 5: Skip goes to the next task. The skipped one is not deleted. Returns the next task id. */
    fun skip(): String? {
        val current = task ?: return null
        repo.save(uid, Planner.skipped(current))
        return Planner.nextInQueue(all.value.orEmpty(), current.id)?.id
    }

    fun unskip(original: Task) = repo.save(uid, original)

    /** IA Flow 5: Pause saves the task as "Continue" on the Today screen. */
    fun pause() {
        task?.let { repo.save(uid, Planner.paused(it)) }
    }

    /** IA Flow 5: Too big → make it smaller. The current step is replaced in place. */
    fun makeSmaller(smaller: List<String>) {
        task?.let { repo.save(uid, Planner.splitCurrentStep(it, smaller)) }
    }

    fun snapshot(): Task? = task

    fun nextTaskId(): String? = task?.let { Planner.nextInQueue(all.value.orEmpty(), it.id)?.id }
}
