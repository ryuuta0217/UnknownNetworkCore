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

package net.unknown.lobby.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.lobby.UnknownNetworkLobby;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;

public class ServerSelector implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().equals(UnknownNetworkLobby.getServerSelectorCompass())) {
            Gui.getInstance().open(event.getPlayer());
            event.setCancelled(true);
        }
    }

    private static class Gui extends GuiBase {
        private static final Gui INSTANCE = new Gui();
        private Gui() {
            super(null, 27, Component.text("サーバー選択", DefinedTextColor.DARK_BLUE),
                    (inv) -> {
                        /*inv.setItem(12, new ItemStackBuilder(Material.APPLE)
                                .displayName(Component.text("ミニゲーム", DefinedTextColor.GREEN))
                                .build());*/
                        inv.setItem(13, new ItemStackBuilder(Material.GRASS_BLOCK)
                                .displayName(Component.text("生活鯖", DefinedTextColor.GOLD))
                                .build());
                        /*inv.setItem(14, new ItemStackBuilder(Material.STRUCTURE_BLOCK)
                                .displayName(Component.text("デバッグ", DefinedTextColor.GRAY))
                                .build());*/
                    }, false);
            this.onClick = (slot, event) -> {
                /*if (slot == 12) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("minigame");
                    ((Player) event.getWhoClicked()).sendPluginMessage(UnknownNetworkCore.getInstance(), "BungeeCord", out.toByteArray());
                }*/

                if (slot == 13) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("survival");
                    ((Player) event.getWhoClicked()).sendPluginMessage(UnknownNetworkCorePlugin.getInstance(), "BungeeCord", out.toByteArray());
                }

                if (slot == 14) {
                    //ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    //out.writeUTF("Connect");
                    //out.writeUTF("debug");
                    //((Player) event.getWhoClicked()).sendPluginMessage(UnknownNetworkCore.getInstance(), "Bungeecord", out.toByteArray());
                }
            };
        }

        public static Gui getInstance() {
            return INSTANCE;
        }
    }
}
