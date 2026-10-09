import Foundation

struct InventoryLine: Identifiable, Hashable {
    let id: String
    let name: String
    let quantity: Int
}

enum InventoryBridge {
    // Stand-in until inventory/core is linked as an iOS framework.
    // The list is owned by inventory.core.Inventory.items.
    static func items() -> [InventoryLine] {
        [
            InventoryLine(id: "nb", name: "Field notebook", quantity: 4),
            InventoryLine(id: "ruler", name: "Brass ruler", quantity: 1),
            InventoryLine(id: "ink", name: "Ink bottle", quantity: 2),
        ]
    }
}
