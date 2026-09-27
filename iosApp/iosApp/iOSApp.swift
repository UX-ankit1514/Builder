import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct iOSApp: App {
    init() {
        // Reads GoogleService-Info.plist (gitignored; see README → Firebase setup).
        FirebaseApp.configure()
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
