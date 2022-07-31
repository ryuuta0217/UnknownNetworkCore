package net.unknown.survival.gui.warp.views;

import net.unknown.core.gui.View;
import net.unknown.survival.gui.warp.WarpGui;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.stream.IntStream;

public class WarpsView implements View {
    private final WarpGui parent;

    public WarpsView(WarpGui parent) {
        this.parent = parent;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void onClick(InventoryClickEvent event) {

    }

    @Override
    public void clearInventory() {
        IntStream.rangeClosed(46, 53).forEach(this.parent.getInventory()::clear);
    }
}
