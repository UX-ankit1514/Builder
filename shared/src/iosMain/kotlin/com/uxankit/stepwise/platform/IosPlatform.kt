package com.uxankit.stepwise.platform

import com.uxankit.stepwise.data.GoogleTokens
import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/** Implemented in Swift (GoogleSignInBridge.swift) with the GoogleSignIn-iOS SDK. */
interface IosGoogleSignInCallback {
    fun onSuccess(idToken: String, accessToken: String?)
    fun onCancel()
    fun onError(message: String)
}

interface IosGoogleSignInProvider {
    fun signIn(callback: IosGoogleSignInCallback)
    fun signOut()
}

internal class IosGoogleSignIn(private val provider: IosGoogleSignInProvider) : GoogleSignInLauncher {
    override suspend fun signIn(): GoogleSignInResult = suspendCancellableCoroutine { continuation ->
        provider.signIn(object : IosGoogleSignInCallback {
            override fun onSuccess(idToken: String, accessToken: String?) {
                if (continuation.isActive) continuation.resume(GoogleSignInResult.Success(GoogleTokens(idToken, accessToken)))
            }

            override fun onCancel() {
                if (continuation.isActive) continuation.resume(GoogleSignInResult.Cancelled)
            }

            override fun onError(message: String) {
                if (continuation.isActive) continuation.resume(GoogleSignInResult.Failure(message))
            }
        })
    }

    override suspend fun signOut() = provider.signOut()
}

/** Writes the export to the temp folder and opens the iOS share sheet. */
internal class IosFileSharer : FileSharer {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun share(fileName: String, mimeType: String, content: String) {
        val path = NSTemporaryDirectory() + fileName
        NSString.create(string = content).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        val presenter = topViewController() ?: return
        val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        sheet.popoverPresentationController?.sourceView = presenter.view
        presenter.presentViewController(sheet, animated = true, completion = null)
    }

    @Suppress("DEPRECATION")
    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}
