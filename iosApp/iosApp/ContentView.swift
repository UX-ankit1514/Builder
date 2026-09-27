import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(googleSignIn: GoogleSignInBridge())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose draws edge to edge and handles safe areas and the keyboard itself.
        ComposeView()
            .ignoresSafeArea()
    }
}
