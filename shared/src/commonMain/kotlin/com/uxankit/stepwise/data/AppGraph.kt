package com.uxankit.stepwise.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * App-wide singletons. Firebase must be configured before first access:
 * Android does it automatically (google-services plugin), iOS calls
 * FirebaseApp.configure() in iOSApp.swift.
 */
object AppGraph {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val messages = Messages()

    val auth: AuthRepository by lazy { AuthRepository(Firebase.auth, scope) }
    val tasks: TaskRepository by lazy { TaskRepository(Firebase.firestore, scope, messages) }
    val profiles: ProfileRepository by lazy { ProfileRepository(Firebase.firestore, scope, messages) }
    val accounts: AccountService by lazy { AccountService(auth, tasks, profiles) }
}
