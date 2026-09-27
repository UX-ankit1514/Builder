package com.uxankit.stepwise.data

import com.uxankit.stepwise.data.firestore.TaskDoc
import com.uxankit.stepwise.data.firestore.toDoc
import com.uxankit.stepwise.data.firestore.toTask
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.util.Time
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tasks live at users/{uid}/tasks/{taskId}.
 *
 * Writes are fire-and-forget on purpose: Firestore applies them to the local
 * cache at once (so the UI updates instantly, even offline) and syncs later.
 * Awaiting a write would hang until the device is back online.
 */
class TaskRepository(
    private val firestore: FirebaseFirestore,
    private val scope: CoroutineScope,
    private val messages: Messages,
) {
    private var cachedUid: String? = null
    private var cachedTasks: StateFlow<List<Task>?> = MutableStateFlow(null)

    private fun tasksRef(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("tasks")

    /** One shared listener per signed-in user. `null` until the first snapshot arrives. */
    fun tasks(uid: String): StateFlow<List<Task>?> {
        if (uid != cachedUid) {
            cachedUid = uid
            cachedTasks = tasksRef(uid).snapshots
                .map { snapshot -> snapshot.documents.mapNotNull { it.toTaskOrNull() } }
                .catch { emit(emptyList()) }
                .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
        }
        return cachedTasks
    }

    fun task(uid: String, taskId: String): Flow<Task?> =
        tasksRef(uid).document(taskId).snapshots
            .map { if (it.exists) it.toTaskOrNull() else null }
            .catch { emit(null) }

    fun create(uid: String, title: String, list: TaskList = TaskList.Inbox): Task {
        val now = Time.nowMillis()
        val task = Task(
            id = tasksRef(uid).document.id,
            title = title.trim().take(Limits.TITLE_MAX),
            list = list,
            createdAt = now,
            updatedAt = now,
            sortOrder = now.toDouble(),
            plannedAt = if (list == TaskList.Inbox) null else now,
        )
        save(uid, task)
        return task
    }

    fun save(uid: String, task: Task) {
        val safe = task.copy(
            title = task.title.take(Limits.TITLE_MAX),
            notes = task.notes.take(Limits.NOTES_MAX),
            steps = task.steps.take(Limits.STEPS_MAX),
        )
        write { tasksRef(uid).document(safe.id).set(TaskDoc.serializer(), safe.toDoc()) }
    }

    fun delete(uid: String, taskId: String) {
        write { tasksRef(uid).document(taskId).delete() }
    }

    /** Reads everything once, e.g. for export or for moving a guest's tasks to an existing account. */
    suspend fun fetchAll(uid: String): List<Task> =
        tasksRef(uid).get().documents.mapNotNull { it.toTaskOrNull() }

    /** Awaited, because account deletion must not report success before the server confirms. */
    suspend fun deleteAll(uid: String) {
        val ids = tasksRef(uid).get().documents.map { it.id }
        ids.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(tasksRef(uid).document(it)) }
            batch.commit()
        }
    }

    fun importAll(uid: String, tasks: List<Task>) {
        tasks.chunked(400).forEach { chunk ->
            write {
                val batch = firestore.batch()
                chunk.forEach { batch.set(tasksRef(uid).document(it.id), TaskDoc.serializer(), it.toDoc()) }
                batch.commit()
            }
        }
    }

    private fun write(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure {
                messages.show("Couldn’t save that change. Please try again.")
            }
        }
    }

    private fun DocumentSnapshot.toTaskOrNull(): Task? =
        runCatching { data(TaskDoc.serializer()).toTask(id) }.getOrNull()
}
