package com.uxankit.stepwise

import androidx.compose.ui.window.ComposeUIViewController
import com.uxankit.stepwise.platform.IosFileSharer
import com.uxankit.stepwise.platform.IosGoogleSignIn
import com.uxankit.stepwise.platform.IosGoogleSignInProvider
import com.uxankit.stepwise.platform.Platform
import platform.UIKit.UIViewController

/**
 * Called from Swift (ContentView.swift). Swift owns the GoogleSignIn SDK and passes it in
 * as [googleSignIn], so Kotlin never needs to bind to the GoogleSignIn Objective-C API.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(googleSignIn: IosGoogleSignInProvider): UIViewController = ComposeUIViewController {
    App(Platform(googleSignIn = IosGoogleSignIn(googleSignIn), fileSharer = IosFileSharer()))
}
