/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.lobby.feature;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.lobby.UnknownNetworkLobby;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerSelector extends ConfigurationBase implements Listener {
    private final Map<Integer, Map.Entry<ItemStack, String>> servers = new HashMap<>();

    public ServerSelector() {
        super("server_selector.yml", true, "UNC/ServerSelector");
    }

    @Override
    public void onLoad() {
        this.servers.clear();
        if (this.getConfig().isSet("servers")) {
            ConfigurationSection section = this.getConfig().getConfigurationSection("servers");
            if (section != null) {
                section.getKeys(false).forEach(slotNumStr -> {
                    int slotNum;
                    try {
                        slotNum = Integer.parseInt(slotNumStr);
                    } catch (NumberFormatException e) {
                        this.getLogger().warning("Invalid slot number: " + slotNumStr);
                        return;
                    }

                    ConfigurationSection serverSection = section.getConfigurationSection(slotNumStr);
                    if (serverSection != null && serverSection.isSet("server-name") && serverSection.isSet("item")) {
                        String serverName = serverSection.getString("server-name");
                        String itemJson = serverSection.getString("item");
                        ItemStack item = MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(itemJson));
                        if (item == null) {
                            this.getLogger().warning("Invalid item json: " + itemJson);
                            return;
                        }
                        this.servers.put(slotNum, Map.entry(item, serverName));
                    } else {
                        this.getLogger().warning("Invalid server section: " + slotNumStr);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().equals(getServerSelectorCompass())) {
            Gui.getInstance().open(event.getPlayer());
            event.setCancelled(true);
        }
    }

    public Map<Integer, Map.Entry<ItemStack, String>> getServers() {
        return Collections.unmodifiableMap(this.servers);
    }

    public static ItemStack getServerSelectorCompass() {
        return new ItemStackBuilder(Material.COMPASS)
                .displayName(Component.text("サーバーをえらぶ", Style.style(DefinedTextColor.AQUA, TextDecoration.BOLD.withState(true))))
                .lore(Component.text("右クリックでサーバー選択画面を開きます", DefinedTextColor.GOLD))
                .build();
    }

    private static class Gui extends GuiBase {
        private static final Gui INSTANCE = new Gui();
        private Gui() {
            super(null, 27, Component.text("サーバー選択", DefinedTextColor.DARK_BLUE),
                    (inv) -> {
                        UnknownNetworkLobby.getServerSelector().getServers().forEach((slot, entry) -> {
                            inv.setItem(slot, entry.getKey());
                        });
                    }, false);
            this.onClick = (slot, event) -> {
                Optional.ofNullable(UnknownNetworkLobby.getServerSelector().getServers().getOrDefault(slot, null)).ifPresent(e -> {
                    String server = e.getValue();
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(server);
                    ((Player) event.getWhoClicked()).sendPluginMessage(UnknownNetworkCorePlugin.getInstance(), "BungeeCord", out.toByteArray());
                });
            };
        }

        public static Gui getInstance() {
            return INSTANCE;
        }
    }
}
