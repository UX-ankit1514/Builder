package com.uxankit.stepwise.platform

import androidx.compose.runtime.staticCompositionLocalOf
import com.uxankit.stepwise.data.GoogleTokens

sealed interface GoogleSignInResult {
    data class Success(val tokens: GoogleTokens) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

/** Shows the native Google account picker. Android: Credential Manager. iOS: GoogleSignIn SDK. */
interface GoogleSignInLauncher {
    suspend fun signIn(): GoogleSignInResult

    /** Clears the remembered Google account so the picker shows again next time. */
    suspend fun signOut()
}

/** Hands a file to the system share sheet (save to Files, email, Drive, ...). */
interface FileSharer {
    fun share(fileName: String, mimeType: String, content: String)
}

class Platform(
    val googleSignIn: GoogleSignInLauncher,
    val fileSharer: FileSharer,
)

val LocalPlatform = staticCompositionLocalOf<Platform> { error("Platform not provided") }
