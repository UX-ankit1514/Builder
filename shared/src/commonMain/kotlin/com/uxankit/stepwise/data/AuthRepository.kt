package com.uxankit.stepwise.data

import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionUser(
    val uid: String,
    /** A silent Firebase anonymous account, used until the user signs in with Google. */
    val isGuest: Boolean,
    val name: String?,
    val email: String?,
) {
    val firstName: String? get() = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: SessionUser) : SessionState
}

/** Tokens from the platform Google sign-in sheet (Credential Manager / GoogleSignIn-iOS). */
data class GoogleTokens(val idToken: String, val accessToken: String?)

class AuthRepository(private val auth: FirebaseAuth, scope: CoroutineScope) {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    init {
        scope.launch {
            auth.authStateChanged.collect { publish(it) }
        }
    }

    val currentUid: String? get() = auth.currentUser?.uid

    /** Linking a provider does not fire an auth-state event, so refresh by hand. */
    fun refresh() = publish(auth.currentUser)

    suspend fun startAsGuest(): String = auth.signInAnonymously().user?.uid
        ?: error("Anonymous sign-in returned no user")

    fun credential(tokens: GoogleTokens): AuthCredential =
        GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken)

    suspend fun signIn(credential: AuthCredential): String {
        val uid = auth.signInWithCredential(credential).user?.uid ?: error("Google sign-in returned no user")
        refresh()
        return uid
    }

    /** Upgrades the guest account in place, keeping its uid and data. */
    suspend fun linkGuest(credential: AuthCredential) {
        val user = auth.currentUser ?: error("No guest session to link")
        user.linkWithCredential(credential)
        runCatching { user.reload() }
        refresh()
    }

    suspend fun reauthenticate(credential: AuthCredential) {
        auth.currentUser?.reauthenticate(credential)
    }

    suspend fun deleteUser() {
        auth.currentUser?.delete()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private fun publish(user: FirebaseUser?) {
        _session.value = if (user == null) SessionState.SignedOut else SessionState.SignedIn(user.toSessionUser())
    }

    private fun FirebaseUser.toSessionUser() = SessionUser(
        uid = uid,
        isGuest = isAnonymous,
        name = displayName ?: providerData.firstNotNullOfOrNull { it.displayName },
        email = email ?: providerData.firstNotNullOfOrNull { it.email },
    )
}
