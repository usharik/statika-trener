import SwiftUI
import UIKit
import Shared

/// Společné Compose Multiplatform UI (`StatikaApp` v shared) v SwiftUI.
struct ContentView: UIViewControllerRepresentable {
    let app: IosAppGraph

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(app: app)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
