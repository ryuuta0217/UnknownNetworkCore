package net.unknown.survival.gui.prefix;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.MainGui;
import net.unknown.survival.gui.prefix.view.PrefixesView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import javax.annotation.Nonnull;

public class PrefixGui extends GuiBase {
    private final GuiBase parent;
    private final Player opener;
    private View view;
    private PrefixGuiState state;

    public PrefixGui(@Nonnull MainGui parent, @Nonnull Player opener) {
        super(opener, 54, Component.text("接頭辞/接尾辞", DefinedTextColor.DARK_GREEN), false);
        this.parent = parent;
        this.opener = opener;
        this.setView(new PrefixesView(this, opener));
        this.setState(PrefixGuiState.AVAILABLE_PREFIXES);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.state != PrefixGuiState.WAITING_CALLBACK) {
            this.unRegisterAsListener();
        }
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setState(PrefixGuiState state) {
        this.state = state;
    }
}
