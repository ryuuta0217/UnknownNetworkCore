package net.unknown.core.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class UnknownNetworkItemStack {
    private final ItemStack stack;
    private final UnknownNetworkItem item;

    public UnknownNetworkItemStack(ItemStack stack, UnknownNetworkItem item) {
        if (!item.equals(stack)) throw new IllegalArgumentException("Item mismatch (expected: " + item.getId() + ", actual: " + stack.getItemMeta().getPersistentDataContainer().getOrDefault(UnknownNetworkItem.ID_CONTAINER_ID, PersistentDataType.STRING, "unknown (vanilla?)") + ")");
        this.stack = stack;
        this.item = item;
    }

    public ItemStack getBukkitStack() {
        return this.stack;
    }

    public UnknownNetworkItem getItem() {
        return this.item;
    }
}
