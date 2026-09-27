import UIKit
import GoogleSignIn
import Shared

/// Google sign-in for the shared Kotlin code. Kotlin only receives the tokens and
/// signs in to Firebase with them; the GoogleSignIn SDK stays on the Swift side.
final class GoogleSignInBridge: NSObject, IosGoogleSignInProvider {

    func signIn(callback: IosGoogleSignInCallback) {
        guard GIDSignIn.sharedInstance.configuration != nil else {
            callback.onError(message: "Google sign-in isn’t set up. Enable Google in Firebase Authentication, then download GoogleService-Info.plist again.")
            return
        }
        guard let presenter = Self.topViewController() else {
            callback.onError(message: "Couldn’t open Google sign-in. Please try again.")
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error {
                if (error as? GIDSignInError)?.code == .canceled {
                    callback.onCancel()
                } else {
                    callback.onError(message: "Google sign-in didn’t work. Please try again.")
                }
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                callback.onError(message: "Google sign-in didn’t return a token. Please try again.")
                return
            }
            callback.onSuccess(idToken: idToken, accessToken: user.accessToken.tokenString)
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first(where: \.isKeyWindow) ?? scenes.first?.windows.first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
