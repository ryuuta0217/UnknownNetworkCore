package net.unknown.core.item;

import net.unknown.UnknownNetworkCore;
import net.unknown.core.builder.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public abstract class UnknownNetworkItem {
    public static final NamespacedKey ID_CONTAINER_ID = new NamespacedKey(UnknownNetworkCore.getInstance(), "custom_item_id");
    public static final UnknownNetworkItem EMPTY = new UnknownNetworkItem(new NamespacedKey("core", "air")) {
        @Override
        public ItemStack createItemStack() {
            return new ItemStack(Material.AIR);
        }
    };

    private final NamespacedKey id;

    public UnknownNetworkItem(NamespacedKey id) {
        if (id == null) throw new IllegalArgumentException("Can't create null id Item!");
        this.id = id;
    }

    public NamespacedKey getId() {
        return this.id;
    }

    public abstract ItemStack createItemStack();

    protected ItemStackBuilder createItemStackBuilder(Material type) {
        return new ItemStackBuilder(type)
                .custom(is -> is.editMeta(meta -> meta.getPersistentDataContainer().set(ID_CONTAINER_ID, PersistentDataType.STRING, this.getId().asString())));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof String str) return this.equals(str);
        if (obj instanceof NamespacedKey key) return this.equals(key);
        if (obj instanceof UnknownNetworkItem item) return this.equals(item);
        if (obj instanceof ItemStack stack) return this.equals(stack);
        return super.equals(obj);
    }

    public boolean equals(String str) {
        return this.equals(NamespacedKey.fromString(str != null ? str : ""));
    }

    public boolean equals(NamespacedKey key) {
        return this.getId().equals(key);
    }

    public boolean equals(UnknownNetworkItem item) {
        return super.equals(item) || (item != null && this.equals(item.getId()));
    }

    public boolean equals(ItemStack stack) {
        if (stack != null && stack.getItemMeta() != null) {
            PersistentDataContainer dataContainer = stack.getItemMeta().getPersistentDataContainer();
            if (dataContainer.has(ID_CONTAINER_ID)) {
                return this.equals(NamespacedKey.fromString(dataContainer.getOrDefault(ID_CONTAINER_ID, PersistentDataType.STRING, "")));
            }
        }
        return false;
    }
}
