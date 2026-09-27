package com.uxankit.stepwise.data.model

data class UserProfile(
    val onboarded: Boolean = false,
    val createdAt: Long? = null,
    val lastActiveAt: Long? = null,
    val settings: UserSettings = UserSettings(),
)

/** Synced through Firestore so the same comfort settings follow the user to every device. */
data class UserSettings(
    val aiSuggestions: Boolean = true,
    val reduceMotion: Boolean = false,
    val showProgressBars: Boolean = true,
    val showStepDoneScreen: Boolean = true,
    val haptics: Boolean = true,
    val higherContrast: Boolean = false,
)
