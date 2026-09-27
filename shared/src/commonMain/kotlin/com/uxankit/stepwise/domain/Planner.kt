package com.uxankit.stepwise.domain

import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Step
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.util.Ids
import com.uxankit.stepwise.util.Time
import kotlinx.datetime.LocalDate

/** Pure rules for what shows where. No Firebase here, so it is easy to test. */
object Planner {

    fun limitOf(list: TaskList): Int? = when (list) {
        TaskList.Urgent -> Limits.URGENT
        TaskList.Today -> Limits.TODAY
        TaskList.Inbox -> null
    }

    fun open(tasks: List<Task>): List<Task> = tasks.filter { !it.isDone }

    fun urgent(tasks: List<Task>): List<Task> =
        open(tasks).filter { it.list == TaskList.Urgent }.sortedBy { it.sortOrder }

    fun today(tasks: List<Task>): List<Task> =
        open(tasks).filter { it.list == TaskList.Today }.sortedBy { it.sortOrder }

    /** Newest first, like the Inbox design. */
    fun inbox(tasks: List<Task>): List<Task> =
        open(tasks).filter { it.list == TaskList.Inbox }.sortedByDescending { it.createdAt }

    fun count(tasks: List<Task>, list: TaskList): Int = open(tasks).count { it.list == list }

    /** The task most recently paused in Focus Mode that still has work left. */
    fun continueTask(tasks: List<Task>): Task? =
        open(tasks).filter { it.pausedAt != null }.maxByOrNull { it.pausedAt!! }

    fun doneOn(tasks: List<Task>, date: LocalDate): List<Task> =
        tasks.filter { it.completedAt != null && Time.dateOf(it.completedAt) == date }
            .sortedByDescending { it.completedAt }

    /**
     * The order Focus Mode walks through when the user skips: Urgent first, then Today.
     * Tasks skipped today go to the back of the line.
     */
    fun focusQueue(tasks: List<Task>, today: LocalDate = Time.today()): List<Task> {
        fun skippedToday(task: Task) = task.skippedAt?.let { Time.dateOf(it) == today } == true
        return (urgent(tasks) + today(tasks)).sortedBy { skippedToday(it) }
    }

    fun nextInQueue(tasks: List<Task>, afterTaskId: String): Task? =
        focusQueue(tasks).firstOrNull { it.id != afterTaskId }

    /** Tasks with steps that are not finished: the "Active goals" on Progress. */
    fun activeGoals(tasks: List<Task>): List<Task> =
        open(tasks).filter { it.hasSteps }.sortedByDescending { it.updatedAt }

    /** Steps finished on a date. A task finished without steps counts as one step. */
    fun stepsDoneOn(tasks: List<Task>, date: LocalDate): Int = tasks.sumOf { task ->
        val steps = task.steps.count { step -> step.doneAt?.let { Time.dateOf(it) == date } == true }
        val singleTask = if (!task.hasSteps && task.completedAt != null && Time.dateOf(task.completedAt) == date) 1 else 0
        steps + singleTask
    }

    fun stepsDoneInWeek(tasks: List<Task>, anyDayInWeek: LocalDate): Int =
        Time.weekOf(anyDayInWeek).sumOf { stepsDoneOn(tasks, it) }

    // ---------------------------------------------------------------------
    // Mutations. Each returns a new Task; the repository persists it.
    // ---------------------------------------------------------------------

    fun moveTo(task: Task, list: TaskList, now: Long = Time.nowMillis()): Task = task.copy(
        list = list,
        plannedAt = if (list == TaskList.Inbox) null else now,
        sortOrder = now.toDouble(),
        updatedAt = now,
    )

    fun markDone(task: Task, now: Long = Time.nowMillis()): Task =
        task.copy(completedAt = now, pausedAt = null, updatedAt = now)

    fun markNotDone(task: Task, now: Long = Time.nowMillis()): Task =
        task.copy(completedAt = null, updatedAt = now)

    /** Marks the current step done. Finishing the last step finishes the task. */
    fun completeCurrentStep(task: Task, now: Long = Time.nowMillis()): Task {
        if (!task.hasSteps) return markDone(task, now)
        val index = task.currentStepIndex
        if (index < 0) return markDone(task, now)
        val steps = task.steps.mapIndexed { i, step ->
            if (i == index) step.copy(done = true, doneAt = now) else step
        }
        val allDone = steps.all { it.done }
        return task.copy(
            steps = steps,
            completedAt = if (allDone) now else null,
            pausedAt = if (allDone) null else task.pausedAt,
            updatedAt = now,
        )
    }

    fun toggleStep(task: Task, stepId: String, now: Long = Time.nowMillis()): Task {
        val steps = task.steps.map { step ->
            if (step.id != stepId) step
            else if (step.done) step.copy(done = false, doneAt = null)
            else step.copy(done = true, doneAt = now)
        }
        val allDone = steps.isNotEmpty() && steps.all { it.done }
        return task.copy(steps = steps, completedAt = if (allDone) (task.completedAt ?: now) else null, updatedAt = now)
    }

    /** Replaces the step list, keeping done state for steps the user did not remove. */
    fun withSteps(task: Task, titles: List<Pair<String?, String>>, now: Long = Time.nowMillis()): Task {
        val existing = task.steps.associateBy { it.id }
        val steps = titles
            .map { (id, title) -> id to title.trim() }
            .filter { (_, title) -> title.isNotEmpty() }
            .take(Limits.STEPS_MAX)
            .map { (id, title) ->
                val old = id?.let { existing[it] }
                old?.copy(title = title) ?: Step(id = Ids.step(), title = title)
            }
        return task.copy(steps = steps, updatedAt = now)
    }

    /** "Make it smaller": the current step is replaced by smaller ones, in place. */
    fun splitCurrentStep(task: Task, smaller: List<String>, now: Long = Time.nowMillis()): Task {
        val newSteps = smaller.map { it.trim() }.filter { it.isNotEmpty() }.map { Step(Ids.step(), it) }
        if (newSteps.isEmpty()) return task
        if (!task.hasSteps) return task.copy(steps = newSteps, updatedAt = now)
        val index = task.currentStepIndex.takeIf { it >= 0 } ?: return task
        val steps = task.steps.toMutableList().apply {
            removeAt(index)
            addAll(index, newSteps)
        }.take(Limits.STEPS_MAX)
        return task.copy(steps = steps, updatedAt = now)
    }

    fun paused(task: Task, now: Long = Time.nowMillis()): Task = task.copy(pausedAt = now, updatedAt = now)

    fun skipped(task: Task, now: Long = Time.nowMillis()): Task = task.copy(skippedAt = now, updatedAt = now)
}
