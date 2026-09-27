package com.uxankit.stepwise.data.firestore

import com.uxankit.stepwise.data.model.Step
import com.uxankit.stepwise.data.model.Task
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.data.model.UserProfile
import com.uxankit.stepwise.data.model.UserSettings
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/*
 * Firestore document shapes. Field names here must match firestore.rules,
 * which rejects any document with unknown fields.
 *
 *   users/{uid}                 -> ProfileDoc
 *   users/{uid}/tasks/{taskId}  -> TaskDoc (steps are embedded)
 */

@Serializable
internal data class StepDoc(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

@Serializable
internal data class TaskDoc(
    val title: String,
    val notes: String = "",
    /** ISO date, e.g. 2026-10-02. */
    val deadline: String? = null,
    val list: String = TaskList.Inbox.key,
    val steps: List<StepDoc> = emptyList(),
    val pausedAt: Long? = null,
    val skippedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Double,
    val plannedAt: Long? = null,
)

@Serializable
internal data class SettingsDoc(
    val aiSuggestions: Boolean = true,
    val reduceMotion: Boolean = false,
    val showProgressBars: Boolean = true,
    val showStepDoneScreen: Boolean = true,
    val haptics: Boolean = true,
    val higherContrast: Boolean = false,
)

@Serializable
internal data class ProfileDoc(
    val onboarded: Boolean = false,
    val createdAt: Long? = null,
    val lastActiveAt: Long? = null,
    val settings: SettingsDoc = SettingsDoc(),
)

internal fun TaskDoc.toTask(id: String) = Task(
    id = id,
    title = title,
    notes = notes,
    deadline = deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    list = TaskList.fromKey(list),
    steps = steps.map { Step(it.id, it.title, it.done, it.doneAt) },
    pausedAt = pausedAt,
    skippedAt = skippedAt,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    plannedAt = plannedAt,
)

internal fun Task.toDoc() = TaskDoc(
    title = title,
    notes = notes,
    deadline = deadline?.toString(),
    list = list.key,
    steps = steps.map { StepDoc(it.id, it.title, it.done, it.doneAt) },
    pausedAt = pausedAt,
    skippedAt = skippedAt,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    plannedAt = plannedAt,
)

internal fun ProfileDoc.toProfile() = UserProfile(
    onboarded = onboarded,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    settings = settings.toSettings(),
)

internal fun SettingsDoc.toSettings() = UserSettings(
    aiSuggestions, reduceMotion, showProgressBars, showStepDoneScreen, haptics, higherContrast,
)

internal fun UserSettings.toDoc() = SettingsDoc(
    aiSuggestions, reduceMotion, showProgressBars, showStepDoneScreen, haptics, higherContrast,
)
