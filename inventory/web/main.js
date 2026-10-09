import { items } from "./inventory-bridge.js";

const list = document.querySelector("#items");
for (const item of items) {
  const row = document.createElement("li");
  const name = document.createElement("span");
  name.textContent = item.name;
  const quantity = document.createElement("span");
  quantity.textContent = String(item.quantity);
  row.append(name, quantity);
  list.append(row);
}
