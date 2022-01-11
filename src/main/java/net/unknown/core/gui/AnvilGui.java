package net.unknown.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class AnvilGui {
    private final Inventory anvil;

    public AnvilGui(Player player) {
        this.anvil = Bukkit.createInventory(player, InventoryType.ANVIL);
    }
}
