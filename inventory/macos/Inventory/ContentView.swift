import SwiftUI

private enum Section: String, CaseIterable, Identifiable {
    case stock = "Stock"
    case orders = "Orders"
    case notes = "Notes"

    var id: String { rawValue }
}

struct ContentView: View {
    @State private var sidebarOpen = true
    @State private var section = Section.stock
    @State private var selectedId: String?
    private let items = InventoryBridge.items()

    private var selected: InventoryLine? {
        items.first { $0.id == selectedId }
    }

    var body: some View {
        HStack(spacing: 0) {
            if sidebarOpen {
                sidePanel
                    .frame(width: 196)
                    .transition(.move(edge: .leading).combined(with: .opacity))
            }
            column(title: section.rawValue) {
                List(items, selection: $selectedId) { item in
                    HStack {
                        Text(item.name)
                        Spacer()
                        Text("\(item.quantity)").monospacedDigit()
                    }
                    .tag(item.id)
                }
            }
            column(title: "Detail") {
                if let selected {
                    Text(selected.name).font(.title2)
                    Text(selected.id).foregroundStyle(.secondary)
                } else {
                    Text("Select an item").foregroundStyle(.secondary)
                }
            }
            column(title: "Notes") {
                if let selected {
                    Text("Quantity \(selected.quantity)").font(.title3)
                } else {
                    Text("Nothing selected").foregroundStyle(.secondary)
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: sidebarOpen)
        .frame(minWidth: 880, minHeight: 480)
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    sidebarOpen.toggle()
                } label: {
                    Image(systemName: "sidebar.leading")
                }
                .help(sidebarOpen ? "Hide panel" : "Show panel")
            }
        }
    }

    private var sidePanel: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Sections")
                .font(.headline)
                .padding(.bottom, 8)
            ForEach(Section.allCases) { entry in
                Button {
                    section = entry
                } label: {
                    Text(entry.rawValue)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(entry == section ? Color.accentColor.opacity(0.18) : Color.clear, in: RoundedRectangle(cornerRadius: 6))
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .padding(12)
        .frame(maxHeight: .infinity)
        .background(.bar)
    }

    private func column<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline)
            content()
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(.background)
        .overlay(alignment: .leading) {
            Rectangle().fill(Color.primary.opacity(0.08)).frame(width: 1)
        }
    }
}
