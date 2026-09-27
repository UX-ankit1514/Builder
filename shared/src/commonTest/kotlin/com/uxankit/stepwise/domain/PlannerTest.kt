package com.uxankit.stepwise.domain

import com.uxankit.stepwise.data.model.Step
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.util.Time
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlannerTest {
    private val now = Time.nowMillis()

    private fun task(
        id: String,
        list: TaskList = TaskList.Inbox,
        steps: List<Step> = emptyList(),
        sortOrder: Double = 0.0,
        completedAt: Long? = null,
        pausedAt: Long? = null,
        skippedAt: Long? = null,
        createdAt: Long = now,
    ) = Task(
        id = id,
        title = "Task $id",
        list = list,
        steps = steps,
        createdAt = createdAt,
        updatedAt = now,
        sortOrder = sortOrder,
        completedAt = completedAt,
        pausedAt = pausedAt,
        skippedAt = skippedAt,
    )

    private fun steps(vararg done: Boolean) = done.mapIndexed { i, d -> Step("s$i", "Step $i", d, if (d) now else null) }

    @Test
    fun listsExcludeDoneTasksAndKeepOrder() {
        val tasks = listOf(
            task("a", TaskList.Today, sortOrder = 2.0),
            task("b", TaskList.Today, sortOrder = 1.0),
            task("c", TaskList.Today, completedAt = now),
            task("d", TaskList.Urgent),
        )
        assertEquals(listOf("b", "a"), Planner.today(tasks).map { it.id })
        assertEquals(listOf("d"), Planner.urgent(tasks).map { it.id })
        assertEquals(2, Planner.count(tasks, TaskList.Today))
    }

    @Test
    fun inboxIsNewestFirst() {
        val tasks = listOf(task("old", createdAt = now - 10_000), task("new", createdAt = now))
        assertEquals(listOf("new", "old"), Planner.inbox(tasks).map { it.id })
    }

    @Test
    fun completingStepsAdvancesAndFinishesTheTask() {
        var t = task("a", steps = steps(false, false))
        assertEquals(0, t.currentStepIndex)

        t = Planner.completeCurrentStep(t, now)
        assertEquals(1, t.currentStepIndex)
        assertFalse(t.isDone)

        t = Planner.completeCurrentStep(t, now)
        assertTrue(t.isDone)
        assertEquals(-1, t.currentStepIndex)
    }

    @Test
    fun taskWithoutStepsIsDoneInOneTap() {
        val t = Planner.completeCurrentStep(task("a"), now)
        assertTrue(t.isDone)
    }

    @Test
    fun makeItSmallerReplacesTheCurrentStepInPlace() {
        val t = task("a", steps = steps(true, false, false))
        val split = Planner.splitCurrentStep(t, listOf("tiny 1", " ", "tiny 2"), now)
        assertEquals(listOf("Step 0", "tiny 1", "tiny 2", "Step 2"), split.steps.map { it.title })
        assertEquals("tiny 1", split.currentStep?.title)
    }

    @Test
    fun editingStepsKeepsDoneStateForKeptSteps() {
        val t = task("a", steps = steps(true, false))
        val edited = Planner.withSteps(t, listOf("s0" to "Renamed", null to "Brand new", null to "   "), now)
        assertEquals(listOf("Renamed", "Brand new"), edited.steps.map { it.title })
        assertTrue(edited.steps[0].done)
        assertFalse(edited.steps[1].done)
    }

    @Test
    fun skippedTasksGoToTheBackOfTheFocusQueue() {
        val tasks = listOf(
            task("urgent", TaskList.Urgent, skippedAt = now),
            task("today1", TaskList.Today, sortOrder = 1.0),
            task("today2", TaskList.Today, sortOrder = 2.0),
            task("inbox", TaskList.Inbox),
        )
        assertEquals(listOf("today1", "today2", "urgent"), Planner.focusQueue(tasks).map { it.id })
        assertEquals("today2", Planner.nextInQueue(tasks, "today1")?.id)
    }

    @Test
    fun continueIsTheMostRecentlyPausedOpenTask() {
        val tasks = listOf(
            task("older", pausedAt = now - 1000),
            task("newer", pausedAt = now),
            task("finished", pausedAt = now + 1000, completedAt = now),
        )
        assertEquals("newer", Planner.continueTask(tasks)?.id)
        assertNull(Planner.continueTask(listOf(task("x"))))
    }

    @Test
    fun movingToInboxClearsThePlannedDate() {
        val planned = Planner.moveTo(task("a"), TaskList.Today, now)
        assertNotNull(planned.plannedAt)
        assertNull(Planner.moveTo(planned, TaskList.Inbox, now).plannedAt)
    }

    @Test
    fun stepsDoneTodayCountSingleStepTasksToo() {
        val today = Time.today()
        val tasks = listOf(
            task("steps", steps = steps(true, true, false)),
            task("single", completedAt = now),
        )
        assertEquals(3, Planner.stepsDoneOn(tasks, today))
    }

    @Test
    fun softLimitsAreThreeUrgentAndFiveToday() {
        assertEquals(3, Planner.limitOf(TaskList.Urgent))
        assertEquals(5, Planner.limitOf(TaskList.Today))
        assertNull(Planner.limitOf(TaskList.Inbox))
    }
}
