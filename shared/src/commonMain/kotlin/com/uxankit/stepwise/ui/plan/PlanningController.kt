package com.uxankit.stepwise.ui.plan

import com.uxankit.stepwise.data.Messages
import com.uxankit.stepwise.data.TaskRepository
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.util.Time
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shown when a move would go past a soft limit (Urgent 3, Today 5).
 * IA Flow 7: ask "Which ones are truly urgent?" instead of blocking.
 */
data class RebalanceState(
    val target: TaskList,
    val taskIds: List<String>,
    val choices: Map<String, TaskList>,
)

/** Moving tasks between Inbox / Today / Urgent, shared by the tabs and the Task Page. */
class PlanningController(
    private val uid: String,
    private val repo: TaskRepository,
    private val messages: Messages,
) {
    private val _rebalance = MutableStateFlow<RebalanceState?>(null)
    val rebalance: StateFlow<RebalanceState?> = _rebalance.asStateFlow()

    fun move(task: Task, target: TaskList, all: List<Task>) {
        if (task.list == target && !task.isDone) return
        val limit = Planner.limitOf(target)
        val occupants = Planner.open(all).filter { it.list == target && it.id != task.id }.sortedBy { it.sortOrder }
        if (limit != null && occupants.size >= limit) {
            val ids = occupants.map { it.id } + task.id
            _rebalance.value = RebalanceState(target, ids, ids.associateWith { target })
            return
        }
        repo.save(uid, Planner.moveTo(task.copy(completedAt = null), target))
        messages.showUndo(movedText(target)) { repo.save(uid, task) }
    }

    fun applyRebalance(choices: Map<String, TaskList>, all: List<Task>) {
        val before = all.filter { it.id in choices }
        val now = Time.nowMillis()
        val changed = before.filter { choices[it.id] != it.list }
        changed.forEach { repo.save(uid, Planner.moveTo(it, choices.getValue(it.id), now)) }
        _rebalance.value = null
        if (changed.isNotEmpty()) {
            messages.showUndo("Plan updated") { before.forEach { repo.save(uid, it) } }
        }
    }

    fun dismissRebalance() {
        _rebalance.value = null
    }

    private fun movedText(target: TaskList) = when (target) {
        TaskList.Urgent -> "Marked as urgent"
        TaskList.Today -> "Moved to Today"
        TaskList.Inbox -> "Moved to Inbox"
    }
}
