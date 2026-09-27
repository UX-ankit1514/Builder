package com.uxankit.stepwise.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.ui.plan.PlanningController
import com.uxankit.stepwise.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

sealed interface TaskState {
    data object Loading : TaskState
    data object Missing : TaskState
    data class Ready(val task: Task) : TaskState
}

class TaskViewModel(private val uid: String, taskId: String) : ViewModel() {
    private val repo = AppGraph.tasks
    private val messages = AppGraph.messages

    val state: StateFlow<TaskState> = repo.task(uid, taskId)
        .map { task -> task?.let { TaskState.Ready(it) } ?: TaskState.Missing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskState.Loading)

    val all: StateFlow<List<Task>?> = repo.tasks(uid)
    val planning = PlanningController(uid, repo, messages)

    private val task: Task? get() = (state.value as? TaskState.Ready)?.task

    private fun update(transform: (Task) -> Task) {
        val current = task ?: return
        repo.save(uid, transform(current).copy(updatedAt = Time.nowMillis()))
    }

    fun rename(title: String) {
        val clean = title.trim().take(Limits.TITLE_MAX)
        if (clean.isNotEmpty() && clean != task?.title) update { it.copy(title = clean) }
    }

    fun setNotes(notes: String) = update { it.copy(notes = notes.take(Limits.NOTES_MAX)) }

    fun setDeadline(date: LocalDate?) = update { it.copy(deadline = date) }

    fun toggleStep(stepId: String) = update { Planner.toggleStep(it, stepId) }

    fun saveSteps(steps: List<Pair<String?, String>>) {
        val before = task ?: return
        val updated = Planner.withSteps(before, steps)
        repo.save(uid, updated)
        val count = updated.steps.size
        messages.showUndo(if (count == 1) "1 step saved" else "$count steps saved") { repo.save(uid, before) }
    }

    fun move(target: TaskList) {
        val current = task ?: return
        planning.move(current, target, all.value.orEmpty())
    }

    fun applyRebalance(choices: Map<String, TaskList>) = planning.applyRebalance(choices, all.value.orEmpty())

    fun setDone(done: Boolean) {
        val before = task ?: return
        repo.save(uid, if (done) Planner.markDone(before) else Planner.markNotDone(before))
        if (done) messages.showUndo("Done. Nice work.") { repo.save(uid, before) }
    }

    fun delete() {
        val before = task ?: return
        repo.delete(uid, before.id)
        messages.showUndo("“${before.title}” deleted") { repo.save(uid, before) }
    }
}
