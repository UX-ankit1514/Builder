package com.uxankit.stepwise.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.ui.components.BottomBar
import com.uxankit.stepwise.ui.components.HomeTab
import com.uxankit.stepwise.ui.components.MessageHost
import com.uxankit.stepwise.ui.components.pageSlide
import com.uxankit.stepwise.ui.plan.PlanSheet
import com.uxankit.stepwise.ui.plan.RebalanceSheet
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepwiseTheme

@Composable
fun HomeScreen(
    onOpenTask: (String) -> Unit,
    onStartFocus: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val user = requireUser()
    val vm = viewModel { HomeViewModel(user.uid) }
    val tasks by vm.tasks.collectAsState()
    val rebalance by vm.planning.rebalance.collectAsState()
    var tab by rememberSaveable { mutableStateOf(HomeTab.Today) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var planTaskId by remember { mutableStateOf<String?>(null) }

    val reduceMotion = StepwiseTheme.settings.reduceMotion
    val slide = with(LocalDensity.current) { Motion.DistanceBase.roundToPx() }
    Box(Modifier.fillMaxSize().background(StepwiseTheme.colors.canvas)) {
        val all = tasks
        if (all == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = StepwiseTheme.colors.ink)
        } else {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // Tabs sit side by side: moving right slides the new tab in from the right.
                    if (reduceMotion) {
                        fadeIn(tween(Motion.QUICK)) togetherWith fadeOut(tween(Motion.QUICK))
                    } else {
                        pageSlide(forward = targetState.ordinal > initialState.ordinal, slide)
                    }
                },
                label = "home tab",
            ) { current ->
                when (current) {
                    HomeTab.Today -> TodayTab(
                        tasks = all,
                        user = user,
                        onProfile = onOpenSettings,
                        onStart = onStartFocus,
                        onOpenTask = onOpenTask,
                        onToggleDone = vm::setDone,
                        onPickFromInbox = { tab = HomeTab.Inbox },
                        onQuickAdd = { showQuickAdd = true },
                    )
                    HomeTab.Inbox -> InboxTab(
                        tasks = all,
                        onProfile = onOpenSettings,
                        onPlan = { planTaskId = it.id },
                        onToggleDone = vm::setDone,
                    )
                    HomeTab.Progress -> ProgressTab(tasks = all, onProfile = onOpenSettings, onOpenTask = onOpenTask)
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            MessageHost(Modifier.padding(horizontal = 20.dp))
            BottomBar(selected = tab, onSelect = { tab = it }, onAdd = { showQuickAdd = true })
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(onSave = vm::quickAdd, onDismiss = { showQuickAdd = false })
    }

    val planTask = planTaskId?.let { id -> tasks?.firstOrNull { it.id == id } }
    if (planTask != null) {
        PlanSheet(
            task = planTask,
            all = tasks.orEmpty(),
            onMove = { target ->
                planTaskId = null
                vm.move(planTask, target)
            },
            onOpen = {
                planTaskId = null
                onOpenTask(planTask.id)
            },
            onDelete = {
                planTaskId = null
                vm.delete(planTask)
            },
            onDismiss = { planTaskId = null },
        )
    }

    rebalance?.let { state ->
        RebalanceSheet(
            state = state,
            all = tasks.orEmpty(),
            onApply = vm::applyRebalance,
            onDismiss = vm.planning::dismissRebalance,
        )
    }
}
