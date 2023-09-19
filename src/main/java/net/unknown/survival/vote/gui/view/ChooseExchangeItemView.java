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

package net.unknown.survival.vote.gui.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.VoteTicketExchangeItems;
import net.unknown.survival.data.model.VoteTicketExchangeItem;
import net.unknown.survival.queue.ItemGiveQueue;
import net.unknown.survival.vote.VoteManager;
import net.unknown.survival.vote.gui.VoteTicketExchangeGui;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class ChooseExchangeItemView extends PaginationView<VoteTicketExchangeItem, VoteTicketExchangeGui> {
    private Map<Integer, VoteTicketExchangeItem> RANDOM_ITEMS = new HashMap<>();

    public ChooseExchangeItemView(VoteTicketExchangeGui gui) {
        this(gui, null);
    }

    public ChooseExchangeItemView(VoteTicketExchangeGui gui, BiConsumer<InventoryClickEvent, PaginationView<VoteTicketExchangeItem, VoteTicketExchangeGui>> previousAction) {
        super(gui, VoteTicketExchangeItems.getExchangeItems().values(), (item) -> {
            ItemStack showItem = item.getDisplayItem(gui.getPlayer());
            if (showItem == null || showItem.getType() == Material.AIR) return showItem;
            showItem = showItem.clone();

            ItemMeta meta = showItem.getItemMeta();
            List<Component> lore = new ArrayList<>() {{
                if (meta.hasLore()) {
                    addAll(meta.lore());
                }
                int playerTicketAmount = VoteManager.getPlayerTicketAmount(gui.getPlayer());
                int price = item.getType() == VoteTicketExchangeItem.ItemType.SCRIPT ? item.execGetPriceFunction(gui.getPlayer()) : item.getPrice();

                boolean purchasable = playerTicketAmount >= price;
                add(Component.empty());
                add(Component.text("チケット" + price + "枚で交換可能", DefinedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                if (purchasable) {
                    add(Component.text("クリックして交換", DefinedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                } else {
                    add(Component.text("チケットが足りません", DefinedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)
                            .appendSpace()
                            .append(Component.text("(必要: " + price + "枚 | 不足: " + (price - playerTicketAmount) + "枚 | 所持: " + playerTicketAmount + "枚)", DefinedTextColor.RED).decoration(TextDecoration.BOLD, false)));
                }
            }};
            meta.lore(lore);
            showItem.setItemMeta(meta);
            return showItem;
        }, null, null, previousAction);
    }

    @Override
    public void onElementButtonClicked(InventoryClickEvent event, VoteTicketExchangeItem item) {
        int playerTicketAmount = VoteManager.getPlayerTicketAmount(this.getGui().getPlayer());
        int price = item.getType() == VoteTicketExchangeItem.ItemType.SCRIPT ? item.execGetPriceFunction(this.getGui().getPlayer()) : item.getPrice();

        if (playerTicketAmount < price) {
            NewMessageUtil.sendErrorMessage(this.getGui().getPlayer(), "チケットが足りません (必要: " + price + " | 不足: " + (price - playerTicketAmount) + " | 所持: " + playerTicketAmount + ")");
            return;
        }

        exchangeItem(item, null);
    }

    public void exchangeItem(VoteTicketExchangeItem item, @Nullable ItemStack choice) {
        boolean isScriptMode = item.getType() == VoteTicketExchangeItem.ItemType.SCRIPT && !item.getOnExchangedScript().isEmpty() && item.getOnExchangedFunction() != null;

        ItemStack exchangeItem = null;
        if (item.getType() != VoteTicketExchangeItem.ItemType.SELECTABLE_CONTAINER) {
            if (!isScriptMode) {
                exchangeItem = item.getItem().clone();
            }
        } else {
            if (choice != null) {
                exchangeItem = item.getItem(this.getGui().getPlayer(), choice);
            } else {
                this.getGui().setView(new ChooseItemView(this, item));
                return;
            }
        }

        if (exchangeItem == null && !isScriptMode) throw new IllegalStateException("Invalid exchange item - " + item.getType());

        if (isScriptMode) {
            item.execOnExchangedFunction(this.getGui().getPlayer(), choice);
        } else {
            ItemGiveQueue.queue(this.getGui().getPlayer().getUniqueId(), exchangeItem);
        }

        int price = item.getType() == VoteTicketExchangeItem.ItemType.SCRIPT ? item.execGetPriceFunction(this.getGui().getPlayer()) : item.getPrice();
        VoteManager.removePlayerTickets(this.getGui().getPlayer(), price);
        NewMessageUtil.sendMessage(this.getGui().getPlayer(), Component.empty()
                .append(Component.text("投票チケット" + price + "枚と"))
                .appendSpace()
                .append(exchangeItem.displayName().hoverEvent(exchangeItem.asHoverEvent()))
                .append(exchangeItem.getAmount() > 1 ? Component.text(" x" + exchangeItem.getAmount()) : Component.empty())
                .appendSpace()
                .append(Component.text("を交換しました")));
        if (item.getType() != VoteTicketExchangeItem.ItemType.SELECTABLE_CONTAINER) this.showPage(this.getCurrentPage());
    }
}
