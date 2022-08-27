package net.unknown.survival.gui.prefix.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.gui.SignGui;
import net.unknown.core.prefix.PlayerPrefixes;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.gui.prefix.PrefixGui;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.survival.gui.prefix.PrefixGuiState;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PrefixesView extends PaginationView<Component> {
    public PrefixesView(PrefixGui gui, Player player) {
        super(gui, PlayerPrefixes.getAvailablePrefixes(player),
                (prefix) -> new ItemStackBuilder(Material.NAME_TAG).displayName(prefix).build(),
                (event, prefix) -> {
                    PlayerPrefixes.setPrefix(event.getWhoClicked().getUniqueId(), prefix);
                    event.getWhoClicked().closeInventory();
                    NewMessageUtil.sendMessage(((Player) event.getWhoClicked()), Component.empty()
                            .append(Component.text("接頭辞を "))
                            .append(prefix)
                            .append(Component.text(" に変更しました")));
                },
                (event, instance) -> {
            gui.setState(PrefixGuiState.WAITING_CALLBACK);
            new SignGui()
                    .withTarget(player)
                    .withLines(Component.empty(), Component.text("^^^^^^^^^^^^^^^^^^^^"), Component.text("接頭辞を入力"), Component.empty())
                    .onComplete(lines -> {
                        String raw = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                        Component colored = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                        PlayerPrefixes.addAvailablePrefix(player, colored);
                        instance.setData(PlayerPrefixes.getAvailablePrefixes(player), true);
                    }).open();
                });
    }
}
