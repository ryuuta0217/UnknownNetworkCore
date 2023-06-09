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

package net.unknown.survival.feature;

import com.vexsoftware.votifier.model.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Logger;

public class VoteListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger("UNC/Vote");
    private static final Map<UUID, Set<ItemStack>> INSERT_ITEM_QUEUE = new HashMap<>();

    @EventHandler
    public void onVote(VotifierEvent event) {
        Bukkit.broadcast(Component.empty()
                .append(Component.text("[Unknown System]", Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD)))
                .append(Component.space())
                .append(Component.text(event.getVote().getUsername() + " さんが投票しました。ありがとう！", DefinedTextColor.GREEN)));

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId(event.getVote().getUsername()));

        ItemStack ticket = new ItemStackBuilder(Material.PAPER)
                .displayName(Component.text("投票チケット", DefinedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                .lore(Component.text("投票でもらえるチケット", DefinedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .custom(stack -> {
                    ItemMeta meta = stack.getItemMeta();
                    meta.getPersistentDataContainer().set(NamespacedKey.fromString("unknown-network:voted_player"), PersistentDataType.STRING, offlinePlayer.getUniqueId().toString());
                    stack.setItemMeta(meta);
                })
                .build();

        if (offlinePlayer.isOnline()) {
            Player player = Bukkit.getPlayer(event.getVote().getUsername());
            player.getInventory().addItem(ticket).forEach((slot, item) -> player.getWorld().dropItem(player.getLocation(), item));
        } else {
            if (INSERT_ITEM_QUEUE.containsKey(offlinePlayer.getUniqueId())) INSERT_ITEM_QUEUE.put(offlinePlayer.getUniqueId(), new HashSet<>());
            INSERT_ITEM_QUEUE.get(offlinePlayer.getUniqueId()).add(ticket);
            LOGGER.info("Give vote ticket queued. Currently queued " + INSERT_ITEM_QUEUE.size() + " player(s).");
        }
        Bukkit.broadcast(Component.text("あなたも投票してみませんか？ ", DefinedTextColor.GREEN)
                .append(Component.text("投票する: ", DefinedTextColor.AQUA)
                        .append(Component.text("monocraft", Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl("https://monocraft.net/servers/hWvNPIBskVkZ743kWt8S/vote")))
                        .append(Component.space())
                        .append(Component.text("JMS", Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl("https://minecraft.jp/servers/play.mc-unknown.net/vote")))));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (INSERT_ITEM_QUEUE.containsKey(event.getPlayer().getUniqueId())) {
            INSERT_ITEM_QUEUE.get(event.getPlayer().getUniqueId()).forEach(queuedStack -> {
                event.getPlayer().getInventory().addItem(queuedStack).forEach((slot, stack) -> {
                    event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), stack);
                });
            });
            INSERT_ITEM_QUEUE.remove(event.getPlayer().getUniqueId());
        }
    }
}
