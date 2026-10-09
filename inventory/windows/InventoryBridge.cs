namespace Inventory;

public sealed record InventoryLine(string Id, string Name, int Quantity);

public static class InventoryBridge
{
    // Stand-in until this window calls inventory/core.
    // The list is owned by inventory.core.Inventory.items.
    public static IReadOnlyList<InventoryLine> Items { get; } =
    [
        new("nb", "Field notebook", 4),
        new("ruler", "Brass ruler", 1),
        new("ink", "Ink bottle", 2),
    ];
}
