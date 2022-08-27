package net.unknown.core.gui.view;

import net.unknown.core.gui.GuiBase;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ChooseView implements View {
    private final GuiBase gui;
    private final Map<Integer, ItemStack> items;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions;


    private ChooseView(GuiBase gui, Map<Integer, ItemStack> items, Map<Integer, Consumer<InventoryClickEvent>> actions) {
        this.gui = gui;
        this.items = items;
        this.actions = actions;
    }

    @Override
    public void initialize() {
        this.items.forEach((slot, item) -> {
            if(this.gui.getInventory().getItem(slot) == null) {
                this.gui.getInventory().setItem(slot, item);
            }
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.actions.getOrDefault(event.getSlot(), (e) -> {}).accept(event);
    }

    @Override
    public void clearInventory() {
        this.items.keySet().forEach(this.gui.getInventory()::clear);
    }

    public static class Builder {
        private final GuiBase gui;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

        public Builder(GuiBase gui) {
            this.gui = gui;
        }

        public static Builder newBuilder(GuiBase gui) {
            return new Builder(gui);
        }

        public Builder addSelection(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
            if ((this.gui.getInventory().getSize() - 1) > slot) {
               throw new IllegalArgumentException("Slot " + slot + " is out of range <" + (this.gui.getInventory().getSize() - 1)  + ">");
            }

            this.items.put(slot, item);
            this.actions.put(slot, action);
            return this;
        }

        public ChooseView build() {
            return new ChooseView(this.gui, this.items, this.actions);
        }
    }
}
