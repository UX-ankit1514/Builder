package com.uxankit.stepwise.ui.home

import androidx.lifecycle.ViewModel
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.ui.plan.PlanningController
import kotlinx.coroutines.flow.StateFlow

/** Backs the Today, Inbox and Progress tabs, which all read the same task list. */
class HomeViewModel(private val uid: String) : ViewModel() {
    private val repo = AppGraph.tasks
    private val messages = AppGraph.messages

    val tasks: StateFlow<List<Task>?> = repo.tasks(uid)
    val planning = PlanningController(uid, repo, messages)

    private val current: List<Task> get() = tasks.value.orEmpty()

    /** IA Flow 2: only the name is required, and the task goes to the Inbox. */
    fun quickAdd(title: String) {
        if (title.isBlank()) return
        val task = repo.create(uid, title)
        messages.show(
            text = "Saved to Inbox",
            linkLabel = "Move to Today",
            link = { planning.move(task, TaskList.Today, current) },
            actionLabel = "Undo",
            action = { repo.delete(uid, task.id) },
        )
    }

    fun setDone(task: Task, done: Boolean) {
        repo.save(uid, if (done) Planner.markDone(task) else Planner.markNotDone(task))
        if (done) messages.showUndo("Done. Nice work.") { repo.save(uid, task) }
    }

    fun move(task: Task, target: TaskList) = planning.move(task, target, current)

    fun applyRebalance(choices: Map<String, TaskList>) = planning.applyRebalance(choices, current)

    fun delete(task: Task) {
        repo.delete(uid, task.id)
        messages.showUndo("“${task.title}” deleted") { repo.save(uid, task) }
    }
}
