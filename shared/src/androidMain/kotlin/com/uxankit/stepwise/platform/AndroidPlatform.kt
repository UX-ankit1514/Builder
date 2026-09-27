package com.uxankit.stepwise.platform

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.uxankit.stepwise.data.GoogleTokens
import java.io.File

fun androidPlatform(activity: Activity, webClientId: String?): Platform = Platform(
    googleSignIn = AndroidGoogleSignIn(activity, webClientId),
    fileSharer = AndroidFileSharer(activity),
)

/**
 * Google sign-in with Credential Manager. [webClientId] is the OAuth *web* client ID that
 * the google-services plugin generates from google-services.json (default_web_client_id).
 */
private class AndroidGoogleSignIn(
    private val activity: Activity,
    private val webClientId: String?,
) : GoogleSignInLauncher {
    private val credentialManager = CredentialManager.create(activity)

    override suspend fun signIn(): GoogleSignInResult {
        val clientId = webClientId
            ?: return GoogleSignInResult.Failure(
                "Google sign-in isn’t set up yet. Enable Google in Firebase Authentication, then download google-services.json again.",
            )
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = clientId).build())
            .build()
        return try {
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(GoogleTokens(idToken = google.idToken, accessToken = null))
            } else {
                GoogleSignInResult.Failure("Google sign-in returned an unexpected credential.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failure("No Google account found on this device. Add one in Settings, then try again.")
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure("Google sign-in didn’t work. Please try again.")
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure("Google sign-in didn’t work. Please try again.")
        }
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

/** Writes the export into the app cache and opens the Android share sheet. */
private class AndroidFileSharer(private val activity: Activity) : FileSharer {
    override fun share(fileName: String, mimeType: String, content: String) {
        val dir = File(activity.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.exports", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Stepwise export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, "Export your Stepwise data"))
    }
}
