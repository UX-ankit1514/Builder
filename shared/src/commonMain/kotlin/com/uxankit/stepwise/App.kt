package com.uxankit.stepwise

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.SessionState
import com.uxankit.stepwise.data.SessionUser
import com.uxankit.stepwise.data.model.UserSettings
import com.uxankit.stepwise.platform.LocalPlatform
import com.uxankit.stepwise.platform.Platform
import com.uxankit.stepwise.ui.LocalSessionUser
import com.uxankit.stepwise.ui.focus.FocusScreen
import com.uxankit.stepwise.ui.home.HomeScreen
import com.uxankit.stepwise.ui.navigation.AccessibilityRoute
import com.uxankit.stepwise.ui.navigation.AccountRoute
import com.uxankit.stepwise.ui.navigation.BreakDownRoute
import com.uxankit.stepwise.ui.navigation.FirstStepsRoute
import com.uxankit.stepwise.ui.navigation.FirstTaskRoute
import com.uxankit.stepwise.ui.navigation.FocusRoute
import com.uxankit.stepwise.ui.navigation.HomeRoute
import com.uxankit.stepwise.ui.navigation.SettingsRoute
import com.uxankit.stepwise.ui.navigation.SignInRoute
import com.uxankit.stepwise.ui.navigation.TaskRoute
import com.uxankit.stepwise.ui.navigation.WelcomeRoute
import com.uxankit.stepwise.ui.onboarding.FirstStepsScreen
import com.uxankit.stepwise.ui.onboarding.FirstTaskScreen
import com.uxankit.stepwise.ui.onboarding.SignInScreen
import com.uxankit.stepwise.ui.onboarding.WelcomeScreen
import com.uxankit.stepwise.ui.settings.AccessibilityScreen
import com.uxankit.stepwise.ui.settings.AccountScreen
import com.uxankit.stepwise.ui.settings.SettingsScreen
import com.uxankit.stepwise.ui.task.BreakDownScreen
import com.uxankit.stepwise.ui.task.TaskScreen
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun App(platform: Platform) {
    CompositionLocalProvider(LocalPlatform provides platform) {
        val session by AppGraph.auth.session.collectAsState()
        when (val s = session) {
            SessionState.Loading -> StepwiseTheme { Splash() }
            SessionState.SignedOut -> key("signed-out") {
                StepwiseTheme { StepwiseNavHost(user = null, start = WelcomeRoute) }
            }
            // A new uid (guest created, account switched) rebuilds the whole graph.
            is SessionState.SignedIn -> key(s.user.uid) { SignedInRoot(s.user) }
        }
    }
}

@Composable
private fun SignedInRoot(user: SessionUser) {
    val profiles = remember(user.uid) { AppGraph.profiles.observe(user.uid) }
    val snapshot by profiles.collectAsState(initial = null)
    var start by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) {
        // Prefer the server's answer so a returning user on a new phone skips onboarding.
        val confirmed = withTimeoutOrNull(5_000) {
            profiles.first { !it.fromCache || (user.isGuest && it.profile != null) }
        }
        val onboarded = if (confirmed != null) {
            confirmed.profile?.onboarded ?: false
        } else {
            snapshot?.profile?.onboarded ?: !user.isGuest
        }
        start = if (onboarded) HomeRoute else FirstTaskRoute
        AppGraph.profiles.touch(user.uid)
    }

    StepwiseTheme(settings = snapshot?.profile?.settings ?: UserSettings()) {
        val destination = start
        if (destination == null) Splash() else StepwiseNavHost(user, destination)
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(StepwiseTheme.colors.canvas))
}

@Composable
private fun StepwiseNavHost(user: SessionUser?, start: Any) {
    val nav = rememberNavController()
    val reduceMotion = StepwiseTheme.settings.reduceMotion
    CompositionLocalProvider(LocalSessionUser provides user) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize().background(StepwiseTheme.colors.canvas),
            enterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(220)) },
            exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(180)) },
            popEnterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(220)) },
            popExitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(180)) },
        ) {
            // Flow 1 · Onboarding
            composable<WelcomeRoute> { WelcomeScreen() }
            composable<FirstTaskRoute> {
                FirstTaskScreen(onNext = { taskId -> nav.navigate(FirstStepsRoute(taskId)) })
            }
            composable<FirstStepsRoute> { entry ->
                val route = entry.toRoute<FirstStepsRoute>()
                FirstStepsScreen(
                    taskId = route.taskId,
                    onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(FocusRoute(route.taskId, onboarding = true)) },
                )
            }
            composable<SignInRoute> { entry ->
                val route = entry.toRoute<SignInRoute>()
                SignInScreen(
                    fromOnboarding = route.fromOnboarding,
                    onDone = { if (route.fromOnboarding) nav.goHome() else nav.popBackStack() },
                    onNotNow = {
                        if (route.fromOnboarding) {
                            user?.let { AppGraph.profiles.markOnboarded(it.uid) }
                            nav.goHome()
                        } else {
                            nav.popBackStack()
                        }
                    },
                )
            }

            // Tabs
            composable<HomeRoute> {
                HomeScreen(
                    onOpenTask = { nav.navigate(TaskRoute(it)) },
                    onStartFocus = { nav.navigate(FocusRoute(it)) },
                    onOpenSettings = { nav.navigate(SettingsRoute) },
                )
            }

            // Task Page + Break Down
            composable<TaskRoute> { entry ->
                val route = entry.toRoute<TaskRoute>()
                TaskScreen(
                    taskId = route.taskId,
                    onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(FocusRoute(route.taskId)) },
                    onBreakDown = { nav.navigate(BreakDownRoute(route.taskId)) },
                )
            }
            composable<BreakDownRoute> { entry ->
                BreakDownScreen(taskId = entry.toRoute<BreakDownRoute>().taskId, onClose = { nav.popBackStack() })
            }

            // Focus Mode
            composable<FocusRoute> { entry ->
                val route = entry.toRoute<FocusRoute>()
                FocusScreen(
                    taskId = route.taskId,
                    onboarding = route.onboarding,
                    onClose = { if (route.onboarding) nav.goHome() else nav.popBackStack() },
                    onOpenTask = { nav.navigate(TaskRoute(route.taskId)) },
                    onSwitchTask = { next ->
                        nav.navigate(FocusRoute(next)) { popUpTo<FocusRoute> { inclusive = true } }
                    },
                    onPaused = {
                        if (route.onboarding) nav.goHome() else nav.popBackStack()
                        AppGraph.messages.show(
                            "Paused. It’s waiting in Continue.",
                            actionLabel = "Resume",
                            action = { nav.navigate(FocusRoute(route.taskId)) },
                        )
                    },
                    onSaveProgress = { nav.navigate(SignInRoute(fromOnboarding = true)) },
                    onFinishOnboarding = { continueFocus ->
                        user?.let { AppGraph.profiles.markOnboarded(it.uid) }
                        nav.goHome()
                        if (continueFocus) nav.navigate(FocusRoute(route.taskId))
                    },
                )
            }

            // Flow 8 · Settings
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onAccessibility = { nav.navigate(AccessibilityRoute) },
                    onAccount = { openDelete -> nav.navigate(AccountRoute(openDelete)) },
                    onSignIn = { nav.navigate(SignInRoute(fromOnboarding = false)) },
                )
            }
            composable<AccessibilityRoute> { AccessibilityScreen(onBack = { nav.popBackStack() }) }
            composable<AccountRoute> { entry ->
                AccountScreen(
                    openDelete = entry.toRoute<AccountRoute>().openDelete,
                    onBack = { nav.popBackStack() },
                    onSignIn = { nav.navigate(SignInRoute(fromOnboarding = false)) },
                )
            }
        }
    }
}

/** Clears onboarding from the back stack and lands on the tabs. */
private fun NavHostController.goHome() {
    navigate(HomeRoute) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
