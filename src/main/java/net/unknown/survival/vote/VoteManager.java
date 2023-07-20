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

package net.unknown.survival.vote;

import com.ryuuta0217.util.MojangApi;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.queue.ItemGiveQueue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteManager implements Listener {
    private static final Component TICKET_ITEM_NAME = Component.text("投票チケット", DefinedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    private static final NamespacedKey TICKET_VOTED_PLAYER_KEY = NamespacedKey.fromString("unknown-network:voter");
    private static final VoteManager INSTANCE = new VoteManager();

    private VoteManager() {}

    @EventHandler
    public void onVote(VotifierEvent event) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getName().equals(event.getVote().getUsername())) {
                NewMessageUtil.sendMessage(player, Component.text("投票ありがとうございます！", DefinedTextColor.AQUA), false);
            } else {
                NewMessageUtil.sendMessage(player, Component.text(event.getVote().getUsername() + " さんが投票しました。ありがとう！", DefinedTextColor.GREEN), false);
                NewMessageUtil.sendMessage(player, Component.text("あなたも投票してみませんか？", DefinedTextColor.GREEN)
                        .appendNewline()
                        .append(Component.text("投票する: ", DefinedTextColor.AQUA)
                                .append(Component.text("monocraft", Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl("https://monocraft.net/servers/hWvNPIBskVkZ743kWt8S/vote")))
                                .append(Component.space())
                                .append(Component.text("JMS", Style.style(DefinedTextColor.AQUA, TextDecoration.UNDERLINED)).clickEvent(ClickEvent.openUrl("https://minecraft.jp/servers/play.mc-unknown.net/vote")))), false);
            }
        });

        RunnableManager.runAsync(() -> { // REST APIコールは遅延を引き起こす可能性があるので非同期で実行する
            UUID uuid = MojangApi.getUUID(event.getVote().getUsername());
            ItemGiveQueue.queue(uuid, VoteManager.buildTicket(event.getVote().getUsername(), uuid));
        });
    }

    public static VoteManager getInstance() {
        return INSTANCE;
    }

    public static Component getTicketItemName() {
        return TICKET_ITEM_NAME;
    }

    public static NamespacedKey getTicketVotedPlayerKey() {
        return TICKET_VOTED_PLAYER_KEY;
    }

    public static ItemStackBuilder ticketBuilder() {
        return new ItemStackBuilder(Material.PAPER)
                .displayName(VoteManager.getTicketItemName())
                .addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
    }

    public static ItemStack buildTicket(String name, UUID uuid) {
        return VoteManager.ticketBuilder()
                .lore(Component.text("投票でもらえるチケット", DefinedTextColor.AQUA).decorate(TextDecoration.BOLD),
                        Component.empty(),
                        Component.empty(),
                        Component.text("投票者: " + name, DefinedTextColor.GRAY))
                .custom(stack -> {
                    ItemMeta meta = stack.getItemMeta();
                    meta.getPersistentDataContainer().set(TICKET_VOTED_PLAYER_KEY, PersistentDataType.STRING, uuid.toString());
                    stack.setItemMeta(meta);
                })
                .build();
    }

    public static int getPlayerTicketAmount(HumanEntity player) {
        AtomicInteger carriedTickets = new AtomicInteger(0);
        player.getInventory().forEach(item -> {
            if (!isValidTicket(player, item)) return;
            carriedTickets.addAndGet(item.getAmount());
        });
        return carriedTickets.get();
    }

    public static void removePlayerTickets(HumanEntity player, int amount) {
        if (getPlayerTicketAmount(player) < amount) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isValidTicket(player, item)) continue;
            if (amount >= item.getAmount()) {
                amount -= item.getAmount();
                item.setAmount(0);
            } else {
                item.setAmount(item.getAmount() - amount);
                amount = 0;
            }
            if (amount <= 0) break;
        }
    }

    public static boolean isValidTicket(HumanEntity player, ItemStack item) {
        if (item != null && item.getType() == Material.PAPER && item.getItemMeta().hasDisplayName() && item.getItemMeta().displayName().equals(getTicketItemName())) {
            return Objects.equals(item.getItemMeta().getPersistentDataContainer().get(getTicketVotedPlayerKey(), PersistentDataType.STRING), player.getUniqueId().toString());
        }
        return false;
    }
}
