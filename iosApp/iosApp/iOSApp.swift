import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // After Google signs the player in, Safari opens com.mehmtcan.brainscroll://login-callback?code=...
                // and iOS brings the app back with that link. The shared code finishes the sign-in with it.
                .onOpenURL { url in
                    DeepLinkInbox.shared.deliver(url: url.absoluteString)
                }
        }
    }
}
