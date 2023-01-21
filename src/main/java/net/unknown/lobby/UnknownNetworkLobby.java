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

package net.unknown.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.lobby.feature.RealTimeSynchronizer;
import net.unknown.lobby.listeners.Blocker;
import net.unknown.lobby.listeners.PlayerJoinListener;
import net.unknown.lobby.listeners.ServerSelector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class UnknownNetworkLobby {
    public static void onLoad() {

    }

    public static void onEnable() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(UnknownNetworkCore.getInstance(), "BungeeCord");
        ListenerManager.registerListener(new Blocker());
        ListenerManager.registerListener(new PlayerJoinListener());
        ListenerManager.registerListener(new ServerSelector());
        RealTimeSynchronizer.start();
    }

    public static void onDisable() {

    }

    public static ItemStack getServerSelectorCompass() {
        return new ItemStackBuilder(Material.COMPASS)
                .displayName(Component.text("サーバーをえらぶ", Style.style(DefinedTextColor.AQUA, TextDecoration.BOLD.withState(true))))
                .lore(Component.text("右クリックでサーバー選択画面を開きます", DefinedTextColor.GOLD))
                .build();
    }
}
