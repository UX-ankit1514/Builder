package com.uxankit.stepwise.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.model.Limits
import com.uxankit.stepwise.data.model.TaskList
import com.uxankit.stepwise.domain.Planner
import com.uxankit.stepwise.sound.UiSound
import com.uxankit.stepwise.ui.account.AccountViewModel
import com.uxankit.stepwise.ui.components.BrandMark
import com.uxankit.stepwise.ui.components.Chip
import com.uxankit.stepwise.ui.components.CircleIconButton
import com.uxankit.stepwise.ui.components.ErrorNote
import com.uxankit.stepwise.ui.components.HeroText
import com.uxankit.stepwise.ui.components.PageDots
import com.uxankit.stepwise.ui.components.PrimaryCta
import com.uxankit.stepwise.ui.components.ScreenScaffold
import com.uxankit.stepwise.ui.components.ServiceCta
import com.uxankit.stepwise.ui.components.StepsIllustration
import com.uxankit.stepwise.ui.components.StepwiseTextField
import com.uxankit.stepwise.ui.components.TextLink
import com.uxankit.stepwise.ui.components.pressClickable
import com.uxankit.stepwise.ui.components.reveal
import com.uxankit.stepwise.ui.rememberGoogleSignIn
import com.uxankit.stepwise.ui.requireUser
import com.uxankit.stepwise.ui.task.EditableStep
import com.uxankit.stepwise.ui.task.StepsEditor
import com.uxankit.stepwise.ui.task.toStepPairs
import com.uxankit.stepwise.ui.task.withDraft
import com.uxankit.stepwise.ui.theme.Motion
import com.uxankit.stepwise.ui.theme.StepIcons
import com.uxankit.stepwise.ui.theme.StepwiseTheme
import kotlinx.coroutines.flow.map

/** 1.1 Welcome. "Get started" creates a silent guest account; "Log in" uses Google. */
@Composable
fun WelcomeScreen() {
    val vm = viewModel { AccountViewModel() }
    val signIn = rememberGoogleSignIn(onError = vm::showError) { tokens ->
        vm.signInWithGoogle(tokens, completeOnboarding = false)
    }
    val colors = StepwiseTheme.colors
    ScreenScaffold(
        bottom = {
            ErrorNote(vm.error)
            ServiceCta(text = "Get started", onClick = vm::startAsGuest, busy = vm.busy)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", style = StepwiseTheme.type.body, color = colors.muted)
                TextLink("Log in", onClick = { if (!vm.busy) signIn() })
            }
        },
    ) {
        // First impression: brand, illustration, headline and promise rise in one after another.
        BrandMark(Modifier.padding(top = 8.dp, start = 4.dp).reveal(0, Motion.MICRO))
        StepsIllustration(Modifier.fillMaxWidth(0.77f).align(Alignment.CenterHorizontally).padding(top = 24.dp).reveal(1, Motion.MICRO))
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroText("Stop organising. Start the next thing.", Modifier.reveal(2, Motion.MICRO))
            Text(
                "Stepwise turns what you’ve been avoiding into small steps — one at a time.",
                style = StepwiseTheme.type.bodyLarge,
                color = colors.muted,
                modifier = Modifier.reveal(3, Motion.MICRO),
            )
        }
    }
}

private val suggestions = listOf("File my taxes", "Call the dentist", "Clean the flat")

/** 1.2 "What have you been putting off?" The task goes straight to Today. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstTaskScreen(onNext: (String) -> Unit) {
    val user = requireUser()
    val colors = StepwiseTheme.colors
    var title by rememberSaveable { mutableStateOf("") }
    var createdId by rememberSaveable { mutableStateOf<String?>(null) }

    fun next() {
        if (title.isBlank()) return
        val repo = AppGraph.tasks
        val existing = createdId?.let { id -> repo.tasks(user.uid).value?.firstOrNull { it.id == id } }
        val id = if (existing != null) {
            repo.save(user.uid, existing.copy(title = title.trim()))
            existing.id
        } else {
            repo.create(user.uid, title, TaskList.Today).id.also { createdId = it }
        }
        onNext(id)
    }

    ScreenScaffold(
        bottom = { ServiceCta(text = "Next", onClick = ::next, enabled = title.isNotBlank()) },
    ) {
        PageDots(3, 0, Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp))
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("What have you been putting off?", style = StepwiseTheme.type.title, color = colors.ink, modifier = Modifier.reveal(0))
            Text("Just one thing. You can add more later.", style = StepwiseTheme.type.body, color = colors.muted, modifier = Modifier.reveal(1))
        }
        StepwiseTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "Prepare portfolio for job application",
            maxLength = Limits.TITLE_MAX,
            onImeAction = ::next,
            accessibilityLabel = "The task you’ve been putting off",
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                Chip(
                    suggestion,
                    modifier = Modifier.pressClickable(RoundedCornerShape(10.dp), sound = UiSound.PressSoft) { title = suggestion },
                )
            }
        }
    }
}

/** 1.3 "Let's make it smaller": the first steps, then straight into Focus Mode. */
@Composable
fun FirstStepsScreen(taskId: String, onBack: () -> Unit, onStart: () -> Unit) {
    val user = requireUser()
    val colors = StepwiseTheme.colors
    val repo = AppGraph.tasks
    val task by remember(taskId) { repo.tasks(user.uid).map { list -> list?.firstOrNull { it.id == taskId } } }
        .collectAsState(initial = null)
    var steps by remember { mutableStateOf(emptyList<EditableStep>()) }
    var draft by remember { mutableStateOf("") }
    val all = steps.withDraft(draft)

    ScreenScaffold(
        bottom = {
            ServiceCta(
                text = "Start step 1",
                enabled = all.isNotEmpty() && task != null,
                onClick = {
                    val current = task ?: return@ServiceCta
                    repo.save(user.uid, Planner.withSteps(current, all.toStepPairs()))
                    onStart()
                },
            )
        },
    ) {
        Box(Modifier.fillMaxWidth()) {
            CircleIconButton(StepIcons.ChevronLeft, contentDescription = "Back", onClick = onBack, sound = UiSound.Navigate)
            PageDots(3, 1, Modifier.align(Alignment.Center))
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Let’s make it smaller", style = StepwiseTheme.type.title, color = colors.ink, modifier = Modifier.reveal(0))
            Text(
                "“${task?.title.orEmpty()}” — what’s the first tiny action?",
                style = StepwiseTheme.type.body,
                color = colors.muted,
                modifier = Modifier.reveal(1),
            )
        }
        StepsEditor(steps = steps, onChange = { steps = it }, draft = draft, onDraftChange = { draft = it }, showHint = false)
    }
}

/** 1.6 "Save your progress". Google is the only sign-in option for now. */
@Composable
fun SignInScreen(fromOnboarding: Boolean, onDone: () -> Unit, onNotNow: () -> Unit) {
    val vm = viewModel { AccountViewModel() }
    val colors = StepwiseTheme.colors
    val signIn = rememberGoogleSignIn(onError = vm::showError) { tokens ->
        vm.signInWithGoogle(tokens, completeOnboarding = fromOnboarding) { onDone() }
    }
    ScreenScaffold {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            TextLink("Not now", onClick = onNotNow)
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Save your progress", style = StepwiseTheme.type.title, color = colors.ink, modifier = Modifier.reveal(0))
            Text(
                "Sign in to keep your tasks and steps synced. You can keep using Stepwise without an account.",
                style = StepwiseTheme.type.body,
                color = colors.muted,
                modifier = Modifier.reveal(1),
            )
        }
        PrimaryCta(text = "Continue with Google", onClick = signIn, fillWidth = true, tall = true, busy = vm.busy)
        ErrorNote(vm.error)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(StepIcons.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Your tasks stay private.", style = StepwiseTheme.type.small, color = colors.muted)
        }
    }
}
