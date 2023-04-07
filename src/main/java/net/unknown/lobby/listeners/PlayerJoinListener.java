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

import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.world.InteractionHand;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.lobby.UnknownNetworkLobby;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ここにテレポートの処理書いてください
        // ッスゥ
        event.getPlayer().teleport(Bukkit.getWorld("world").getSpawnLocation());
        //summon ryuuta0217 hmhm koresa souiu syori suru yatu oboetenaito murizyane
        // まあそういうこった panna kotta oppai
        // 作りたい機能行ってみて ここに書いたる
        // ペット小屋機能：ﾗﾊﾞとかﾈｺを召喚したり仕舞ったりできる
        //tp ﾍﾟｯﾄ　置いてきたﾍﾟｯﾄにtpできる できたよ@15:50:20
        //山に登ったり降りたりするの大変だからジップラインほしい
        //アイデア書いたらコードができてくのすげえ!!w
        // キレそう
        //w
        if (!event.getPlayer().getInventory().contains(UnknownNetworkLobby.getServerSelectorCompass())) {
            event.getPlayer().getInventory().addItem(UnknownNetworkLobby.getServerSelectorCompass());
        }

        if (!event.getPlayer().getInventory().contains(UnknownNetworkLobby.getBook())) {
            event.getPlayer().getInventory().addItem(UnknownNetworkLobby.getBook());
        }

        if (!event.getPlayer().hasPlayedBefore()) {
            event.getPlayer().getInventory().setHeldItemSlot(event.getPlayer().getInventory().first(UnknownNetworkLobby.getBook()));
            ClientboundOpenBookPacket openBookPacket = new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND);
            MinecraftAdapter.player(event.getPlayer()).connection.send(openBookPacket);
        }
    }
}
