package com.uxankit.stepwise.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.uxankit.stepwise.data.GoogleTokens
import com.uxankit.stepwise.data.SessionUser
import com.uxankit.stepwise.platform.GoogleSignInResult
import com.uxankit.stepwise.platform.LocalPlatform
import kotlinx.coroutines.launch

/** The signed-in user for everything below the NavHost. Null only on the Welcome screen. */
val LocalSessionUser = staticCompositionLocalOf<SessionUser?> { null }

@Composable
fun requireUser(): SessionUser = LocalSessionUser.current ?: error("This screen needs a signed-in user")

/**
 * Opens the native Google picker from the UI (it needs the current Activity /
 * view controller) and hands the tokens to [onTokens].
 */
@Composable
fun rememberGoogleSignIn(onError: (String) -> Unit, onTokens: (GoogleTokens) -> Unit): () -> Unit {
    val launcher = LocalPlatform.current.googleSignIn
    val scope = rememberCoroutineScope()
    val currentOnTokens by rememberUpdatedState(onTokens)
    val currentOnError by rememberUpdatedState(onError)
    return remember(launcher, scope) {
        {
            scope.launch {
                when (val result = launcher.signIn()) {
                    is GoogleSignInResult.Success -> currentOnTokens(result.tokens)
                    is GoogleSignInResult.Failure -> currentOnError(result.message)
                    GoogleSignInResult.Cancelled -> Unit
                }
            }
        }
    }
}
