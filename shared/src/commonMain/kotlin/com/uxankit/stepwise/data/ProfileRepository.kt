package com.uxankit.stepwise.data

import com.uxankit.stepwise.data.firestore.ProfileDoc
import com.uxankit.stepwise.data.firestore.SettingsDoc
import com.uxankit.stepwise.data.firestore.toDoc
import com.uxankit.stepwise.data.firestore.toProfile
import com.uxankit.stepwise.data.model.UserProfile
import com.uxankit.stepwise.data.model.UserSettings
import com.uxankit.stepwise.util.Time
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class ProfileSnapshot(val profile: UserProfile?, val fromCache: Boolean)

/** The user document at users/{uid}: onboarding state and synced settings. */
class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val scope: CoroutineScope,
    private val messages: Messages,
) {
    private fun ref(uid: String): DocumentReference = firestore.collection("users").document(uid)

    fun observe(uid: String): Flow<ProfileSnapshot> =
        ref(uid).snapshots(includeMetadataChanges = true)
            .map { snapshot ->
                ProfileSnapshot(
                    profile = if (snapshot.exists) {
                        runCatching { snapshot.data(ProfileDoc.serializer()).toProfile() }.getOrNull()
                    } else {
                        null
                    },
                    fromCache = snapshot.metadata.isFromCache,
                )
            }
            .catch { emit(ProfileSnapshot(null, fromCache = true)) }

    /** Only for a brand-new guest account, which has no document yet. */
    fun createForGuest(uid: String) {
        val now = Time.nowMillis()
        write { ref(uid).set(ProfileDoc.serializer(), ProfileDoc(onboarded = false, createdAt = now, lastActiveAt = now)) }
    }

    fun touch(uid: String) {
        write { ref(uid).set(TouchPatch.serializer(), TouchPatch(Time.nowMillis()), merge = true) }
    }

    fun markOnboarded(uid: String) {
        write { ref(uid).set(OnboardedPatch.serializer(), OnboardedPatch(true), merge = true) }
    }

    fun updateSettings(uid: String, settings: UserSettings) {
        write { ref(uid).set(SettingsPatch.serializer(), SettingsPatch(settings.toDoc()), merge = true) }
    }

    suspend fun delete(uid: String) {
        ref(uid).delete()
    }

    private fun write(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure { messages.show("Couldn’t save your settings. Please try again.") }
        }
    }

    @Serializable
    private data class TouchPatch(val lastActiveAt: Long)

    @Serializable
    private data class OnboardedPatch(val onboarded: Boolean)

    @Serializable
    private data class SettingsPatch(val settings: SettingsDoc)
}
