package com.uxankit.stepwise.data

import com.uxankit.stepwise.data.firestore.TaskDoc
import com.uxankit.stepwise.data.firestore.toDoc
import com.uxankit.stepwise.util.Time
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface SignInOutcome {
    /** Signed in from the Welcome screen, with no guest data involved. */
    data object SignedIn : SignInOutcome

    /** The guest account was upgraded in place. Same uid, all data kept. */
    data object Linked : SignInOutcome

    /** The Google account already had Stepwise data. The guest's tasks were copied into it. */
    data class SwitchedAccount(val movedTasks: Int) : SignInOutcome
}

/** Account-level flows that touch both Firebase Auth and Firestore. */
class AccountService(
    private val auth: AuthRepository,
    private val tasks: TaskRepository,
    private val profiles: ProfileRepository,
) {
    /** "Get started": a silent guest account so tasks sync to Firestore right away (IA Flow 1). */
    suspend fun startAsGuest() {
        val uid = auth.startAsGuest()
        profiles.createForGuest(uid)
    }

    suspend fun signInWithGoogle(tokens: GoogleTokens, completeOnboarding: Boolean): SignInOutcome {
        val session = auth.session.value
        val guest = (session as? SessionState.SignedIn)?.user?.takeIf { it.isGuest }

        if (guest == null) {
            val uid = auth.signIn(auth.credential(tokens))
            if (completeOnboarding) profiles.markOnboarded(uid)
            return SignInOutcome.SignedIn
        }

        return try {
            auth.linkGuest(auth.credential(tokens))
            profiles.touch(guest.uid)
            if (completeOnboarding) profiles.markOnboarded(guest.uid)
            SignInOutcome.Linked
        } catch (_: FirebaseAuthUserCollisionException) {
            // This Google account is already a Stepwise user (e.g. a new phone).
            // Read the guest's tasks while we still can, then switch accounts and copy them over.
            val guestTasks = tasks.fetchAll(guest.uid)
            val uid = auth.signIn(auth.credential(tokens))
            tasks.importAll(uid, guestTasks)
            profiles.touch(uid)
            profiles.markOnboarded(uid)
            SignInOutcome.SwitchedAccount(guestTasks.size)
        }
    }

    /**
     * Deletes every task, the profile and the Firebase user.
     * Google users must pass fresh tokens: Firebase requires a recent sign-in to delete an account.
     */
    suspend fun deleteAccount(freshTokens: GoogleTokens?) {
        val uid = auth.currentUid ?: return
        withTimeout(30.seconds) {
            if (freshTokens != null) auth.reauthenticate(auth.credential(freshTokens))
            tasks.deleteAll(uid)
            profiles.delete(uid)
            try {
                auth.deleteUser()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                // A guest cannot re-authenticate. Its data is already gone, so just sign out.
                if (freshTokens != null) throw e
                auth.signOut()
            }
        }
        auth.refresh()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun exportJson(uid: String): String {
        val all = tasks.fetchAll(uid)
        val file = ExportFile(
            exportedAt = Instant.fromEpochMilliseconds(Time.nowMillis()).toString(),
            tasks = all.map { ExportedTask(it.id, it.toDoc()) },
        )
        return exportJson.encodeToString(ExportFile.serializer(), file)
    }

    @Serializable
    private data class ExportFile(
        val app: String = "Stepwise",
        val formatVersion: Int = 1,
        val exportedAt: String,
        val tasks: List<ExportedTask>,
    )

    @Serializable
    private data class ExportedTask(val id: String, val task: TaskDoc)

    private companion object {
        val exportJson = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
