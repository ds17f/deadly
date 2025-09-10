import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    init() {
        // Initialize Koin DI before any Compose UI is created
        IOSKoinInitKt.doInitKoin()
    }
    
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}



