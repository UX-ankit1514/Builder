package com.uxankit.stepwise.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uxankit.stepwise.data.AppGraph
import com.uxankit.stepwise.data.GoogleTokens
import com.uxankit.stepwise.data.SignInOutcome
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

/** Sign in, guest start, sign out and delete. Shows one calm error line on failure. */
class AccountViewModel : ViewModel() {
    private val accounts = AppGraph.accounts

    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun startAsGuest() = run(authError = "Couldn’t get started right now. Please try again in a moment.") {
        accounts.startAsGuest()
    }

    fun signInWithGoogle(tokens: GoogleTokens, completeOnboarding: Boolean, onDone: (SignInOutcome) -> Unit = {}) = run(
        authError = "Google sign-in didn’t work. Please try again.",
    ) {
        val outcome = accounts.signInWithGoogle(tokens, completeOnboarding)
        when (outcome) {
            SignInOutcome.Linked -> AppGraph.messages.show("You’re signed in. Your tasks are backed up.")
            is SignInOutcome.SwitchedAccount -> if (outcome.movedTasks > 0) {
                AppGraph.messages.show(
                    if (outcome.movedTasks == 1) "Welcome back. We added your new task to your account."
                    else "Welcome back. We added your ${outcome.movedTasks} new tasks to your account.",
                )
            }
            SignInOutcome.SignedIn -> Unit
        }
        onDone(outcome)
    }

    fun signOut(clearGoogle: suspend () -> Unit) = run {
        runCatching { clearGoogle() }
        accounts.signOut()
    }

    fun deleteAccount(freshTokens: GoogleTokens?, clearGoogle: suspend () -> Unit) = run {
        accounts.deleteAccount(freshTokens)
        runCatching { clearGoogle() }
    }

    fun showError(message: String) {
        error = message
    }

    /** [authError] is shown for Firebase Auth failures, so each action explains itself. */
    private fun run(authError: String = "That didn’t work. Please try again.", block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: TimeoutCancellationException) {
                error = "This is taking too long. Check your connection and try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseNetworkException) {
                error = "You’re offline. Connect to the internet and try again."
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                error = "For your safety, please sign in with Google again, then retry."
            } catch (e: FirebaseAuthException) {
                error = authError
            } catch (e: Throwable) {
                error = "Something went wrong. Please try again."
            } finally {
                busy = false
            }
        }
    }
}
