import Cocoa
import WebKit

/// AppKit window for the same desktop page the Windows host opens.
/// The title bar and traffic lights belong to the system. GearUI fills the
/// content view. Build on a Mac, from this directory, with the staged site
/// next to the binary:
///
///   swiftc -O -o GearUIDesktop main.swift -framework Cocoa -framework WebKit
///   ./GearUIDesktop /path/to/www
final class AppDelegate: NSObject, NSApplicationDelegate, WKNavigationDelegate {
    private var window: NSWindow?

    func applicationDidFinishLaunching(_ notification: Notification) {
        let args = CommandLine.arguments
        let content = args.count > 1
            ? URL(fileURLWithPath: args[1], isDirectory: true)
            : URL(fileURLWithPath: "www", isDirectory: true)
        let index = content.appendingPathComponent("index.html")
        guard FileManager.default.fileExists(atPath: index.path) else {
            fputs("GearUI desktop content was not found at \(index.path)\n", stderr)
            NSApplication.shared.terminate(nil)
            return
        }

        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1280, height: 800),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )
        window.title = "GearUI"
        window.minSize = NSSize(width: 800, height: 600)
        window.center()
        window.setFrameAutosaveName("GearUIDesktop")

        let web = WKWebView(frame: window.contentView?.bounds ?? .zero)
        web.autoresizingMask = [.width, .height]
        web.navigationDelegate = self
        window.contentView?.addSubview(web)

        var components = URLComponents(url: index, resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "page_name", value: "DesktopHost"),
            URLQueryItem(name: "systemTitleBar", value: "1"),
            URLQueryItem(name: "lang", value: "zh-Hans"),
        ]
        if let url = components?.url {
            web.loadFileURL(url, allowingReadAccessTo: content)
        }
        window.makeKeyAndOrderFront(nil)
        self.window = window
        NSApp.activate(ignoringOtherApps: true)
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        true
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.setActivationPolicy(.regular)
app.run()
