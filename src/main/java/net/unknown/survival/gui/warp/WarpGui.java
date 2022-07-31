package net.unknown.survival.gui.warp;

import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.View;
import net.unknown.survival.gui.warp.views.WarpsView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class WarpGui extends GuiBase {
    private final GuiBase parent;
    private final Player opener;
    private View view;
    private WarpGuiState state;

    public WarpGui(GuiBase parent, Player opener) {
        super(opener, 54, Component.text("ワープ", DefinedTextColor.GREEN), false);
        this.parent = parent;
        this.opener = opener;

        this.view = new WarpsView(this);
        this.state = WarpGuiState.AVAILABLE_WARPS;
        this.view.initialize();

        this.getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                .build());
    }

    public void setView(View view, WarpGuiState state) {
        this.view = view;
        this.state = state;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(event.getSlot() == 45) {
            event.getWhoClicked().openInventory(this.parent.getInventory());
        } else this.view.onClick(event);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if(this.state != WarpGuiState.WAITING_CALLBACK) this.unRegisterAsListener();
    }
}
