import CoreHaptics
import SwiftUI
import UIKit

private enum Section: String, CaseIterable, Identifiable {
    case stock = "Stock"
    case orders = "Orders"
    case notes = "Notes"

    var id: String { rawValue }
}

struct ContentView: View {
    @State private var section = Section.stock
    @State private var selected: InventoryLine?
    private let haptics = InventoryHaptics()
    private let items = InventoryBridge.items()

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                sectionList
                    .id(section)
                    .transition(sectionTransition)
                pill
            }
            .animation(.spring(duration: 0.35), value: section)
            .navigationDestination(item: $selected) { item in
                VStack(alignment: .leading, spacing: 12) {
                    Text(item.name).font(.largeTitle)
                    Text(item.id).foregroundStyle(.secondary)
                    Text("Quantity \(item.quantity)").font(.title2)
                    Spacer()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .navigationTitle(item.name)
                .navigationBarTitleDisplayMode(.inline)
            }
        }
    }

    private var sectionTransition: AnyTransition {
        .asymmetric(
            insertion: .move(edge: .trailing).combined(with: .opacity),
            removal: .move(edge: .leading).combined(with: .opacity)
        )
    }

    private var sectionList: some View {
        List(items) { item in
            Button {
                haptics.open()
                selected = item
            } label: {
                HStack {
                    Text(item.name).foregroundStyle(.primary)
                    Spacer()
                    Text("\(item.quantity)").monospacedDigit().foregroundStyle(.primary)
                }
            }
        }
        .navigationTitle(section.rawValue)
        .safeAreaInset(edge: .bottom) {
            Color.clear.frame(height: 72)
        }
    }

    private var pill: some View {
        HStack(spacing: 0) {
            ForEach(Section.allCases) { entry in
                Button {
                    guard entry != section else { return }
                    haptics.sectionChanged()
                    withAnimation(.spring(duration: 0.35)) {
                        section = entry
                    }
                } label: {
                    Text(entry.rawValue)
                        .font(.subheadline.weight(entry == section ? .semibold : .regular))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .foregroundStyle(entry == section ? Color.accentColor : Color.primary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(.ultraThinMaterial, in: Capsule())
        .shadow(color: .black.opacity(0.12), radius: 12, y: 4)
        .padding(.bottom, 12)
    }
}

private final class InventoryHaptics {
    private var engine: CHHapticEngine?

    init() {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return }
        engine = try? CHHapticEngine()
        try? engine?.start()
    }

    func sectionChanged() {
        play(intensities: [0.35, 0.7], sharpnesses: [0.25, 0.85], times: [0, 0.07])
    }

    func open() {
        play(intensities: [0.85, 0.4], sharpnesses: [0.9, 0.2], times: [0, 0.06])
    }

    private func play(intensities: [Float], sharpnesses: [Float], times: [TimeInterval]) {
        guard let engine else {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            return
        }
        let events = zip(times, zip(intensities, sharpnesses)).map { time, pair in
            CHHapticEvent(
                eventType: .hapticTransient,
                parameters: [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: pair.0),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: pair.1),
                ],
                relativeTime: time
            )
        }
        guard let pattern = try? CHHapticPattern(events: events, parameters: []),
              let player = try? engine.makePlayer(with: pattern) else { return }
        try? player.start(atTime: 0)
    }
}
