package inventory.core

data class InventoryItem(
    val id: String,
    val name: String,
    val quantity: Int,
)

object Inventory {
    fun items(): List<InventoryItem> = listOf(
        InventoryItem(id = "nb", name = "Field notebook", quantity = 4),
        InventoryItem(id = "ruler", name = "Brass ruler", quantity = 1),
        InventoryItem(id = "ink", name = "Ink bottle", quantity = 2),
    )
}
