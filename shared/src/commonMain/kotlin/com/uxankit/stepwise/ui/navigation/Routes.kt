package com.uxankit.stepwise.ui.navigation

import kotlinx.serialization.Serializable

// Flow 1 · Onboarding
@Serializable data object WelcomeRoute
@Serializable data object FirstTaskRoute
@Serializable data class FirstStepsRoute(val taskId: String)
@Serializable data class SignInRoute(val fromOnboarding: Boolean)

// Tabs: Today | Inbox | Progress
@Serializable data object HomeRoute

// Screens that open from the tabs
@Serializable data class TaskRoute(val taskId: String)
@Serializable data class BreakDownRoute(val taskId: String)
@Serializable data class FocusRoute(val taskId: String, val onboarding: Boolean = false)

// Flow 8 · Settings
@Serializable data object SettingsRoute
@Serializable data object AccessibilityRoute
@Serializable data class AccountRoute(val openDelete: Boolean = false)
